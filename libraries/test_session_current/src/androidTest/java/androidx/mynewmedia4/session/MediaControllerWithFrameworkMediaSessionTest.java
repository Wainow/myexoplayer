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
package androidx.mynewmedia4.session;

import static androidx.mynewmedia4.common.Player.STATE_READY;
import static androidx.mynewmedia4.test.session.common.TestUtils.SERVICE_CONNECTION_TIMEOUT_MS;
import static androidx.mynewmedia4.test.session.common.TestUtils.TIMEOUT_MS;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.content.Context;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.HandlerThread;
import androidx.mynewmedia4.common.Player;
import androidx.mynewmedia4.common.Player.State;
import androidx.mynewmedia4.common.util.Util;
import androidx.mynewmedia4.test.session.common.MainLooperTestRule;
import androidx.mynewmedia4.test.session.common.TestHandler;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link MediaController} with framework MediaSession, which exists since Android-L. */
@RunWith(AndroidJUnit4.class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP) // For framework MediaSession
public class MediaControllerWithFrameworkMediaSessionTest {

  private static final String TAG = "MCFMediaSessionTest";

  @ClassRule public static MainLooperTestRule mainLooperTestRule = new MainLooperTestRule();

  private Context context;
  private TestHandler handler;
  private MediaSession fwkSession;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();

    HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    TestHandler handler = new TestHandler(handlerThread.getLooper());
    this.handler = handler;

    fwkSession = new android.media.session.MediaSession(context, TAG);
    fwkSession.setActive(true);
    fwkSession.setFlags(
        MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    fwkSession.setCallback(new android.media.session.MediaSession.Callback() {}, handler);
  }

  @After
  public void cleanUp() {
    if (fwkSession != null) {
      fwkSession.release();
      fwkSession = null;
    }
    if (handler != null) {
      if (Util.SDK_INT >= 18) {
        handler.getLooper().quitSafely();
      } else {
        handler.getLooper().quit();
      }
      handler = null;
    }
  }

  @Test
  public void createController() throws Exception {
    SessionToken token =
        SessionToken.createSessionToken(context, fwkSession.getSessionToken())
            .get(TIMEOUT_MS, MILLISECONDS);
    MediaController controller =
        new MediaController.Builder(context, token)
            .setApplicationLooper(handler.getLooper())
            .buildAsync()
            .get(SERVICE_CONNECTION_TIMEOUT_MS, MILLISECONDS);
    handler.postAndSync(controller::release);
  }

  @Test
  public void onPlaybackStateChanged_isNotifiedByFwkSessionChanges() throws Exception {
    CountDownLatch playbackStateChangedLatch = new CountDownLatch(1);
    AtomicInteger playbackStateRef = new AtomicInteger();
    AtomicBoolean playWhenReadyRef = new AtomicBoolean();
    SessionToken token =
        SessionToken.createSessionToken(context, fwkSession.getSessionToken())
            .get(TIMEOUT_MS, MILLISECONDS);
    MediaController controller =
        new MediaController.Builder(context, token)
            .setApplicationLooper(handler.getLooper())
            .buildAsync()
            .get(SERVICE_CONNECTION_TIMEOUT_MS, MILLISECONDS);
    Player.Listener listener =
        new Player.Listener() {
          @Override
          public void onPlaybackStateChanged(@State int playbackState) {
            playbackStateRef.set(playbackState);
            playWhenReadyRef.set(controller.getPlayWhenReady());
            playbackStateChangedLatch.countDown();
          }
        };
    controller.addListener(listener);
    fwkSession.setPlaybackState(
        new PlaybackState.Builder()
            .setState(PlaybackState.STATE_PLAYING, /* position= */ 0, /* playbackSpeed= */ 1.0f)
            .build());
    try {
      assertThat(playbackStateChangedLatch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
      assertThat(playbackStateRef.get()).isEqualTo(STATE_READY);
      assertThat(playWhenReadyRef.get()).isTrue();
    } finally {
      handler.postAndSync(controller::release);
    }
  }
}
