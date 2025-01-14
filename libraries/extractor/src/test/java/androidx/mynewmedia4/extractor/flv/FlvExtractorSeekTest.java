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
package androidx.mynewmedia4.extractor.flv;

import static androidx.mynewmedia4.test.utils.TestUtil.extractAllSamplesFromFile;
import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.util.Util;
import androidx.mynewmedia4.datasource.DefaultDataSource;
import androidx.mynewmedia4.extractor.SeekMap;
import androidx.mynewmedia4.test.utils.FakeExtractorOutput;
import androidx.mynewmedia4.test.utils.FakeTrackOutput;
import androidx.mynewmedia4.test.utils.TestUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Seeking tests for {@link FlvExtractor}. */
@RunWith(AndroidJUnit4.class)
public class FlvExtractorSeekTest {

  private static final String TEST_FILE_KEY_FRAME_INDEX =
      "media/flv/sample-with-key-frame-index.flv";
  private static final long DURATION_US = 3_042_000;
  private static final long KEY_FRAMES_INTERVAL_US = C.MICROS_PER_SECOND;

  private FlvExtractor extractor;
  private FakeExtractorOutput extractorOutput;
  private DefaultDataSource dataSource;

  @Before
  public void setUp() throws Exception {
    extractor = new FlvExtractor();
    extractorOutput = new FakeExtractorOutput();
    dataSource =
        new DefaultDataSource.Factory(ApplicationProvider.getApplicationContext())
            .createDataSource();
  }

  @Test
  public void flvExtractorReads_returnsSeekableSeekMap() throws Exception {
    Uri fileUri = TestUtil.buildAssetUri(TEST_FILE_KEY_FRAME_INDEX);

    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);

    assertThat(seekMap.getDurationUs()).isEqualTo(DURATION_US);
    assertThat(seekMap.isSeekable()).isTrue();
  }

  @Test
  public void seeking_handlesSeekToZero() throws Exception {
    String fileName = TEST_FILE_KEY_FRAME_INDEX;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    int trackId = extractorOutput.trackOutputs.keyAt(0);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(trackId);

    long targetSeekTimeUs = 0;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsWithinKeyFrameInterval(
        fileName, trackId, trackOutput, extractedFrameIndex, targetSeekTimeUs);
  }

  @Test
  public void seeking_handlesSeekToEof() throws Exception {
    String fileName = TEST_FILE_KEY_FRAME_INDEX;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    int trackId = extractorOutput.trackOutputs.keyAt(0);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(trackId);

    long targetSeekTimeUs = seekMap.getDurationUs();
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsWithinKeyFrameInterval(
        fileName, trackId, trackOutput, extractedFrameIndex, targetSeekTimeUs);
  }

  @Test
  public void seeking_handlesSeekingBackward() throws Exception {
    String fileName = TEST_FILE_KEY_FRAME_INDEX;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    int trackId = extractorOutput.trackOutputs.keyAt(0);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(trackId);

    long firstSeekTimeUs = seekMap.getDurationUs() * 2 / 3;
    TestUtil.seekToTimeUs(extractor, seekMap, firstSeekTimeUs, dataSource, trackOutput, fileUri);
    long targetSeekTimeUs = seekMap.getDurationUs() / 3;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsWithinKeyFrameInterval(
        fileName, trackId, trackOutput, extractedFrameIndex, targetSeekTimeUs);
  }

  @Test
  public void seeking_handlesSeekingForward() throws Exception {
    String fileName = TEST_FILE_KEY_FRAME_INDEX;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    int trackId = extractorOutput.trackOutputs.keyAt(0);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(trackId);

    long firstSeekTimeUs = seekMap.getDurationUs() / 3;
    TestUtil.seekToTimeUs(extractor, seekMap, firstSeekTimeUs, dataSource, trackOutput, fileUri);
    long targetSeekTimeUs = seekMap.getDurationUs() * 2 / 3;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsWithinKeyFrameInterval(
        fileName, trackId, trackOutput, extractedFrameIndex, targetSeekTimeUs);
  }

  private static void assertFirstFrameAfterSeekIsWithinKeyFrameInterval(
      String fileName,
      int trackId,
      FakeTrackOutput trackOutput,
      int firstFrameIndexAfterSeek,
      long targetSeekTimeUs)
      throws IOException {
    long foundFrameTimeUs = trackOutput.getSampleTimeUs(firstFrameIndexAfterSeek);
    assertThat(targetSeekTimeUs - foundFrameTimeUs).isAtMost(KEY_FRAMES_INTERVAL_US);

    FakeTrackOutput expectedTrackOutput = getTrackOutput(fileName, trackId);
    int foundFrameIndex = getFrameIndex(expectedTrackOutput, foundFrameTimeUs);

    trackOutput.assertSample(
        firstFrameIndexAfterSeek,
        expectedTrackOutput.getSampleData(foundFrameIndex),
        expectedTrackOutput.getSampleTimeUs(foundFrameIndex),
        expectedTrackOutput.getSampleFlags(foundFrameIndex),
        expectedTrackOutput.getSampleCryptoData(foundFrameIndex));
  }

  private static FakeTrackOutput getTrackOutput(String fileName, int trackId) throws IOException {
    return extractAllSamplesFromFile(
            new FlvExtractor(), ApplicationProvider.getApplicationContext(), fileName)
        .trackOutputs
        .get(trackId);
  }

  private static int getFrameIndex(FakeTrackOutput trackOutput, long targetSeekTimeUs) {
    List<Long> frameTimes = trackOutput.getSampleTimesUs();
    return Util.binarySearchFloor(
        frameTimes, targetSeekTimeUs, /* inclusive= */ true, /* stayInBounds= */ false);
  }
}
