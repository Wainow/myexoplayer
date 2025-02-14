/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.mynewmedia4.transformer;

import static androidx.mynewmedia4.common.ColorInfo.isTransferHdr;
import static androidx.mynewmedia4.common.util.Assertions.checkNotNull;
import static androidx.mynewmedia4.common.util.Assertions.checkStateNotNull;
import static androidx.mynewmedia4.transformer.EncoderUtil.getSupportedEncoders;
import static androidx.mynewmedia4.transformer.EncoderUtil.getSupportedEncodersForHdrEditing;
import static androidx.mynewmedia4.transformer.TransformerUtil.getProcessedTrackType;

import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.ColorInfo;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.Metadata;
import androidx.mynewmedia4.common.MimeTypes;
import androidx.mynewmedia4.decoder.DecoderInputBuffer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;

/**
 * Pipeline for processing media data.
 *
 * <p>This pipeline can be used to implement transformations of audio or video samples.
 *
 * <p>The {@link SampleConsumer} and {@link OnMediaItemChangedListener} methods must be called from
 * the same thread. This thread can change when the {@link
 * OnMediaItemChangedListener#onMediaItemChanged(EditedMediaItem, long, Format, boolean) MediaItem}
 * changes, and can be different from the thread used to call the other {@code SamplePipeline}
 * methods.
 */
/* package */ abstract class SamplePipeline implements SampleConsumer, OnMediaItemChangedListener {

  private final MuxerWrapper muxerWrapper;
  private final @C.TrackType int outputTrackType;
  @Nullable private final Metadata metadata;

  private boolean muxerWrapperTrackAdded;

  public SamplePipeline(Format firstInputFormat, MuxerWrapper muxerWrapper) {
    this.muxerWrapper = muxerWrapper;
    this.metadata = firstInputFormat.metadata;
    outputTrackType = getProcessedTrackType(firstInputFormat.sampleMimeType);
  }

  /**
   * Processes the input data and returns whether it may be possible to process more data by calling
   * this method again.
   */
  public final boolean processData() throws ExportException {
    return feedMuxer() || processDataUpToMuxer();
  }

  /** Releases all resources held by the pipeline. */
  public abstract void release();

  protected boolean processDataUpToMuxer() throws ExportException {
    return false;
  }

  @Nullable
  protected abstract Format getMuxerInputFormat() throws ExportException;

  @Nullable
  protected abstract DecoderInputBuffer getMuxerInputBuffer() throws ExportException;

  protected abstract void releaseMuxerInputBuffer() throws ExportException;

  protected abstract boolean isMuxerInputEnded();

  /**
   * Attempts to pass encoded data to the muxer, and returns whether it may be possible to pass more
   * data immediately by calling this method again.
   */
  private boolean feedMuxer() throws ExportException {
    if (!muxerWrapperTrackAdded) {
      @Nullable Format inputFormat = getMuxerInputFormat();
      if (inputFormat == null) {
        return false;
      }
      if (metadata != null) {
        inputFormat = inputFormat.buildUpon().setMetadata(metadata).build();
      }
      try {
        muxerWrapper.addTrackFormat(inputFormat);
      } catch (Muxer.MuxerException e) {
        throw ExportException.createForMuxer(e, ExportException.ERROR_CODE_MUXING_FAILED);
      }
      muxerWrapperTrackAdded = true;
    }

    if (isMuxerInputEnded()) {
      muxerWrapper.endTrack(outputTrackType);
      return false;
    }

    @Nullable DecoderInputBuffer muxerInputBuffer = getMuxerInputBuffer();
    if (muxerInputBuffer == null) {
      return false;
    }

    try {
      if (!muxerWrapper.writeSample(
          outputTrackType,
          checkStateNotNull(muxerInputBuffer.data),
          muxerInputBuffer.isKeyFrame(),
          muxerInputBuffer.timeUs)) {
        return false;
      }
    } catch (Muxer.MuxerException e) {
      throw ExportException.createForMuxer(e, ExportException.ERROR_CODE_MUXING_FAILED);
    }

    releaseMuxerInputBuffer();
    return true;
  }

  /**
   * Finds a {@linkplain MimeTypes MIME type} that is supported by the encoder and the muxer.
   *
   * <p>The {@linkplain Format requestedFormat} determines what support is checked.
   *
   * <ul>
   *   <li>The {@link Format#sampleMimeType} determines whether audio or video MIME types are
   *       considered. See {@link MimeTypes#isAudio} and {@link MimeTypes#isVideo} for more details.
   *   <li>The {@link Format#sampleMimeType} must be populated with the preferred {@linkplain
   *       MimeTypes MIME type}. This MIME type will be the first checked.
   *   <li>When checking video support, if the HDR {@link Format#colorInfo} is set, only encoders
   *       that support that {@link ColorInfo} will be considered.
   * </ul>
   *
   * @param requestedFormat The {@link Format} requested.
   * @param muxerSupportedMimeTypes The list of sample {@linkplain MimeTypes MIME types} that the
   *     muxer supports.
   * @return A supported {@linkplain MimeTypes MIME type}.
   * @throws ExportException If there are no supported {@linkplain MimeTypes MIME types}.
   */
  protected static String findSupportedMimeTypeForEncoderAndMuxer(
      Format requestedFormat, List<String> muxerSupportedMimeTypes) throws ExportException {
    boolean isVideo = MimeTypes.isVideo(checkNotNull(requestedFormat.sampleMimeType));

    ImmutableSet.Builder<String> mimeTypesToCheckSetBuilder =
        new ImmutableSet.Builder<String>().add(requestedFormat.sampleMimeType);
    if (isVideo) {
      mimeTypesToCheckSetBuilder.add(MimeTypes.VIDEO_H265).add(MimeTypes.VIDEO_H264);
    }
    mimeTypesToCheckSetBuilder.addAll(muxerSupportedMimeTypes);
    ImmutableList<String> mimeTypesToCheck = mimeTypesToCheckSetBuilder.build().asList();

    for (int i = 0; i < mimeTypesToCheck.size(); i++) {
      String mimeType = mimeTypesToCheck.get(i);

      if (!muxerSupportedMimeTypes.contains(mimeType)) {
        continue;
      }

      if (isVideo && isTransferHdr(requestedFormat.colorInfo)) {
        if (!getSupportedEncodersForHdrEditing(mimeType, requestedFormat.colorInfo).isEmpty()) {
          return mimeType;
        }
      } else if (!getSupportedEncoders(mimeType).isEmpty()) {
        return mimeType;
      }
    }

    throw createNoSupportedMimeTypeException(requestedFormat);
  }

  private static ExportException createNoSupportedMimeTypeException(Format format) {
    String errorMessage = "No MIME type is supported by both encoder and muxer.";
    int errorCode = ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED;
    boolean isVideo = MimeTypes.isVideo(format.sampleMimeType);

    if (isVideo && isTransferHdr(format.colorInfo)) {
      errorMessage += " Requested HDR colorInfo: " + format.colorInfo;
    }

    return ExportException.createForCodec(
        new IllegalArgumentException(errorMessage),
        errorCode,
        isVideo,
        /* isDecoder= */ false,
        format);
  }
}
