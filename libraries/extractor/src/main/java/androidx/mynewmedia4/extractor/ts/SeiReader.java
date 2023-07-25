/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.mynewmedia4.extractor.ts;

import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.MimeTypes;
import androidx.mynewmedia4.common.util.Assertions;
import androidx.mynewmedia4.common.util.ParsableByteArray;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.extractor.CeaUtil;
import androidx.mynewmedia4.extractor.ExtractorOutput;
import androidx.mynewmedia4.extractor.TrackOutput;
import androidx.mynewmedia4.extractor.ts.TsPayloadReader.TrackIdGenerator;
import java.util.List;

/** Consumes SEI buffers, outputting contained CEA-608/708 messages to a {@link TrackOutput}. */
@UnstableApi
public final class SeiReader {

  private final List<Format> closedCaptionFormats;
  private final TrackOutput[] outputs;

  /**
   * @param closedCaptionFormats A list of formats for the closed caption channels to expose.
   */
  public SeiReader(List<Format> closedCaptionFormats) {
    this.closedCaptionFormats = closedCaptionFormats;
    outputs = new TrackOutput[closedCaptionFormats.size()];
  }

  public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
    for (int i = 0; i < outputs.length; i++) {
      idGenerator.generateNewId();
      TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_TEXT);
      Format channelFormat = closedCaptionFormats.get(i);
      @Nullable String channelMimeType = channelFormat.sampleMimeType;
      Assertions.checkArgument(
          MimeTypes.APPLICATION_CEA608.equals(channelMimeType)
              || MimeTypes.APPLICATION_CEA708.equals(channelMimeType),
          "Invalid closed caption MIME type provided: " + channelMimeType);
      String formatId = channelFormat.id != null ? channelFormat.id : idGenerator.getFormatId();
      output.format(
          new Format.Builder()
              .setId(formatId)
              .setSampleMimeType(channelMimeType)
              .setSelectionFlags(channelFormat.selectionFlags)
              .setLanguage(channelFormat.language)
              .setAccessibilityChannel(channelFormat.accessibilityChannel)
              .setInitializationData(channelFormat.initializationData)
              .build());
      outputs[i] = output;
    }
  }

  public void consume(long pesTimeUs, ParsableByteArray seiBuffer) {
    CeaUtil.consume(pesTimeUs, seiBuffer, outputs);
  }
}
