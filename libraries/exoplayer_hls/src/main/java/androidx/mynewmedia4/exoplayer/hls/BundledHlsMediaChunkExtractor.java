/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.mynewmedia4.exoplayer.hls;

import androidx.annotation.VisibleForTesting;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.util.Assertions;
import androidx.mynewmedia4.common.util.TimestampAdjuster;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.extractor.Extractor;
import androidx.mynewmedia4.extractor.ExtractorInput;
import androidx.mynewmedia4.extractor.ExtractorOutput;
import androidx.mynewmedia4.extractor.PositionHolder;
import androidx.mynewmedia4.extractor.mp3.Mp3Extractor;
import androidx.mynewmedia4.extractor.mp4.FragmentedMp4Extractor;
import androidx.mynewmedia4.extractor.ts.Ac3Extractor;
import androidx.mynewmedia4.extractor.ts.Ac4Extractor;
import androidx.mynewmedia4.extractor.ts.AdtsExtractor;
import androidx.mynewmedia4.extractor.ts.TsExtractor;
import java.io.IOException;

/**
 * {@link HlsMediaChunkExtractor} implementation that uses ExoPlayer app-bundled {@link Extractor
 * Extractors}.
 */
@UnstableApi
public final class BundledHlsMediaChunkExtractor implements HlsMediaChunkExtractor {

  private static final PositionHolder POSITION_HOLDER = new PositionHolder();

  @VisibleForTesting /* package */ final Extractor extractor;
  private final Format multivariantPlaylistFormat;
  private final TimestampAdjuster timestampAdjuster;

  /**
   * Creates a new instance.
   *
   * @param extractor The underlying {@link Extractor}.
   * @param multivariantPlaylistFormat The {@link Format} obtained from the multivariant playlist.
   * @param timestampAdjuster A {@link TimestampAdjuster} to adjust sample timestamps.
   */
  public BundledHlsMediaChunkExtractor(
      Extractor extractor, Format multivariantPlaylistFormat, TimestampAdjuster timestampAdjuster) {
    this.extractor = extractor;
    this.multivariantPlaylistFormat = multivariantPlaylistFormat;
    this.timestampAdjuster = timestampAdjuster;
  }

  @Override
  public void init(ExtractorOutput extractorOutput) {
    extractor.init(extractorOutput);
  }

  @Override
  public boolean read(ExtractorInput extractorInput) throws IOException {
    return extractor.read(extractorInput, POSITION_HOLDER) == Extractor.RESULT_CONTINUE;
  }

  @Override
  public boolean isPackedAudioExtractor() {
    return extractor instanceof AdtsExtractor
        || extractor instanceof Ac3Extractor
        || extractor instanceof Ac4Extractor
        || extractor instanceof Mp3Extractor;
  }

  @Override
  public boolean isReusable() {
    return extractor instanceof TsExtractor || extractor instanceof FragmentedMp4Extractor;
  }

  @Override
  public HlsMediaChunkExtractor recreate() {
    Assertions.checkState(!isReusable());
    Extractor newExtractorInstance;
    if (extractor instanceof WebvttExtractor) {
      newExtractorInstance =
          new WebvttExtractor(multivariantPlaylistFormat.language, timestampAdjuster);
    } else if (extractor instanceof AdtsExtractor) {
      newExtractorInstance = new AdtsExtractor();
    } else if (extractor instanceof Ac3Extractor) {
      newExtractorInstance = new Ac3Extractor();
    } else if (extractor instanceof Ac4Extractor) {
      newExtractorInstance = new Ac4Extractor();
    } else if (extractor instanceof Mp3Extractor) {
      newExtractorInstance = new Mp3Extractor();
    } else {
      throw new IllegalStateException(
          "Unexpected extractor type for recreation: " + extractor.getClass().getSimpleName());
    }
    return new BundledHlsMediaChunkExtractor(
        newExtractorInstance, multivariantPlaylistFormat, timestampAdjuster);
  }

  @Override
  public void onTruncatedSegmentParsed() {
    extractor.seek(/* position= */ 0, /* timeUs= */ 0);
  }
}
