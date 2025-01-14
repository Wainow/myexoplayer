/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.mynewmedia4.exoplayer.source;

import static androidx.mynewmedia4.test.utils.robolectric.RobolectricUtil.runMainLooperUntil;
import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.datasource.AssetDataSource;
import androidx.mynewmedia4.exoplayer.analytics.PlayerId;
import androidx.mynewmedia4.exoplayer.drm.DrmSessionEventListener;
import androidx.mynewmedia4.exoplayer.drm.DrmSessionManager;
import androidx.mynewmedia4.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.mynewmedia4.exoplayer.upstream.DefaultAllocator;
import androidx.mynewmedia4.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.mynewmedia4.extractor.mp4.Mp4Extractor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit test for {@link ProgressiveMediaPeriod}. */
@RunWith(AndroidJUnit4.class)
public final class ProgressiveMediaPeriodTest {

  @Test
  public void prepareUsingBundledExtractors_updatesSourceInfoBeforeOnPreparedCallback()
      throws TimeoutException {
    testExtractorsUpdatesSourceInfoBeforeOnPreparedCallback(
        new BundledExtractorsAdapter(Mp4Extractor.FACTORY));
  }

  @Test
  public void prepareUsingMediaParser_updatesSourceInfoBeforeOnPreparedCallback()
      throws TimeoutException {
    testExtractorsUpdatesSourceInfoBeforeOnPreparedCallback(
        new MediaParserExtractorAdapter(PlayerId.UNSET));
  }

  private static void testExtractorsUpdatesSourceInfoBeforeOnPreparedCallback(
      ProgressiveMediaExtractor extractor) throws TimeoutException {
    AtomicBoolean sourceInfoRefreshCalled = new AtomicBoolean(false);
    ProgressiveMediaPeriod.Listener sourceInfoRefreshListener =
        (durationUs, isSeekable, isLive) -> sourceInfoRefreshCalled.set(true);
    MediaPeriodId mediaPeriodId = new MediaPeriodId(/* periodUid= */ new Object());
    ProgressiveMediaPeriod mediaPeriod =
        new ProgressiveMediaPeriod(
            Uri.parse("asset://android_asset/media/mp4/sample.mp4"),
            new AssetDataSource(ApplicationProvider.getApplicationContext()),
            extractor,
            DrmSessionManager.DRM_UNSUPPORTED,
            new DrmSessionEventListener.EventDispatcher()
                .withParameters(/* windowIndex= */ 0, mediaPeriodId),
            new DefaultLoadErrorHandlingPolicy(),
            new MediaSourceEventListener.EventDispatcher()
                .withParameters(/* windowIndex= */ 0, mediaPeriodId),
            sourceInfoRefreshListener,
            new DefaultAllocator(/* trimOnReset= */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
            /* customCacheKey= */ null,
            ProgressiveMediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES);

    AtomicBoolean prepareCallbackCalled = new AtomicBoolean(false);
    AtomicBoolean sourceInfoRefreshCalledBeforeOnPrepared = new AtomicBoolean(false);
    mediaPeriod.prepare(
        new MediaPeriod.Callback() {
          @Override
          public void onPrepared(MediaPeriod mediaPeriod) {
            sourceInfoRefreshCalledBeforeOnPrepared.set(sourceInfoRefreshCalled.get());
            prepareCallbackCalled.set(true);
          }

          @Override
          public void onContinueLoadingRequested(MediaPeriod source) {
            source.continueLoading(/* positionUs= */ 0);
          }
        },
        /* positionUs= */ 0);
    runMainLooperUntil(prepareCallbackCalled::get);
    mediaPeriod.release();

    assertThat(sourceInfoRefreshCalledBeforeOnPrepared.get()).isTrue();
  }
}
