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
package androidx.mynewmedia4.test.utils;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaCodec;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.Timeline;
import androidx.mynewmedia4.common.util.Assertions;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.common.util.Util;
import androidx.mynewmedia4.database.DatabaseProvider;
import androidx.mynewmedia4.database.DefaultDatabaseProvider;
import androidx.mynewmedia4.datasource.DataSource;
import androidx.mynewmedia4.datasource.DataSourceUtil;
import androidx.mynewmedia4.datasource.DataSpec;
import androidx.mynewmedia4.extractor.DefaultExtractorInput;
import androidx.mynewmedia4.extractor.Extractor;
import androidx.mynewmedia4.extractor.ExtractorInput;
import androidx.mynewmedia4.extractor.PositionHolder;
import androidx.mynewmedia4.extractor.SeekMap;
import androidx.mynewmedia4.extractor.metadata.MetadataInputBuffer;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;
import com.google.common.truth.Correspondence;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/** Utility methods for tests. */
@UnstableApi
public class TestUtil {

  private TestUtil() {}

  /**
   * Equivalent to {@code buildTestData(length, length)}.
   *
   * @param length The length of the array.
   * @return The generated array.
   */
  public static byte[] buildTestData(int length) {
    return buildTestData(length, length);
  }

  /**
   * Generates an array of random bytes with the specified length.
   *
   * @param length The length of the array.
   * @param seed A seed for an internally created {@link Random source of randomness}.
   * @return The generated array.
   */
  public static byte[] buildTestData(int length, int seed) {
    return buildTestData(length, new Random(seed));
  }

  /**
   * Generates an array of random bytes with the specified length.
   *
   * @param length The length of the array.
   * @param random A source of randomness.
   * @return The generated array.
   */
  public static byte[] buildTestData(int length, Random random) {
    byte[] source = new byte[length];
    random.nextBytes(source);
    return source;
  }

  /**
   * Generates a random string with the specified length.
   *
   * @param length The length of the string.
   * @param random A source of randomness.
   * @return The generated string.
   */
  public static String buildTestString(int length, Random random) {
    char[] chars = new char[length];
    for (int i = 0; i < length; i++) {
      chars[i] = (char) random.nextInt();
    }
    return new String(chars);
  }

  /**
   * Converts an array of integers in the range [0, 255] into an equivalent byte array.
   *
   * @param bytes An array of integers, all of which must be in the range [0, 255].
   * @return The equivalent byte array.
   */
  public static byte[] createByteArray(int... bytes) {
    byte[] array = new byte[bytes.length];
    for (int i = 0; i < array.length; i++) {
      Assertions.checkState(0x00 <= bytes[i] && bytes[i] <= 0xFF);
      array[i] = (byte) bytes[i];
    }
    return array;
  }

  /** Gets the underlying data of the {@link ByteBuffer} as a {@code float[]}. */
  public static float[] createFloatArray(ByteBuffer byteBuffer) {
    FloatBuffer buffer = byteBuffer.asFloatBuffer();
    float[] content = new float[buffer.remaining()];
    buffer.get(content);
    return content;
  }

  /** Creates a {@link ByteBuffer} containing the {@code data}. */
  public static ByteBuffer createByteBuffer(float[] data) {
    ByteBuffer buffer =
        ByteBuffer.allocateDirect(data.length * C.BYTES_PER_FLOAT).order(ByteOrder.nativeOrder());
    buffer.asFloatBuffer().put(data);
    return buffer;
  }

  /** Creates a {@link ByteBuffer} containing the {@code data}. */
  public static ByteBuffer createByteBuffer(short[] data) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(data.length * 2).order(ByteOrder.nativeOrder());
    buffer.asShortBuffer().put(data);
    return buffer;
  }

  /**
   * Converts an array of integers in the range [0, 255] into an equivalent byte list.
   *
   * @param bytes An array of integers, all of which must be in the range [0, 255].
   * @return The equivalent byte list.
   */
  public static ImmutableList<Byte> createByteList(int... bytes) {
    return ImmutableList.copyOf(Bytes.asList(createByteArray(bytes)));
  }

  /** Writes one byte long test data to the file and returns it. */
  public static File createTestFile(File directory, String name) throws IOException {
    return createTestFile(directory, name, /* length= */ 1);
  }

  /** Writes test data with the specified length to the file and returns it. */
  public static File createTestFile(File directory, String name, long length) throws IOException {
    return createTestFile(new File(directory, name), length);
  }

  /** Writes test data with the specified length to the file and returns it. */
  public static File createTestFile(File file, long length) throws IOException {
    FileOutputStream output = new FileOutputStream(file);
    for (long i = 0; i < length; i++) {
      output.write((int) i);
    }
    output.close();
    return file;
  }

  /** Returns the bytes of an asset file. */
  public static byte[] getByteArray(Context context, String fileName) throws IOException {
    return Util.toByteArray(getInputStream(context, fileName));
  }

  /** Returns the bytes of a file using its file path. */
  public static byte[] getByteArrayFromFilePath(String filePath) throws IOException {
    InputStream inputStream = new FileInputStream(filePath);
    return Util.toByteArray(inputStream);
  }

  /** Returns an {@link InputStream} for reading from an asset file. */
  public static InputStream getInputStream(Context context, String fileName) throws IOException {
    return context.getResources().getAssets().open(fileName);
  }

  /** Returns a {@link String} read from an asset file. */
  public static String getString(Context context, String fileName) throws IOException {
    return Util.fromUtf8Bytes(getByteArray(context, fileName));
  }

  /** Returns a {@link Bitmap} read from an asset file. */
  public static Bitmap getBitmap(Context context, String fileName) throws IOException {
    return BitmapFactory.decodeStream(getInputStream(context, fileName));
  }

  /** Returns a {@link DatabaseProvider} that provides an in-memory database. */
  public static DatabaseProvider getInMemoryDatabaseProvider() {
    return new DefaultDatabaseProvider(
        new SQLiteOpenHelper(
            /* context= */ null, /* name= */ null, /* factory= */ null, /* version= */ 1) {
          @Override
          public void onCreate(SQLiteDatabase db) {
            // Do nothing.
          }

          @Override
          public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Do nothing.
          }
        });
  }

  /**
   * Asserts that the actual timelines are the same to the expected timelines. This assert differs
   * from testing equality by not comparing:
   *
   * <ul>
   *   <li>Period IDs, which may be different due to ID mapping of child source period IDs.
   *   <li>Shuffle order, which by default is random and non-deterministic.
   * </ul>
   *
   * @param actualTimelines A list of actual {@link Timeline timelines}.
   * @param expectedTimelines A list of expected {@link Timeline timelines}.
   */
  public static void assertTimelinesSame(
      List<Timeline> actualTimelines, List<Timeline> expectedTimelines) {
    assertThat(actualTimelines)
        .comparingElementsUsing(
            Correspondence.from(
                TestUtil::timelinesAreSame, "is equal to (ignoring Window.uid and Period.uid)"))
        .containsExactlyElementsIn(expectedTimelines)
        .inOrder();
  }

  /**
   * Returns true if {@code thisTimeline} is equal to {@code thatTimeline}, ignoring {@link
   * Timeline.Window#uid} and {@link Timeline.Period#uid} values, and shuffle order.
   */
  public static boolean timelinesAreSame(Timeline thisTimeline, Timeline thatTimeline) {
    return new NoUidOrShufflingTimeline(thisTimeline)
        .equals(new NoUidOrShufflingTimeline(thatTimeline));
  }

  /**
   * Asserts that data read from a {@link DataSource} matches {@code expected}.
   *
   * @param dataSource The {@link DataSource} through which to read.
   * @param dataSpec The {@link DataSpec} to use when opening the {@link DataSource}.
   * @param expectedData The expected data.
   * @param expectKnownLength Whether to assert that {@link DataSource#open} returns the expected
   *     data length. If false then it's asserted that {@link C#LENGTH_UNSET} is returned.
   * @throws IOException If an error occurs reading fom the {@link DataSource}.
   */
  public static void assertDataSourceContent(
      DataSource dataSource, DataSpec dataSpec, byte[] expectedData, boolean expectKnownLength)
      throws IOException {
    try {
      long length = dataSource.open(dataSpec);
      assertThat(length).isEqualTo(expectKnownLength ? expectedData.length : C.LENGTH_UNSET);
      byte[] readData = DataSourceUtil.readToEnd(dataSource);
      assertThat(readData).isEqualTo(expectedData);
    } finally {
      dataSource.close();
    }
  }

  /** Returns whether two {@link android.media.MediaCodec.BufferInfo BufferInfos} are equal. */
  public static void assertBufferInfosEqual(
      MediaCodec.BufferInfo expected, MediaCodec.BufferInfo actual) {
    assertThat(actual.flags).isEqualTo(expected.flags);
    assertThat(actual.offset).isEqualTo(expected.offset);
    assertThat(actual.presentationTimeUs).isEqualTo(expected.presentationTimeUs);
    assertThat(actual.size).isEqualTo(expected.size);
  }

  /**
   * Asserts whether actual bitmap is very similar to the expected bitmap at some quality level.
   *
   * <p>This is defined as their PSNR value is greater than or equal to the threshold. The higher
   * the threshold, the more similar they are.
   *
   * @param expectedBitmap The expected bitmap.
   * @param actualBitmap The actual bitmap.
   * @param psnrThresholdDb The PSNR threshold (in dB), at or above which bitmaps are considered
   *     very similar.
   */
  public static void assertBitmapsAreSimilar(
      Bitmap expectedBitmap, Bitmap actualBitmap, double psnrThresholdDb) {
    assertThat(getPsnr(expectedBitmap, actualBitmap)).isAtLeast(psnrThresholdDb);
  }

  /**
   * Calculates the Peak-Signal-to-Noise-Ratio value for 2 bitmaps.
   *
   * <p>This is the logarithmic decibel(dB) value of the average mean-squared-error of normalized
   * (0.0-1.0) R/G/B values from the two bitmaps. The higher the value, the more similar they are.
   *
   * @param firstBitmap The first bitmap.
   * @param secondBitmap The second bitmap.
   * @return The PSNR value calculated from these 2 bitmaps.
   */
  private static double getPsnr(Bitmap firstBitmap, Bitmap secondBitmap) {
    assertThat(firstBitmap.getWidth()).isEqualTo(secondBitmap.getWidth());
    assertThat(firstBitmap.getHeight()).isEqualTo(secondBitmap.getHeight());
    long mse = 0;
    for (int i = 0; i < firstBitmap.getWidth(); i++) {
      for (int j = 0; j < firstBitmap.getHeight(); j++) {
        int firstColorInt = firstBitmap.getPixel(i, j);
        int firstRed = Color.red(firstColorInt);
        int firstGreen = Color.green(firstColorInt);
        int firstBlue = Color.blue(firstColorInt);
        int secondColorInt = secondBitmap.getPixel(i, j);
        int secondRed = Color.red(secondColorInt);
        int secondGreen = Color.green(secondColorInt);
        int secondBlue = Color.blue(secondColorInt);
        mse +=
            ((firstRed - secondRed) * (firstRed - secondRed)
                + (firstGreen - secondGreen) * (firstGreen - secondGreen)
                + (firstBlue - secondBlue) * (firstBlue - secondBlue));
      }
    }
    double normalizedMse =
        mse / (255.0 * 255.0 * 3.0 * firstBitmap.getWidth() * firstBitmap.getHeight());
    return 10 * Math.log10(1.0 / normalizedMse);
  }

  /** Returns the {@link Uri} for the given asset path. */
  public static Uri buildAssetUri(String assetPath) {
    return Uri.parse("asset:///" + assetPath);
  }

  /**
   * Reads from the given input using the given {@link Extractor}, until it can produce the {@link
   * SeekMap} and all of the track formats have been identified, or until the extractor encounters
   * EOF.
   *
   * @param extractor The {@link Extractor} to extractor from input.
   * @param output The {@link FakeTrackOutput} to store the extracted {@link SeekMap} and track.
   * @param dataSource The {@link DataSource} that will be used to read from the input.
   * @param uri The Uri of the input.
   * @return The extracted {@link SeekMap}.
   * @throws IOException If an error occurred reading from the input, or if the extractor finishes
   *     reading from input without extracting any {@link SeekMap}.
   */
  public static SeekMap extractSeekMap(
      Extractor extractor, FakeExtractorOutput output, DataSource dataSource, Uri uri)
      throws IOException {
    ExtractorInput input = getExtractorInputFromPosition(dataSource, /* position= */ 0, uri);
    extractor.init(output);
    PositionHolder positionHolder = new PositionHolder();
    int readResult = Extractor.RESULT_CONTINUE;
    while (true) {
      try {
        // Keep reading until we get the seek map and the track information.
        while (readResult == Extractor.RESULT_CONTINUE
            && (output.seekMap == null || !output.tracksEnded)) {
          readResult = extractor.read(input, positionHolder);
        }
        for (int i = 0; i < output.trackOutputs.size(); i++) {
          int trackId = output.trackOutputs.keyAt(i);
          while (readResult == Extractor.RESULT_CONTINUE
              && output.trackOutputs.get(trackId).lastFormat == null) {
            readResult = extractor.read(input, positionHolder);
          }
        }
      } finally {
        DataSourceUtil.closeQuietly(dataSource);
      }

      if (readResult == Extractor.RESULT_SEEK) {
        input = getExtractorInputFromPosition(dataSource, positionHolder.position, uri);
        readResult = Extractor.RESULT_CONTINUE;
      } else if (readResult == Extractor.RESULT_END_OF_INPUT) {
        throw new IOException("EOF encountered without seekmap");
      }
      if (output.seekMap != null) {
        return output.seekMap;
      }
    }
  }

  /**
   * Extracts all samples from the given file into a {@link FakeTrackOutput}.
   *
   * @param extractor The {@link Extractor} to be used.
   * @param context A {@link Context}.
   * @param fileName The name of the input file.
   * @return The {@link FakeTrackOutput} containing the extracted samples.
   * @throws IOException If an error occurred reading from the input, or if the extractor finishes
   *     reading from input without extracting any {@link SeekMap}.
   */
  public static FakeExtractorOutput extractAllSamplesFromFile(
      Extractor extractor, Context context, String fileName) throws IOException {
    byte[] data = TestUtil.getByteArray(context, fileName);
    return extractAllSamplesFromByteArray(extractor, data);
  }

  /**
   * Extracts all samples from the given file into a {@link FakeTrackOutput}.
   *
   * @param extractor The {@link Extractor} to be used.
   * @param filePath The file path.
   * @return The {@link FakeTrackOutput} containing the extracted samples.
   * @throws IOException If an error occurred reading from the input, or if the extractor finishes
   *     reading from input without extracting any {@link SeekMap}.
   */
  public static FakeExtractorOutput extractAllSamplesFromFilePath(
      Extractor extractor, String filePath) throws IOException {
    byte[] data = getByteArrayFromFilePath(filePath);
    return extractAllSamplesFromByteArray(extractor, data);
  }

  private static FakeExtractorOutput extractAllSamplesFromByteArray(
      Extractor extractor, byte[] data) throws IOException {
    FakeExtractorOutput expectedOutput = new FakeExtractorOutput();
    extractor.init(expectedOutput);
    FakeExtractorInput input = new FakeExtractorInput.Builder().setData(data).build();

    PositionHolder positionHolder = new PositionHolder();
    int readResult = Extractor.RESULT_CONTINUE;
    while (readResult != Extractor.RESULT_END_OF_INPUT) {
      while (readResult == Extractor.RESULT_CONTINUE) {
        readResult = extractor.read(input, positionHolder);
      }
      if (readResult == Extractor.RESULT_SEEK) {
        input.setPosition((int) positionHolder.position);
        readResult = Extractor.RESULT_CONTINUE;
      }
    }
    return expectedOutput;
  }

  /**
   * Seeks to the given seek time of the stream from the given input, and keeps reading from the
   * input until we can extract at least one sample following the seek position, or until
   * end-of-input is reached.
   *
   * @param extractor The {@link Extractor} to extract from input.
   * @param seekMap The {@link SeekMap} of the stream from the given input.
   * @param seekTimeUs The seek time, in micro-seconds.
   * @param trackOutput The {@link FakeTrackOutput} to store the extracted samples.
   * @param dataSource The {@link DataSource} that will be used to read from the input.
   * @param uri The Uri of the input.
   * @return The index of the first extracted sample written to the given {@code trackOutput} after
   *     the seek is completed, or {@link C#INDEX_UNSET} if the seek is completed without any
   *     extracted sample.
   */
  public static int seekToTimeUs(
      Extractor extractor,
      SeekMap seekMap,
      long seekTimeUs,
      DataSource dataSource,
      FakeTrackOutput trackOutput,
      Uri uri)
      throws IOException {
    int numSampleBeforeSeek = trackOutput.getSampleCount();
    SeekMap.SeekPoints seekPoints = seekMap.getSeekPoints(seekTimeUs);

    long initialSeekLoadPosition = seekPoints.first.position;
    extractor.seek(initialSeekLoadPosition, seekTimeUs);

    PositionHolder positionHolder = new PositionHolder();
    positionHolder.position = C.INDEX_UNSET;
    ExtractorInput extractorInput =
        TestUtil.getExtractorInputFromPosition(dataSource, initialSeekLoadPosition, uri);
    int extractorReadResult = Extractor.RESULT_CONTINUE;
    while (true) {
      try {
        // Keep reading until we can read at least one sample after seek
        while (extractorReadResult == Extractor.RESULT_CONTINUE
            && trackOutput.getSampleCount() == numSampleBeforeSeek) {
          extractorReadResult = extractor.read(extractorInput, positionHolder);
        }
      } finally {
        DataSourceUtil.closeQuietly(dataSource);
      }

      if (extractorReadResult == Extractor.RESULT_SEEK) {
        extractorInput =
            TestUtil.getExtractorInputFromPosition(dataSource, positionHolder.position, uri);
        extractorReadResult = Extractor.RESULT_CONTINUE;
      } else if (extractorReadResult == Extractor.RESULT_END_OF_INPUT
          && trackOutput.getSampleCount() == numSampleBeforeSeek) {
        return C.INDEX_UNSET;
      } else if (trackOutput.getSampleCount() > numSampleBeforeSeek) {
        // First index after seek = num sample before seek.
        return numSampleBeforeSeek;
      }
    }
  }

  /** Returns an {@link ExtractorInput} to read from the given input at given position. */
  public static ExtractorInput getExtractorInputFromPosition(
      DataSource dataSource, long position, Uri uri) throws IOException {
    DataSpec dataSpec = new DataSpec(uri, position, C.LENGTH_UNSET);
    long length = dataSource.open(dataSpec);
    if (length != C.LENGTH_UNSET) {
      length += position;
    }
    return new DefaultExtractorInput(dataSource, position, length);
  }

  /**
   * Create a new {@link MetadataInputBuffer} and copy {@code data} into the backing {@link
   * ByteBuffer}.
   */
  public static MetadataInputBuffer createMetadataInputBuffer(byte[] data) {
    MetadataInputBuffer buffer = new MetadataInputBuffer();
    buffer.data = ByteBuffer.allocate(data.length).put(data);
    buffer.data.flip();
    return buffer;
  }

  /** Returns all the public methods of a Java class (except those defined by {@link Object}). */
  public static List<Method> getPublicMethods(Class<?> clazz) {
    // Run a BFS over all extended types to inspect them all.
    Queue<Class<?>> supertypeQueue = new ArrayDeque<>();
    supertypeQueue.add(clazz);
    Set<Class<?>> supertypes = new HashSet<>();
    Object object = new Object();
    while (!supertypeQueue.isEmpty()) {
      Class<?> currentSupertype = supertypeQueue.remove();
      if (supertypes.add(currentSupertype)) {
        @Nullable Class<?> superclass = currentSupertype.getSuperclass();
        if (superclass != null && !superclass.isInstance(object)) {
          supertypeQueue.add(superclass);
        }

        Collections.addAll(supertypeQueue, currentSupertype.getInterfaces());
      }
    }

    List<Method> list = new ArrayList<>();
    for (Class<?> supertype : supertypes) {
      for (Method method : supertype.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          list.add(method);
        }
      }
    }

    return list;
  }

  private static final class NoUidOrShufflingTimeline extends Timeline {

    private final Timeline delegate;

    public NoUidOrShufflingTimeline(Timeline timeline) {
      this.delegate = timeline;
    }

    @Override
    public int getWindowCount() {
      return delegate.getWindowCount();
    }

    @Override
    public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
      return delegate.getNextWindowIndex(windowIndex, repeatMode, /* shuffleModeEnabled= */ false);
    }

    @Override
    public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
      return delegate.getPreviousWindowIndex(
          windowIndex, repeatMode, /* shuffleModeEnabled= */ false);
    }

    @Override
    public int getLastWindowIndex(boolean shuffleModeEnabled) {
      return delegate.getLastWindowIndex(/* shuffleModeEnabled= */ false);
    }

    @Override
    public int getFirstWindowIndex(boolean shuffleModeEnabled) {
      return delegate.getFirstWindowIndex(/* shuffleModeEnabled= */ false);
    }

    @Override
    public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
      delegate.getWindow(windowIndex, window, defaultPositionProjectionUs);
      window.uid = 0;
      return window;
    }

    @Override
    public int getPeriodCount() {
      return delegate.getPeriodCount();
    }

    @Override
    public Period getPeriod(int periodIndex, Period period, boolean setIds) {
      delegate.getPeriod(periodIndex, period, setIds);
      period.uid = 0;
      return period;
    }

    @Override
    public int getIndexOfPeriod(Object uid) {
      return delegate.getIndexOfPeriod(uid);
    }

    @Override
    public Object getUidOfPeriod(int periodIndex) {
      return 0;
    }
  }
}
