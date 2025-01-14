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
package androidx.mynewmedia4.extractor.mp3;

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
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link ConstantBitrateSeeker}. */
@RunWith(AndroidJUnit4.class)
public class ConstantBitrateSeekerTest {
  private static final String CONSTANT_FRAME_SIZE_TEST_FILE =
      "media/mp3/bear-cbr-constant-frame-size-no-seek-table.mp3";
  private static final String VARIABLE_FRAME_SIZE_TEST_FILE =
      "media/mp3/bear-cbr-variable-frame-size-no-seek-table.mp3";

  private Mp3Extractor extractor;
  private FakeExtractorOutput extractorOutput;
  private DefaultDataSource dataSource;

  @Before
  public void setUp() throws Exception {
    extractor = new Mp3Extractor();
    extractorOutput = new FakeExtractorOutput();
    dataSource =
        new DefaultDataSource.Factory(ApplicationProvider.getApplicationContext())
            .createDataSource();
  }

  @Test
  public void mp3ExtractorReads_returnSeekableCbrSeeker() throws IOException {
    Uri fileUri = TestUtil.buildAssetUri(CONSTANT_FRAME_SIZE_TEST_FILE);

    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);

    assertThat(seekMap.getClass()).isEqualTo(ConstantBitrateSeeker.class);
    assertThat(seekMap.getDurationUs()).isEqualTo(2_784_000);
    assertThat(seekMap.isSeekable()).isTrue();
  }

  @Test
  public void seeking_handlesSeekToZero() throws IOException {
    String fileName = CONSTANT_FRAME_SIZE_TEST_FILE;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(0);

    long targetSeekTimeUs = 0;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsExactFrame(
        fileName, trackOutput, targetSeekTimeUs, extractedFrameIndex);
  }

  @Test
  public void seeking_handlesSeekToEoF() throws IOException {
    String fileName = CONSTANT_FRAME_SIZE_TEST_FILE;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(0);

    long targetSeekTimeUs = seekMap.getDurationUs();
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsExactFrame(
        fileName, trackOutput, targetSeekTimeUs, extractedFrameIndex);
  }

  @Test
  public void seeking_handlesSeekingBackward() throws IOException {
    String fileName = CONSTANT_FRAME_SIZE_TEST_FILE;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(0);

    long firstSeekTimeUs = 1_234_000;
    TestUtil.seekToTimeUs(extractor, seekMap, firstSeekTimeUs, dataSource, trackOutput, fileUri);
    long targetSeekTimeUs = 987_000;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsExactFrame(
        fileName, trackOutput, targetSeekTimeUs, extractedFrameIndex);
  }

  @Test
  public void seeking_handlesSeekingForward() throws IOException {
    String fileName = CONSTANT_FRAME_SIZE_TEST_FILE;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(0);

    long firstSeekTimeUs = 987_000;
    TestUtil.seekToTimeUs(extractor, seekMap, firstSeekTimeUs, dataSource, trackOutput, fileUri);
    long targetSeekTimeUs = 1_234_000;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsExactFrame(
        fileName, trackOutput, targetSeekTimeUs, extractedFrameIndex);
  }

  @Test
  public void seeking_variableFrameSize_seeksNearlyExactlyToCorrectFrame() throws IOException {
    String fileName = VARIABLE_FRAME_SIZE_TEST_FILE;
    Uri fileUri = TestUtil.buildAssetUri(fileName);
    SeekMap seekMap = TestUtil.extractSeekMap(extractor, extractorOutput, dataSource, fileUri);
    FakeTrackOutput trackOutput = extractorOutput.trackOutputs.get(0);

    long targetSeekTimeUs = 1_234_000;
    int extractedFrameIndex =
        TestUtil.seekToTimeUs(
            extractor, seekMap, targetSeekTimeUs, dataSource, trackOutput, fileUri);

    assertThat(extractedFrameIndex).isNotEqualTo(C.INDEX_UNSET);
    assertFirstFrameAfterSeekIsWithin1FrameOfExactFrame(
        fileName, trackOutput, targetSeekTimeUs, extractedFrameIndex);
  }

  private static void assertFirstFrameAfterSeekIsExactFrame(
      String fileName,
      FakeTrackOutput trackOutput,
      long targetSeekTimeUs,
      int firstFrameIndexAfterSeek)
      throws IOException {
    FakeTrackOutput expectedTrackOutput = getExpectedTrackOutput(fileName);
    int exactFrameIndex = getFrameIndex(expectedTrackOutput, targetSeekTimeUs);

    assertThat(trackOutput.getSampleData(firstFrameIndexAfterSeek))
        .isEqualTo(expectedTrackOutput.getSampleData(exactFrameIndex));
  }

  private static void assertFirstFrameAfterSeekIsWithin1FrameOfExactFrame(
      String fileName,
      FakeTrackOutput trackOutput,
      long targetSeekTimeUs,
      int firstFrameIndexAfterSeek)
      throws IOException {
    FakeTrackOutput expectedTrackOutput = getExpectedTrackOutput(fileName);
    int exactFrameIndex = getFrameIndex(expectedTrackOutput, targetSeekTimeUs);

    boolean foundPreviousFrame =
        exactFrameIndex != 0
            && Arrays.equals(
                trackOutput.getSampleData(firstFrameIndexAfterSeek),
                expectedTrackOutput.getSampleData(exactFrameIndex - 1));
    boolean foundExactFrame =
        Arrays.equals(
            trackOutput.getSampleData(firstFrameIndexAfterSeek),
            expectedTrackOutput.getSampleData(exactFrameIndex));
    boolean foundNextFrame =
        exactFrameIndex != expectedTrackOutput.getSampleCount() - 1
            && Arrays.equals(
                trackOutput.getSampleData(firstFrameIndexAfterSeek),
                expectedTrackOutput.getSampleData(exactFrameIndex + 1));

    assertThat(foundPreviousFrame || foundExactFrame || foundNextFrame).isTrue();
  }

  private static FakeTrackOutput getExpectedTrackOutput(String fileName) throws IOException {
    return TestUtil.extractAllSamplesFromFile(
            new Mp3Extractor(), ApplicationProvider.getApplicationContext(), fileName)
        .trackOutputs
        .get(0);
  }

  private static int getFrameIndex(FakeTrackOutput trackOutput, long targetSeekTimeUs) {
    List<Long> frameTimes = trackOutput.getSampleTimesUs();
    return Util.binarySearchFloor(
        frameTimes, targetSeekTimeUs, /* inclusive= */ true, /* stayInBounds= */ false);
  }
}
