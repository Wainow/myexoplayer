/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.mynewmedia4.session;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.MediaItem;
import androidx.mynewmedia4.common.Player.PositionInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link SessionPositionInfo}. */
@RunWith(AndroidJUnit4.class)
public class SessionPositionInfoTest {

  @Test
  public void roundTripViaBundle_yieldsEqualInstance() {
    SessionPositionInfo testSessionPositionInfo =
        new SessionPositionInfo(
            new PositionInfo(
                /* windowUid= */ null,
                /* mediaItemIndex= */ 33,
                new MediaItem.Builder().setMediaId("1234").build(),
                /* periodUid= */ null,
                /* periodIndex= */ 44,
                /* positionMs= */ 233L,
                /* contentPositionMs= */ 333L,
                /* adGroupIndex= */ 2,
                /* adIndexInAdGroup= */ 8),
            /* isPlayingAd= */ true,
            /* eventTimeMs= */ 103L,
            /* durationMs= */ 400L,
            /* bufferedPositionMs= */ 200L,
            /* bufferedPercentage= */ 50,
            /* totalBufferedDurationMs= */ 500L,
            /* currentLiveOffsetMs= */ 20L,
            /* contentDurationMs= */ 400L,
            /* contentBufferedPositionMs= */ 223L);
    Bundle sessionPositionInfoBundle = testSessionPositionInfo.toBundle();
    SessionPositionInfo sessionPositionInfo =
        SessionPositionInfo.CREATOR.fromBundle(sessionPositionInfoBundle);
    assertThat(sessionPositionInfo).isEqualTo(testSessionPositionInfo);
  }

  @Test
  public void constructor_invalidIsPlayingAd_throwsIllegalArgumentException() {
    Assert.assertThrows(
        IllegalArgumentException.class,
        () ->
            new SessionPositionInfo(
                new PositionInfo(
                    /* windowUid= */ null,
                    /* mediaItemIndex= */ 33,
                    MediaItem.EMPTY,
                    /* periodUid= */ null,
                    /* periodIndex= */ 44,
                    /* positionMs= */ 233L,
                    /* contentPositionMs= */ 333L,
                    /* adGroupIndex= */ 2,
                    /* adIndexInAdGroup= */ C.INDEX_UNSET),
                /* isPlayingAd= */ false,
                /* eventTimeMs= */ 103L,
                /* durationMs= */ 400L,
                /* bufferedPositionMs= */ 200L,
                /* bufferedPercentage= */ 50,
                /* totalBufferedDurationMs= */ 500L,
                /* currentLiveOffsetMs= */ 20L,
                /* contentDurationMs= */ 400L,
                /* contentBufferedPositionMs= */ 223L));
  }
}
