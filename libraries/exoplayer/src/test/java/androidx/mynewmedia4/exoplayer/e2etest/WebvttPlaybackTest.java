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
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

/** End-to-end tests using side-loaded WebVTT subtitles. */
@RunWith(ParameterizedRobolectricTestRunner.class)
public class WebvttPlaybackTest {
  @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
  public static ImmutableList<String> mediaSamples() {
    return ImmutableList.of("typical");
  }

  @ParameterizedRobolectricTestRunner.Parameter public String inputFile;

  @Rule
  public ShadowMediaCodecConfig mediaCodecConfig =
      ShadowMediaCodecConfig.forAllSupportedMimeTypes();

  @Test
  public void test() throws Exception {
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
    MediaItem mediaItem =
        new MediaItem.Builder()
            .setUri("asset:///media/mp4/preroll-5s.mp4")
            .setSubtitleConfigurations(
                ImmutableList.of(
                    new MediaItem.SubtitleConfiguration.Builder(
                            Uri.parse("asset:///media/webvtt/" + inputFile))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage("en")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()))
            .build();

    player.setMediaItem(mediaItem);
    player.prepare();
    player.play();
    TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_ENDED);
    player.release();

    DumpFileAsserts.assertOutput(
        applicationContext, playbackOutput, "playbackdumps/webvtt/" + inputFile + ".dump");
  }
}
