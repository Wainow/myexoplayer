/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.mynewmedia4.muxer;

import static androidx.mynewmedia4.common.util.Assertions.checkNotNull;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import androidx.mynewmedia4.common.util.MediaFormatUtil;
import androidx.mynewmedia4.extractor.mp4.Mp4Extractor;
import androidx.mynewmedia4.test.utils.DumpFileAsserts;
import androidx.mynewmedia4.test.utils.FakeExtractorOutput;
import androidx.mynewmedia4.test.utils.TestUtil;
import androidx.test.core.app.ApplicationProvider;
import com.google.common.collect.ImmutableList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** End to end instrumentation tests for {@link Mp4Muxer}. */
@RunWith(Parameterized.class)
public class Mp4MuxerEndToEndTest {
  private static final String H264_MP4 = "sample.mp4";
  private static final String H265_HDR10_MP4 = "hdr10-720p.mp4";
  private static final String H265_WITH_METADATA_TRACK_MP4 = "h265_with_metadata_track.mp4";
  private static final String AV1_MP4 = "sample_av1.mp4";

  @Parameters(name = "{0}")
  public static ImmutableList<String> mediaSamples() {
    return ImmutableList.of(H264_MP4, H265_HDR10_MP4, H265_WITH_METADATA_TRACK_MP4, AV1_MP4);
  }

  @Parameter public @MonotonicNonNull String inputFile;
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String MP4_FILE_ASSET_DIRECTORY = "media/mp4/";
  private final Context context = ApplicationProvider.getApplicationContext();
  private @MonotonicNonNull String outputPath;
  private @MonotonicNonNull FileOutputStream outputStream;

  @Before
  public void setUp() throws Exception {
    outputPath = temporaryFolder.newFile("muxeroutput.mp4").getPath();
    outputStream = new FileOutputStream(outputPath);
  }

  @After
  public void tearDown() throws IOException {
    checkNotNull(outputStream).close();
  }

  @Test
  public void createMp4File_fromInputFileSampleData_matchesExpected() throws IOException {
    Mp4Muxer mp4Muxer = null;

    try {
      mp4Muxer = new Mp4Muxer.Builder(checkNotNull(outputStream)).build();
      mp4Muxer.setModificationTime(/* timestampMs= */ 500_000_000L);
      feedInputDataToMuxer(mp4Muxer, checkNotNull(inputFile));
    } finally {
      if (mp4Muxer != null) {
        mp4Muxer.close();
      }
    }

    FakeExtractorOutput fakeExtractorOutput =
        TestUtil.extractAllSamplesFromFilePath(new Mp4Extractor(), checkNotNull(outputPath));
    DumpFileAsserts.assertOutput(
        context, fakeExtractorOutput, AndroidMuxerTestUtil.getExpectedDumpFilePath(inputFile));
  }

  @Test
  public void createMp4File_muxerNotClosed_createsPartiallyWrittenValidFile() throws IOException {
    Mp4Muxer mp4Muxer = new Mp4Muxer.Builder(checkNotNull(outputStream)).build();
    mp4Muxer.setModificationTime(/* timestampMs= */ 500_000_000L);
    feedInputDataToMuxer(mp4Muxer, H265_HDR10_MP4);

    // Muxer not closed.

    // Audio sample written = 192 out of 195.
    // Video sample written = 94 out of 127.
    // Output is still a valid MP4 file.
    FakeExtractorOutput fakeExtractorOutput =
        TestUtil.extractAllSamplesFromFilePath(new Mp4Extractor(), checkNotNull(outputPath));
    DumpFileAsserts.assertOutput(
        context,
        fakeExtractorOutput,
        AndroidMuxerTestUtil.getExpectedDumpFilePath("partial_" + H265_HDR10_MP4));
  }

  private void feedInputDataToMuxer(Mp4Muxer mp4Muxer, String inputFileName) throws IOException {
    MediaExtractor extractor = new MediaExtractor();
    extractor.setDataSource(
        context.getResources().getAssets().openFd(MP4_FILE_ASSET_DIRECTORY + inputFileName));

    List<Mp4Muxer.TrackToken> addedTracks = new ArrayList<>();
    int sortKey = 0;
    for (int i = 0; i < extractor.getTrackCount(); i++) {
      Mp4Muxer.TrackToken trackToken =
          mp4Muxer.addTrack(
              sortKey++, MediaFormatUtil.createFormatFromMediaFormat(extractor.getTrackFormat(i)));
      addedTracks.add(trackToken);
      extractor.selectTrack(i);
    }

    do {
      MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
      bufferInfo.flags = extractor.getSampleFlags();
      bufferInfo.offset = 0;
      bufferInfo.presentationTimeUs = extractor.getSampleTime();
      int sampleSize = (int) extractor.getSampleSize();
      bufferInfo.size = sampleSize;

      ByteBuffer sampleBuffer = ByteBuffer.allocateDirect(sampleSize);
      extractor.readSampleData(sampleBuffer, /* offset= */ 0);

      sampleBuffer.rewind();

      mp4Muxer.writeSampleData(
          addedTracks.get(extractor.getSampleTrackIndex()), sampleBuffer, bufferInfo);
    } while (extractor.advance());

    extractor.release();
  }
}
