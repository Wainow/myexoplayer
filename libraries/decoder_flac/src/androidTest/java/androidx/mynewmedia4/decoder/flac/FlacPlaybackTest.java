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
package androidx.mynewmedia4.decoder.flac;

import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.MediaItem;
import androidx.mynewmedia4.common.PlaybackException;
import androidx.mynewmedia4.common.Player;
import androidx.mynewmedia4.datasource.DefaultDataSource;
import androidx.mynewmedia4.exoplayer.ExoPlayer;
import androidx.mynewmedia4.exoplayer.Renderer;
import androidx.mynewmedia4.exoplayer.RenderersFactory;
import androidx.mynewmedia4.exoplayer.audio.AudioSink;
import androidx.mynewmedia4.exoplayer.audio.DefaultAudioSink;
import androidx.mynewmedia4.exoplayer.source.MediaSource;
import androidx.mynewmedia4.exoplayer.source.ProgressiveMediaSource;
import androidx.mynewmedia4.extractor.mkv.MatroskaExtractor;
import androidx.mynewmedia4.test.utils.CapturingAudioSink;
import androidx.mynewmedia4.test.utils.DumpFileAsserts;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Playback tests using {@link LibflacAudioRenderer}. */
@RunWith(AndroidJUnit4.class)
public class FlacPlaybackTest {

  private static final String BEAR_FLAC_16BIT = "mka/bear-flac-16bit.mka";
  private static final String BEAR_FLAC_24BIT = "mka/bear-flac-24bit.mka";

  @Before
  public void setUp() {
    if (!FlacLibrary.isAvailable()) {
      fail("Flac library not available.");
    }
  }

  @Test
  public void test16BitPlayback() throws Exception {
    playAndAssertAudioSinkInput(BEAR_FLAC_16BIT);
  }

  @Test
  public void test24BitPlayback() throws Exception {
    playAndAssertAudioSinkInput(BEAR_FLAC_24BIT);
  }

  private static void playAndAssertAudioSinkInput(String fileName) throws Exception {
    CapturingAudioSink audioSink = new CapturingAudioSink(new DefaultAudioSink.Builder().build());

    TestPlaybackRunnable testPlaybackRunnable =
        new TestPlaybackRunnable(
            Uri.parse("asset:///media/" + fileName),
            ApplicationProvider.getApplicationContext(),
            audioSink);
    Thread thread = new Thread(testPlaybackRunnable);
    thread.start();
    thread.join();
    if (testPlaybackRunnable.playbackException != null) {
      throw testPlaybackRunnable.playbackException;
    }

    DumpFileAsserts.assertOutput(
        ApplicationProvider.getApplicationContext(),
        audioSink,
        "audiosinkdumps/" + fileName + ".audiosink.dump");
  }

  private static class TestPlaybackRunnable implements Player.Listener, Runnable {

    private final Context context;
    private final Uri uri;
    private final AudioSink audioSink;

    @Nullable private ExoPlayer player;
    @Nullable private PlaybackException playbackException;

    public TestPlaybackRunnable(Uri uri, Context context, AudioSink audioSink) {
      this.uri = uri;
      this.context = context;
      this.audioSink = audioSink;
    }

    @Override
    public void run() {
      Looper.prepare();
      RenderersFactory renderersFactory =
          (eventHandler,
              videoRendererEventListener,
              audioRendererEventListener,
              textRendererOutput,
              metadataRendererOutput) ->
              new Renderer[] {
                new LibflacAudioRenderer(eventHandler, audioRendererEventListener, audioSink)
              };
      player = new ExoPlayer.Builder(context, renderersFactory).build();
      player.addListener(this);
      MediaSource mediaSource =
          new ProgressiveMediaSource.Factory(
                  new DefaultDataSource.Factory(context), MatroskaExtractor.FACTORY)
              .createMediaSource(MediaItem.fromUri(uri));
      player.setMediaSource(mediaSource);
      player.prepare();
      player.play();
      Looper.loop();
    }

    @Override
    public void onPlayerError(PlaybackException error) {
      playbackException = error;
    }

    @Override
    public void onPlaybackStateChanged(@Player.State int playbackState) {
      if (playbackState == Player.STATE_ENDED
          || (playbackState == Player.STATE_IDLE && playbackException != null)) {
        player.release();
        Looper.myLooper().quit();
      }
    }
  }
}
