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

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link AnnexBToAvccConverter#DEFAULT}. */
@RunWith(AndroidJUnit4.class)
public final class DefaultAnnexBToAvccConverterTest {
  @Test
  public void convertAnnexBToAvcc_singleNalUnit() {
    ByteBuffer in = generateFakeNalUnitData(1000);
    // Add start code for the NAL unit.
    in.put(0, (byte) 0);
    in.put(1, (byte) 0);
    in.put(2, (byte) 0);
    in.put(3, (byte) 1);

    AnnexBToAvccConverter annexBToAvccConverter = AnnexBToAvccConverter.DEFAULT;
    annexBToAvccConverter.process(in);

    // The start code should get replaced with the length of the NAL unit.
    assertThat(in.getInt(0)).isEqualTo(996);
  }

  @Test
  public void convertAnnexBToAvcc_twoNalUnits() {
    ByteBuffer in = generateFakeNalUnitData(1000);
    // Add start code for the first NAL unit.
    in.put(0, (byte) 0);
    in.put(1, (byte) 0);
    in.put(2, (byte) 0);
    in.put(3, (byte) 1);

    // Add start code for the second NAL unit.
    in.put(600, (byte) 0);
    in.put(601, (byte) 0);
    in.put(602, (byte) 0);
    in.put(603, (byte) 1);

    AnnexBToAvccConverter annexBToAvccConverter = AnnexBToAvccConverter.DEFAULT;
    annexBToAvccConverter.process(in);

    // Both the NAL units should have length headers.
    assertThat(in.getInt(0)).isEqualTo(596);
    assertThat(in.getInt(600)).isEqualTo(396);
  }

  @Test
  public void convertAnnexBToAvcc_noNalUnit_outputSameAsInput() {
    ByteBuffer input = generateFakeNalUnitData(1000);
    ByteBuffer inputCopy = ByteBuffer.allocate(input.limit());
    inputCopy.put(input);
    input.rewind();
    inputCopy.rewind();

    AnnexBToAvccConverter annexBToAvccConverter = AnnexBToAvccConverter.DEFAULT;
    annexBToAvccConverter.process(input);

    assertThat(input).isEqualTo(inputCopy);
  }

  /** Returns {@link ByteBuffer} filled with random NAL unit data without start code. */
  private static ByteBuffer generateFakeNalUnitData(int length) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(length);
    for (int i = 0; i < length; i++) {
      // Avoid anything resembling start codes (0x00000001) or emulation prevention byte (0x03).
      buffer.put((byte) ((i % 250) + 5));
    }

    buffer.rewind();
    return buffer;
  }
}
