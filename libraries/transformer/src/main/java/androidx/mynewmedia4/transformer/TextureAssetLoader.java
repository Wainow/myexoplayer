/*
 * Copyright 2023 The Android Open Source Project
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

import static androidx.mynewmedia4.common.util.Assertions.checkArgument;
import static androidx.mynewmedia4.common.util.Assertions.checkNotNull;
import static androidx.mynewmedia4.transformer.ExportException.ERROR_CODE_UNSPECIFIED;
import static androidx.mynewmedia4.transformer.Transformer.PROGRESS_STATE_AVAILABLE;
import static androidx.mynewmedia4.transformer.Transformer.PROGRESS_STATE_NOT_STARTED;
import static java.lang.Math.round;

import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.MimeTypes;
import androidx.mynewmedia4.common.OnInputFrameProcessedListener;
import androidx.mynewmedia4.common.util.UnstableApi;
import com.google.common.collect.ImmutableMap;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * An {@link AssetLoader} implementation that loads videos from {@linkplain
 * android.opengl.GLES10#GL_TEXTURE_2D traditional GLES texture} instances.
 *
 * <p>Typically instantiated in a custom {@link AssetLoader.Factory} saving a reference to the
 * created {@link TextureAssetLoader}. Input is provided calling {@link #queueInputTexture} to
 * provide all the video frames, then {@link #signalEndOfVideoInput() signalling the end of input}
 * when finished.
 */
@UnstableApi
public final class TextureAssetLoader implements AssetLoader {
  private final EditedMediaItem editedMediaItem;
  private final Listener assetLoaderListener;
  private final Format format;
  private final OnInputFrameProcessedListener frameProcessedListener;

  private @MonotonicNonNull SampleConsumer sampleConsumer;
  private @Transformer.ProgressState int progressState;
  private boolean isTrackAdded;

  private volatile boolean isStarted;
  private volatile long lastQueuedPresentationTimeUs;

  /**
   * Creates an instance.
   *
   * <p>The {@link EditedMediaItem#durationUs}, {@link Format#width} and {@link Format#height} must
   * be set.
   */
  public TextureAssetLoader(
      EditedMediaItem editedMediaItem,
      Listener assetLoaderListener,
      Format format,
      OnInputFrameProcessedListener frameProcessedListener) {
    checkArgument(editedMediaItem.durationUs != C.TIME_UNSET);
    checkArgument(format.height != Format.NO_VALUE && format.width != Format.NO_VALUE);
    this.editedMediaItem = editedMediaItem;
    this.assetLoaderListener = assetLoaderListener;
    this.format = format.buildUpon().setSampleMimeType(MimeTypes.VIDEO_RAW).build();
    this.frameProcessedListener = frameProcessedListener;
    progressState = PROGRESS_STATE_NOT_STARTED;
  }

  @Override
  public void start() {
    progressState = PROGRESS_STATE_AVAILABLE;
    assetLoaderListener.onDurationUs(editedMediaItem.durationUs);
    assetLoaderListener.onTrackCount(1);
    isStarted = true;
  }

  @Override
  public @Transformer.ProgressState int getProgress(ProgressHolder progressHolder) {
    if (progressState == PROGRESS_STATE_AVAILABLE) {
      progressHolder.progress =
          round((lastQueuedPresentationTimeUs / (float) editedMediaItem.durationUs) * 100);
    }
    return progressState;
  }

  @Override
  public ImmutableMap<Integer, String> getDecoderNames() {
    return ImmutableMap.of();
  }

  @Override
  public void release() {
    progressState = PROGRESS_STATE_NOT_STARTED;
  }

  /**
   * Attempts to provide an input texture.
   *
   * <p>Must be called on the same thread as {@link #signalEndOfVideoInput}.
   *
   * @param texId The ID of the texture to queue.
   * @param presentationTimeUs The presentation time for the texture, in microseconds.
   * @return Whether the texture was successfully queued. If {@code false}, the caller should try
   *     again later.
   */
  public boolean queueInputTexture(int texId, long presentationTimeUs) {
    try {
      if (!isTrackAdded) {
        if (!isStarted) {
          return false;
        }
        assetLoaderListener.onTrackAdded(format, SUPPORTED_OUTPUT_TYPE_DECODED);
        isTrackAdded = true;
      }
      if (sampleConsumer == null) {
        @Nullable SampleConsumer sampleConsumer = assetLoaderListener.onOutputFormat(format);
        if (sampleConsumer == null) {
          return false;
        } else {
          this.sampleConsumer = sampleConsumer;
          sampleConsumer.setOnInputFrameProcessedListener(frameProcessedListener);
        }
      }
      if (!sampleConsumer.queueInputTexture(texId, presentationTimeUs)) {
        return false;
      }
      lastQueuedPresentationTimeUs = presentationTimeUs;
      return true;
    } catch (ExportException e) {
      assetLoaderListener.onError(e);
    } catch (RuntimeException e) {
      assetLoaderListener.onError(ExportException.createForAssetLoader(e, ERROR_CODE_UNSPECIFIED));
    }
    return false;
  }

  /**
   * Signals that no further input frames will be rendered.
   *
   * <p>Must be called on the same thread as {@link #queueInputTexture}.
   */
  public void signalEndOfVideoInput() {
    try {
      checkNotNull(sampleConsumer).signalEndOfVideoInput();
    } catch (RuntimeException e) {
      assetLoaderListener.onError(ExportException.createForAssetLoader(e, ERROR_CODE_UNSPECIFIED));
    }
  }
}
