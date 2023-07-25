/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.mynewmedia4.test.utils;

import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.Timeline;
import androidx.mynewmedia4.common.Timeline.Period;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.common.util.Util;
import androidx.mynewmedia4.datasource.TransferListener;
import androidx.mynewmedia4.exoplayer.drm.DrmSessionEventListener;
import androidx.mynewmedia4.exoplayer.drm.DrmSessionManager;
import androidx.mynewmedia4.exoplayer.source.MediaPeriod;
import androidx.mynewmedia4.exoplayer.source.MediaSource;
import androidx.mynewmedia4.exoplayer.source.MediaSourceEventListener;
import androidx.mynewmedia4.exoplayer.source.TrackGroupArray;
import androidx.mynewmedia4.exoplayer.upstream.Allocator;

/**
 * Fake {@link MediaSource} that provides a given timeline. Creating the period returns a {@link
 * FakeAdaptiveMediaPeriod} from the given {@link TrackGroupArray}.
 */
@UnstableApi
public class FakeAdaptiveMediaSource extends FakeMediaSource {

  private final FakeChunkSource.Factory chunkSourceFactory;

  public FakeAdaptiveMediaSource(
      Timeline timeline,
      TrackGroupArray trackGroupArray,
      FakeChunkSource.Factory chunkSourceFactory) {
    super(
        timeline,
        DrmSessionManager.DRM_UNSUPPORTED,
        /* trackDataFactory= */ (unusedFormat, unusedMediaPeriodId) -> {
          throw new RuntimeException("Unused TrackDataFactory");
        },
        trackGroupArray);
    this.chunkSourceFactory = chunkSourceFactory;
  }

  @Override
  protected MediaPeriod createMediaPeriod(
      MediaPeriodId id,
      TrackGroupArray trackGroupArray,
      Allocator allocator,
      MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher,
      DrmSessionManager drmSessionManager,
      DrmSessionEventListener.EventDispatcher drmEventDispatcher,
      @Nullable TransferListener transferListener) {
    Period period = Util.castNonNull(getTimeline()).getPeriodByUid(id.periodUid, new Period());
    return new FakeAdaptiveMediaPeriod(
        trackGroupArray,
        mediaSourceEventDispatcher,
        allocator,
        chunkSourceFactory,
        period.durationUs,
        transferListener);
  }

  @Override
  public void releaseMediaPeriod(MediaPeriod mediaPeriod) {
    ((FakeAdaptiveMediaPeriod) mediaPeriod).release();
  }
}
