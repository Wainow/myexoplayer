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

import static androidx.mynewmedia4.exoplayer.source.SampleStream.FLAG_REQUIRE_FORMAT;
import static androidx.mynewmedia4.test.utils.FakeSampleStream.FakeSampleStreamItem.END_OF_STREAM_ITEM;
import static androidx.mynewmedia4.test.utils.FakeSampleStream.FakeSampleStreamItem.oneByteSample;
import static com.google.common.truth.Truth.assertThat;

import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.TrackGroup;
import androidx.mynewmedia4.decoder.DecoderInputBuffer;
import androidx.mynewmedia4.exoplayer.FormatHolder;
import androidx.mynewmedia4.exoplayer.drm.DrmSessionEventListener;
import androidx.mynewmedia4.exoplayer.drm.DrmSessionManager;
import androidx.mynewmedia4.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.mynewmedia4.exoplayer.source.MediaSourceEventListener.EventDispatcher;
import androidx.mynewmedia4.exoplayer.trackselection.ExoTrackSelection;
import androidx.mynewmedia4.exoplayer.trackselection.FixedTrackSelection;
import androidx.mynewmedia4.exoplayer.upstream.DefaultAllocator;
import androidx.mynewmedia4.test.utils.FakeMediaPeriod;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.checkerframework.checker.nullness.compatqual.NullableType;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit test for {@link MergingMediaPeriod}. */
@RunWith(AndroidJUnit4.class)
public final class MergingMediaPeriodTest {

  private static final Format childFormat11 = new Format.Builder().setId("1_1").build();
  private static final Format childFormat12 = new Format.Builder().setId("1_2").build();
  private static final Format childFormat21 = new Format.Builder().setId("2_1").build();
  private static final Format childFormat22 = new Format.Builder().setId("2_2").build();

  @Test
  public void getTrackGroups_returnsAllChildTrackGroups() throws Exception {
    MergingMediaPeriod mergingMediaPeriod =
        prepareMergingPeriod(
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 0, childFormat11, childFormat12),
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 0, childFormat21, childFormat22));

    assertThat(mergingMediaPeriod.getTrackGroups().length).isEqualTo(4);
    assertThat(mergingMediaPeriod.getTrackGroups().get(0).getFormat(0)).isEqualTo(childFormat11);
    assertThat(mergingMediaPeriod.getTrackGroups().get(1).getFormat(0)).isEqualTo(childFormat12);
    assertThat(mergingMediaPeriod.getTrackGroups().get(2).getFormat(0)).isEqualTo(childFormat21);
    assertThat(mergingMediaPeriod.getTrackGroups().get(3).getFormat(0)).isEqualTo(childFormat22);
  }

  @Test
  public void selectTracks_createsSampleStreamsFromChildPeriods() throws Exception {
    MergingMediaPeriod mergingMediaPeriod =
        prepareMergingPeriod(
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 0, childFormat11, childFormat12),
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 0, childFormat21, childFormat22));

    ExoTrackSelection selectionForChild1 =
        new FixedTrackSelection(mergingMediaPeriod.getTrackGroups().get(1), /* track= */ 0);
    ExoTrackSelection selectionForChild2 =
        new FixedTrackSelection(mergingMediaPeriod.getTrackGroups().get(2), /* track= */ 0);
    SampleStream[] streams = new SampleStream[4];
    mergingMediaPeriod.selectTracks(
        /* selections= */ new ExoTrackSelection[] {
          null, selectionForChild1, selectionForChild2, null
        },
        /* mayRetainStreamFlags= */ new boolean[] {false, false, false, false},
        streams,
        /* streamResetFlags= */ new boolean[] {false, false, false, false},
        /* positionUs= */ 0);
    mergingMediaPeriod.continueLoading(/* positionUs= */ 0);

    assertThat(streams[0]).isNull();
    assertThat(streams[3]).isNull();

    FormatHolder formatHolder = new FormatHolder();
    DecoderInputBuffer inputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
    assertThat(streams[1].readData(formatHolder, inputBuffer, FLAG_REQUIRE_FORMAT))
        .isEqualTo(C.RESULT_FORMAT_READ);
    assertThat(formatHolder.format).isEqualTo(childFormat12);

    assertThat(streams[2].readData(formatHolder, inputBuffer, FLAG_REQUIRE_FORMAT))
        .isEqualTo(C.RESULT_FORMAT_READ);
    assertThat(formatHolder.format).isEqualTo(childFormat21);
  }

  @Test
  public void
      selectTracks_withPeriodOffsets_selectTracksWithOffset_andCreatesSampleStreamsCorrectingOffset()
          throws Exception {
    MergingMediaPeriod mergingMediaPeriod =
        prepareMergingPeriod(
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0,
                /* singleSampleTimeUs= */ 123_000,
                childFormat11,
                childFormat12),
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ -3000,
                /* singleSampleTimeUs= */ 456_000,
                childFormat21,
                childFormat22));

    ExoTrackSelection selectionForChild1 =
        new FixedTrackSelection(mergingMediaPeriod.getTrackGroups().get(0), /* track= */ 0);
    ExoTrackSelection selectionForChild2 =
        new FixedTrackSelection(mergingMediaPeriod.getTrackGroups().get(2), /* track= */ 0);
    SampleStream[] streams = new SampleStream[2];
    mergingMediaPeriod.selectTracks(
        /* selections= */ new ExoTrackSelection[] {selectionForChild1, selectionForChild2},
        /* mayRetainStreamFlags= */ new boolean[] {false, false},
        streams,
        /* streamResetFlags= */ new boolean[] {false, false},
        /* positionUs= */ 0);
    mergingMediaPeriod.continueLoading(/* positionUs= */ 0);
    FormatHolder formatHolder = new FormatHolder();
    DecoderInputBuffer inputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
    streams[0].readData(formatHolder, inputBuffer, FLAG_REQUIRE_FORMAT);
    streams[1].readData(formatHolder, inputBuffer, FLAG_REQUIRE_FORMAT);

    FakeMediaPeriodWithSelectionParameters childMediaPeriod1 =
        (FakeMediaPeriodWithSelectionParameters) mergingMediaPeriod.getChildPeriod(0);
    assertThat(childMediaPeriod1.selectTracksPositionUs).isEqualTo(0);
    assertThat(streams[0].readData(formatHolder, inputBuffer, /* readFlags= */ 0))
        .isEqualTo(C.RESULT_BUFFER_READ);
    assertThat(inputBuffer.timeUs).isEqualTo(123_000L);

    FakeMediaPeriodWithSelectionParameters childMediaPeriod2 =
        (FakeMediaPeriodWithSelectionParameters) mergingMediaPeriod.getChildPeriod(1);
    assertThat(childMediaPeriod2.selectTracksPositionUs).isEqualTo(3000L);
    assertThat(streams[1].readData(formatHolder, inputBuffer, /* readFlags= */ 0))
        .isEqualTo(C.RESULT_BUFFER_READ);
    assertThat(inputBuffer.timeUs).isEqualTo(456_000 - 3000);
  }

  @Test
  public void selectTracks_withSameArguments_forwardsEqualSelectionsToChildSources()
      throws Exception {
    MergingMediaPeriod mergingMediaPeriod =
        prepareMergingPeriod(
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 0, childFormat11),
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 0, childFormat22));
    FakeMediaPeriodWithSelectionParameters childMediaPeriod1 =
        (FakeMediaPeriodWithSelectionParameters) mergingMediaPeriod.getChildPeriod(0);
    FakeMediaPeriodWithSelectionParameters childMediaPeriod2 =
        (FakeMediaPeriodWithSelectionParameters) mergingMediaPeriod.getChildPeriod(1);

    TrackGroupArray mergedTrackGroups = mergingMediaPeriod.getTrackGroups();
    ExoTrackSelection[] selectionArray =
        new ExoTrackSelection[] {
          new FixedTrackSelection(mergedTrackGroups.get(0), /* track= */ 0),
          new FixedTrackSelection(mergedTrackGroups.get(1), /* track= */ 0)
        };

    mergingMediaPeriod.selectTracks(
        selectionArray,
        /* mayRetainStreamFlags= */ new boolean[2],
        /* streams= */ new SampleStream[2],
        /* streamResetFlags= */ new boolean[2],
        /* positionUs= */ 0);
    ExoTrackSelection firstSelectionChild1 = childMediaPeriod1.selectTracksSelections[0];
    ExoTrackSelection firstSelectionChild2 = childMediaPeriod2.selectTracksSelections[1];

    mergingMediaPeriod.selectTracks(
        selectionArray,
        /* mayRetainStreamFlags= */ new boolean[2],
        /* streams= */ new SampleStream[2],
        /* streamResetFlags= */ new boolean[2],
        /* positionUs= */ 0);
    ExoTrackSelection secondSelectionChild1 = childMediaPeriod1.selectTracksSelections[0];
    ExoTrackSelection secondSelectionChild2 = childMediaPeriod2.selectTracksSelections[1];

    assertThat(firstSelectionChild1).isEqualTo(secondSelectionChild1);
    assertThat(firstSelectionChild2).isEqualTo(secondSelectionChild2);
  }

  // https://github.com/google/ExoPlayer/issues/10930
  @Test
  public void selectTracks_withIdenticalFormats_selectsMatchingPeriod() throws Exception {
    MergingMediaPeriod mergingMediaPeriod =
        prepareMergingPeriod(
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ 0, /* singleSampleTimeUs= */ 123_000, childFormat11),
            new MergingPeriodDefinition(
                /* timeOffsetUs= */ -3000, /* singleSampleTimeUs= */ 456_000, childFormat11));

    ExoTrackSelection[] selectionArray = {
      new FixedTrackSelection(mergingMediaPeriod.getTrackGroups().get(1), /* track= */ 0)
    };

    SampleStream[] streams = new SampleStream[1];
    mergingMediaPeriod.selectTracks(
        selectionArray,
        /* mayRetainStreamFlags= */ new boolean[2],
        streams,
        /* streamResetFlags= */ new boolean[2],
        /* positionUs= */ 0);
    mergingMediaPeriod.continueLoading(/* positionUs= */ 0);

    FormatHolder formatHolder = new FormatHolder();
    DecoderInputBuffer inputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
    streams[0].readData(formatHolder, inputBuffer, FLAG_REQUIRE_FORMAT);

    assertThat(streams[0].readData(formatHolder, inputBuffer, /* readFlags= */ 0))
        .isEqualTo(C.RESULT_BUFFER_READ);
    assertThat(inputBuffer.timeUs).isEqualTo(456_000 - 3000);
  }

  private MergingMediaPeriod prepareMergingPeriod(MergingPeriodDefinition... definitions)
      throws Exception {
    MediaPeriod[] mediaPeriods = new MediaPeriod[definitions.length];
    long[] timeOffsetsUs = new long[definitions.length];
    for (int i = 0; i < definitions.length; i++) {
      MergingPeriodDefinition definition = definitions[i];
      timeOffsetsUs[i] = definition.timeOffsetUs;
      TrackGroup[] trackGroups = new TrackGroup[definition.formats.length];
      for (int j = 0; j < definition.formats.length; j++) {
        trackGroups[j] = new TrackGroup(definition.formats[j]);
      }
      mediaPeriods[i] =
          new FakeMediaPeriodWithSelectionParameters(
              new TrackGroupArray(trackGroups),
              new EventDispatcher()
                  .withParameters(/* windowIndex= */ i, new MediaPeriodId(/* periodUid= */ i)),
              /* trackDataFactory= */ (unusedFormat, unusedMediaPeriodId) ->
                  ImmutableList.of(
                      oneByteSample(definition.singleSampleTimeUs, C.BUFFER_FLAG_KEY_FRAME),
                      END_OF_STREAM_ITEM));
    }
    MergingMediaPeriod mergingMediaPeriod =
        new MergingMediaPeriod(
            new DefaultCompositeSequenceableLoaderFactory(), timeOffsetsUs, mediaPeriods);

    CountDownLatch prepareCountDown = new CountDownLatch(1);
    mergingMediaPeriod.prepare(
        new MediaPeriod.Callback() {
          @Override
          public void onPrepared(MediaPeriod mediaPeriod) {
            prepareCountDown.countDown();
          }

          @Override
          public void onContinueLoadingRequested(MediaPeriod source) {
            mergingMediaPeriod.continueLoading(/* positionUs= */ 0);
          }
        },
        /* positionUs= */ 0);
    prepareCountDown.await();

    return mergingMediaPeriod;
  }

  private static final class FakeMediaPeriodWithSelectionParameters extends FakeMediaPeriod {

    public @NullableType ExoTrackSelection[] selectTracksSelections;
    public long selectTracksPositionUs;

    public FakeMediaPeriodWithSelectionParameters(
        TrackGroupArray trackGroupArray,
        EventDispatcher mediaSourceEventDispatcher,
        TrackDataFactory trackDataFactory) {
      super(
          trackGroupArray,
          new DefaultAllocator(/* trimOnReset= */ false, /* individualAllocationSize= */ 1024),
          trackDataFactory,
          mediaSourceEventDispatcher,
          DrmSessionManager.DRM_UNSUPPORTED,
          new DrmSessionEventListener.EventDispatcher(),
          /* deferOnPrepared= */ false);
      selectTracksSelections = new ExoTrackSelection[trackGroupArray.length];
      selectTracksPositionUs = C.TIME_UNSET;
    }

    @Override
    public long selectTracks(
        @NullableType ExoTrackSelection[] selections,
        boolean[] mayRetainStreamFlags,
        @NullableType SampleStream[] streams,
        boolean[] streamResetFlags,
        long positionUs) {
      selectTracksSelections = Arrays.copyOf(selections, selections.length);
      selectTracksPositionUs = positionUs;
      return super.selectTracks(
          selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
    }
  }

  private static final class MergingPeriodDefinition {

    public final long timeOffsetUs;
    public final long singleSampleTimeUs;
    public final Format[] formats;

    public MergingPeriodDefinition(long timeOffsetUs, long singleSampleTimeUs, Format... formats) {
      this.timeOffsetUs = timeOffsetUs;
      this.singleSampleTimeUs = singleSampleTimeUs;
      this.formats = formats;
    }
  }
}
