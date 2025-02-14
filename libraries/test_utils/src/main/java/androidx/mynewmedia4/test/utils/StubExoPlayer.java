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

import android.media.AudioDeviceInfo;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.AudioAttributes;
import androidx.mynewmedia4.common.AuxEffectInfo;
import androidx.mynewmedia4.common.Effect;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.Player;
import androidx.mynewmedia4.common.PriorityTaskManager;
import androidx.mynewmedia4.common.util.Clock;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.exoplayer.DecoderCounters;
import androidx.mynewmedia4.exoplayer.ExoPlaybackException;
import androidx.mynewmedia4.exoplayer.ExoPlayer;
import androidx.mynewmedia4.exoplayer.PlayerMessage;
import androidx.mynewmedia4.exoplayer.Renderer;
import androidx.mynewmedia4.exoplayer.SeekParameters;
import androidx.mynewmedia4.exoplayer.analytics.AnalyticsCollector;
import androidx.mynewmedia4.exoplayer.analytics.AnalyticsListener;
import androidx.mynewmedia4.exoplayer.source.MediaSource;
import androidx.mynewmedia4.exoplayer.source.ShuffleOrder;
import androidx.mynewmedia4.exoplayer.source.TrackGroupArray;
import androidx.mynewmedia4.exoplayer.trackselection.TrackSelectionArray;
import androidx.mynewmedia4.exoplayer.trackselection.TrackSelector;
import androidx.mynewmedia4.exoplayer.video.VideoFrameMetadataListener;
import androidx.mynewmedia4.exoplayer.video.spherical.CameraMotionListener;
import java.util.List;

/**
 * An abstract {@link ExoPlayer} implementation that throws {@link UnsupportedOperationException}
 * from every method.
 */
@UnstableApi
public class StubExoPlayer extends StubPlayer implements ExoPlayer {

  /**
   * @deprecated Use {@link ExoPlayer}, as the {@link AudioComponent} methods are defined by that
   *     interface.
   */
  @Override
  @Deprecated
  public AudioComponent getAudioComponent() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link ExoPlayer}, as the {@link VideoComponent} methods are defined by that
   *     interface.
   */
  @Override
  @Deprecated
  public VideoComponent getVideoComponent() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link Player}, as the {@link TextComponent} methods are defined by that
   *     interface.
   */
  @Override
  @Deprecated
  public TextComponent getTextComponent() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link Player}, as the {@link DeviceComponent} methods are defined by that
   *     interface.
   */
  @Override
  @Deprecated
  public DeviceComponent getDeviceComponent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Looper getPlaybackLooper() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Clock getClock() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAudioOffloadListener(AudioOffloadListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAudioOffloadListener(AudioOffloadListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnalyticsCollector getAnalyticsCollector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAnalyticsListener(AnalyticsListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAnalyticsListener(AnalyticsListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ExoPlaybackException getPlayerError() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link #setMediaSource(MediaSource)} and {@link #prepare()} instead.
   */
  @Deprecated
  @Override
  public void prepare(MediaSource mediaSource) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link #setMediaSource(MediaSource, boolean)} and {@link #prepare()} instead.
   */
  @Deprecated
  @Override
  public void prepare(MediaSource mediaSource, boolean resetPosition, boolean resetState) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMediaSource(MediaSource mediaSource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMediaSource(MediaSource mediaSource, long startPositionMs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMediaSource(MediaSource mediaSource, boolean resetPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMediaSources(List<MediaSource> mediaSources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMediaSources(List<MediaSource> mediaSources, boolean resetPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setMediaSources(
      List<MediaSource> mediaSources, int startMediaItemIndex, long startPositionMs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMediaSource(MediaSource mediaSource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMediaSource(int index, MediaSource mediaSource) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMediaSources(List<MediaSource> mediaSources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addMediaSources(int index, List<MediaSource> mediaSources) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setShuffleOrder(ShuffleOrder shuffleOrder) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAudioSessionId(int audioSessionId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getAudioSessionId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearAuxEffectInfo() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPreferredAudioDevice(@Nullable AudioDeviceInfo audioDeviceInfo) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSkipSilenceEnabled(boolean skipSilenceEnabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean getSkipSilenceEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVideoEffects(List<Effect> videoEffects) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVideoScalingMode(int videoScalingMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getVideoScalingMode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVideoChangeFrameRateStrategy(int videoChangeFrameRateStrategy) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getVideoChangeFrameRateStrategy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setCameraMotionListener(CameraMotionListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearCameraMotionListener(CameraMotionListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSeekParameters(@Nullable SeekParameters seekParameters) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SeekParameters getSeekParameters() {
    throw new UnsupportedOperationException();
  }

  @Override
  public PlayerMessage createMessage(PlayerMessage.Target target) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getRendererCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getRendererType(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Renderer getRenderer(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public TrackSelector getTrackSelector() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link #getCurrentTracks()}.
   */
  @Deprecated
  @Override
  public TrackGroupArray getCurrentTrackGroups() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Use {@link #getCurrentTracks()}.
   */
  @Deprecated
  @Override
  public TrackSelectionArray getCurrentTrackSelections() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setForegroundMode(boolean foregroundMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPauseAtEndOfMediaItems(boolean pauseAtEndOfMediaItems) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean getPauseAtEndOfMediaItems() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Format getAudioFormat() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Format getVideoFormat() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public DecoderCounters getAudioDecoderCounters() {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public DecoderCounters getVideoDecoderCounters() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setHandleAudioBecomingNoisy(boolean handleAudioBecomingNoisy) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWakeMode(int wakeMode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setPriorityTaskManager(@Nullable PriorityTaskManager priorityTaskManager) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void experimentalSetOffloadSchedulingEnabled(boolean offloadSchedulingEnabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean experimentalIsSleepingForOffload() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTunnelingEnabled() {
    throw new UnsupportedOperationException();
  }
}
