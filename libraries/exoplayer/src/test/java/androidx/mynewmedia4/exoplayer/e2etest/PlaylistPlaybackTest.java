/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.mynewmedia4.exoplayer.e2etest;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.Surface;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.MediaItem;
import androidx.mynewmedia4.common.MimeTypes;
import androidx.mynewmedia4.common.Player;
import androidx.mynewmedia4.exoplayer.ExoPlayer;
import androidx.mynewmedia4.exoplayer.source.DefaultMediaSourceFactory;
import androidx.mynewmedia4.exoplayer.source.MediaSource;
import androidx.mynewmedia4.test.utils.CapturingRenderersFactory;
import androidx.mynewmedia4.test.utils.DumpFileAsserts;
import androidx.mynewmedia4.test.utils.FakeClock;
import androidx.mynewmedia4.test.utils.robolectric.PlaybackOutput;
import androidx.mynewmedia4.test.utils.robolectric.ShadowMediaCodecConfig;
import androidx.mynewmedia4.test.utils.robolectric.TestPlayerRunHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** End-to-end tests for playlists. */
@RunWith(AndroidJUnit4.class)
public final class PlaylistPlaybackTest {

  @Rule
  public ShadowMediaCodecConfig mediaCodecConfig =
      ShadowMediaCodecConfig.forAllSupportedMimeTypes();

  @Test
  public void test_bypassOnThenOn() throws Exception {
    Context applicationContext = ApplicationProvider.getApplicationContext();
    CapturingRenderersFactory capturingRenderersFactory =
        new CapturingRenderersFactory(applicationContext);
    ExoPlayer player =
        new ExoPlayer.Builder(applicationContext, capturingRenderersFactory)
            .setClock(new FakeClock(/* isAutoAdvancing= */ true))
            .build();
    PlaybackOutput playbackOutput = PlaybackOutput.register(player, capturingRenderersFactory);

    player.addMediaItem(MediaItem.fromUri("asset:///media/wav/sample.wav"));
    player.addMediaItem(MediaItem.fromUri("asset:///media/mka/bear-opus.mka"));
    player.prepare();
    player.play();
    TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_ENDED);
    player.release();

    DumpFileAsserts.assertOutput(
        applicationContext, playbackOutput, "playbackdumps/playlists/bypass-on-then-off.dump");
  }

  @Test
  public void test_bypassOffThenOn() throws Exception {
    Context applicationContext = ApplicationProvider.getApplicationContext();
    CapturingRenderersFactory capturingRenderersFactory =
        new CapturingRenderersFactory(applicationContext);
    ExoPlayer player =
        new ExoPlayer.Builder(applicationContext, capturingRenderersFactory)
            .setClock(new FakeClock(/* isAutoAdvancing= */ true))
            .build();
    PlaybackOutput playbackOutput = PlaybackOutput.register(player, capturingRenderersFactory);

    player.addMediaItem(MediaItem.fromUri("asset:///media/mka/bear-opus.mka"));
    player.addMediaItem(MediaItem.fromUri("asset:///media/wav/sample.wav"));
    player.prepare();
    player.play();
    TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_ENDED);
    player.release();

    DumpFileAsserts.assertOutput(
        applicationContext, playbackOutput, "playbackdumps/playlists/bypass-off-then-on.dump");
  }

  @Test
  public void test_subtitle() throws Exception {
    Context applicationContext = ApplicationProvider.getApplicationContext();
    CapturingRenderersFactory capturingRenderersFactory =
        new CapturingRenderersFactory(applicationContext);
    MediaSource.Factory mediaSourceFactory =
        new DefaultMediaSourceFactory(applicationContext)
            .experimentalUseProgressiveMediaSourceForSubtitles(true);
    ExoPlayer player =
        new ExoPlayer.Builder(applicationContext, capturingRenderersFactory)
            .setClock(new FakeClock(/* isAutoAdvancing= */ true))
            .setMediaSourceFactory(mediaSourceFactory)
            .build();
    player.setVideoSurface(new Surface(new SurfaceTexture(/* texName= */ 1)));
    PlaybackOutput playbackOutput = PlaybackOutput.register(player, capturingRenderersFactory);

    player.addMediaItem(MediaItem.fromUri("asset:///media/mp4/preroll-5s.mp4"));
    MediaItem mediaItemWithSubtitle =
        new MediaItem.Builder()
            .setUri("asset:///media/mp4/preroll-5s.mp4")
            .setSubtitleConfigurations(
                ImmutableList.of(
                    new MediaItem.SubtitleConfiguration.Builder(
                            Uri.parse("asset:///media/webvtt/typical"))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()))
            .build();
    player.addMediaItem(mediaItemWithSubtitle);
    player.prepare();
    player.play();
    TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_ENDED);
    player.release();

    DumpFileAsserts.assertOutput(
        applicationContext, playbackOutput, "playbackdumps/playlists/playlist_with_subtitles.dump");
  }
}
