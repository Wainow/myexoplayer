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
package androidx.mynewmedia4.exoplayer;

import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.MediaPeriodId;
import androidx.mynewmedia4.common.Timeline;
import androidx.mynewmedia4.common.TrackGroup;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.exoplayer.source.MediaPeriod;
import androidx.mynewmedia4.exoplayer.source.TrackGroupArray;
import androidx.mynewmedia4.exoplayer.trackselection.ExoTrackSelection;
import androidx.mynewmedia4.exoplayer.upstream.Allocator;

/** Controls buffering of media. */
@UnstableApi
public interface LoadControl {

  /**
   * @deprecated Used as a placeholder when MediaPeriodId is unknown. Only used when the deprecated
   *     methods {@link #onTracksSelected(Renderer[], TrackGroupArray, ExoTrackSelection[])} or
   *     {@link #shouldStartPlayback(long, float, boolean, long)} are called.
   */
  @Deprecated
  MediaPeriodId EMPTY_MEDIA_PERIOD_ID = new MediaPeriodId(/* periodUid= */ new Object());

  /** Called by the player when prepared with a new source. */
  void onPrepared();

  /**
   * Called by the player when a track selection occurs.
   *
   * @param timeline The current {@link Timeline} in ExoPlayer. Can be {@link Timeline#EMPTY} only
   *     when the deprecated {@link #onTracksSelected(Renderer[], TrackGroupArray,
   *     ExoTrackSelection[])} was called.
   * @param mediaPeriodId Identifies (in the current timeline) the {@link MediaPeriod} for which the
   *     selection was made. Will be {@link #EMPTY_MEDIA_PERIOD_ID} when {@code timeline} is empty.
   * @param renderers The renderers.
   * @param trackGroups The {@link TrackGroup}s from which the selection was made.
   * @param trackSelections The track selections that were made.
   */
  @SuppressWarnings("deprecation") // Calling deprecated version of this method.
  default void onTracksSelected(
      Timeline timeline,
      MediaPeriodId mediaPeriodId,
      Renderer[] renderers,
      TrackGroupArray trackGroups,
      ExoTrackSelection[] trackSelections) {
    onTracksSelected(renderers, trackGroups, trackSelections);
  }

  /**
   * @deprecated Implement {@link #onTracksSelected(Timeline, MediaPeriodId, Renderer[],
   *     TrackGroupArray, ExoTrackSelection[])} instead.
   */
  @Deprecated
  default void onTracksSelected(
      Renderer[] renderers, TrackGroupArray trackGroups, ExoTrackSelection[] trackSelections) {
    onTracksSelected(
        Timeline.EMPTY, EMPTY_MEDIA_PERIOD_ID, renderers, trackGroups, trackSelections);
  }

  /** Called by the player when stopped. */
  void onStopped();

  /** Called by the player when released. */
  void onReleased();

  /** Returns the {@link Allocator} that should be used to obtain media buffer allocations. */
  Allocator getAllocator();

  /**
   * Returns the duration of media to retain in the buffer prior to the current playback position,
   * for fast backward seeking.
   *
   * <p>Note: If {@link #retainBackBufferFromKeyframe()} is false then seeking in the back-buffer
   * will only be fast if the back-buffer contains a keyframe prior to the seek position.
   *
   * <p>Note: Implementations should return a single value. Dynamic changes to the back-buffer are
   * not currently supported.
   *
   * @return The duration of media to retain in the buffer prior to the current playback position,
   *     in microseconds.
   */
  long getBackBufferDurationUs();

  /**
   * Returns whether media should be retained from the keyframe before the current playback position
   * minus {@link #getBackBufferDurationUs()}, rather than any sample before or at that position.
   *
   * <p>Warning: Returning true will cause the back-buffer size to depend on the spacing of
   * keyframes in the media being played. Returning true is not recommended unless you control the
   * media and are comfortable with the back-buffer size exceeding {@link
   * #getBackBufferDurationUs()} by as much as the maximum duration between adjacent keyframes in
   * the media.
   *
   * <p>Note: Implementations should return a single value. Dynamic changes to the back-buffer are
   * not currently supported.
   *
   * @return Whether media should be retained from the keyframe before the current playback position
   *     minus {@link #getBackBufferDurationUs()}, rather than any sample before or at that
   *     position.
   */
  boolean retainBackBufferFromKeyframe();

  /**
   * Called by the player to determine whether it should continue to load the source. If this method
   * returns true, the {@link MediaPeriod} identified in the most recent {@link #onTracksSelected}
   * call will continue being loaded.
   *
   * @param playbackPositionUs The current playback position in microseconds, relative to the start
   *     of the {@link Timeline.Period period} that will continue to be loaded if this method
   *     returns {@code true}. If playback of this period has not yet started, the value will be
   *     negative and equal in magnitude to the duration of any media in previous periods still to
   *     be played.
   * @param bufferedDurationUs The duration of media that's currently buffered.
   * @param playbackSpeed The current factor by which playback is sped up.
   * @return Whether the loading should continue.
   */
  boolean shouldContinueLoading(
      long playbackPositionUs, long bufferedDurationUs, float playbackSpeed);

  /**
   * Called repeatedly by the player when it's loading the source, has yet to start playback, and
   * has the minimum amount of data necessary for playback to be started. The value returned
   * determines whether playback is actually started. The load control may opt to return {@code
   * false} until some condition has been met (e.g. a certain amount of media is buffered).
   *
   * @param timeline The current {@link Timeline} in ExoPlayer. Can be {@link Timeline#EMPTY} only
   *     when the deprecated {@link #shouldStartPlayback(long, float, boolean, long)} was called.
   * @param mediaPeriodId Identifies (in the current timeline) the {@link MediaPeriod} for which
   *     playback will start. Will be {@link #EMPTY_MEDIA_PERIOD_ID} when {@code timeline} is empty.
   * @param bufferedDurationUs The duration of media that's currently buffered.
   * @param playbackSpeed The current factor by which playback is sped up.
   * @param rebuffering Whether the player is rebuffering. A rebuffer is defined to be caused by
   *     buffer depletion rather than a user action. Hence this parameter is false during initial
   *     buffering and when buffering as a result of a seek operation.
   * @param targetLiveOffsetUs The desired playback position offset to the live edge in
   *     microseconds, or {@link C#TIME_UNSET} if the media is not a live stream or no offset is
   *     configured.
   * @return Whether playback should be allowed to start or resume.
   */
  @SuppressWarnings("deprecation") // Calling deprecated version of this method.
  default boolean shouldStartPlayback(
      Timeline timeline,
      MediaPeriodId mediaPeriodId,
      long bufferedDurationUs,
      float playbackSpeed,
      boolean rebuffering,
      long targetLiveOffsetUs) {
    return shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering, targetLiveOffsetUs);
  }

  /**
   * @deprecated Implement {@link #shouldStartPlayback(Timeline, MediaPeriodId, long, float,
   *     boolean, long)} instead.
   */
  @Deprecated
  default boolean shouldStartPlayback(
      long bufferedDurationUs, float playbackSpeed, boolean rebuffering, long targetLiveOffsetUs) {
    return shouldStartPlayback(
        Timeline.EMPTY,
        EMPTY_MEDIA_PERIOD_ID,
        bufferedDurationUs,
        playbackSpeed,
        rebuffering,
        targetLiveOffsetUs);
  }
}
