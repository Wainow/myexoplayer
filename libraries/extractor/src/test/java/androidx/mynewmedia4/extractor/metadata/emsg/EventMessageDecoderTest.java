/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.mynewmedia4.extractor.metadata.emsg;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import androidx.mynewmedia4.common.Metadata;
import androidx.mynewmedia4.common.util.Assertions;
import androidx.mynewmedia4.extractor.metadata.MetadataInputBuffer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.primitives.Bytes;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test for {@link EventMessageDecoder}. */
@RunWith(AndroidJUnit4.class)
public final class EventMessageDecoderTest {

  @Test
  public void decodeEventMessage() {
    byte[] rawEmsgBody =
        Bytes.concat(
            createByteArray(117, 114, 110, 58, 116, 101, 115, 116, 0), // scheme_id_uri = "urn:test"
            createByteArray(49, 50, 51, 0), // value = "123"
            createByteArray(0, 0, 0, 0, 0, 0, 11, 184), // event_duration_ms = 3000
            createByteArray(0, 0, 0, 0, 0, 15, 67, 211), // id = 1000403
            createByteArray(0, 1, 2, 3, 4)); // message_data = {0, 1, 2, 3, 4}
    EventMessageDecoder decoder = new EventMessageDecoder();

    Metadata metadata = decoder.decode(createMetadataInputBuffer(rawEmsgBody));

    assertThat(metadata.length()).isEqualTo(1);
    EventMessage eventMessage = (EventMessage) metadata.get(0);
    assertThat(eventMessage.schemeIdUri).isEqualTo("urn:test");
    assertThat(eventMessage.value).isEqualTo("123");
    assertThat(eventMessage.durationMs).isEqualTo(3000);
    assertThat(eventMessage.id).isEqualTo(1000403);
    assertThat(eventMessage.messageData).isEqualTo(new byte[] {0, 1, 2, 3, 4});
  }

  @Test
  public void decodeEventMessage_failsIfPositionNonZero() {
    EventMessageDecoder decoder = new EventMessageDecoder();
    MetadataInputBuffer buffer = createMetadataInputBuffer(createByteArray(1, 2, 3));
    buffer.data.position(1);

    assertThrows(IllegalArgumentException.class, () -> decoder.decode(buffer));
  }

  @Test
  public void decodeEventMessage_failsIfBufferHasNoArray() {
    EventMessageDecoder decoder = new EventMessageDecoder();
    MetadataInputBuffer buffer = createMetadataInputBuffer(createByteArray(1, 2, 3));
    buffer.data = buffer.data.asReadOnlyBuffer();

    assertThrows(IllegalArgumentException.class, () -> decoder.decode(buffer));
  }

  @Test
  public void decodeEventMessage_failsIfArrayOffsetNonZero() {
    EventMessageDecoder decoder = new EventMessageDecoder();
    MetadataInputBuffer buffer = createMetadataInputBuffer(createByteArray(1, 2, 3));
    buffer.data.position(1);
    buffer.data = buffer.data.slice();

    assertThrows(IllegalArgumentException.class, () -> decoder.decode(buffer));
  }

  /** Converts an array of integers in the range [0, 255] into an equivalent byte array. */
  // TODO(internal b/161804035): Use TestUtils when it's available in a dependency we can use here.
  private static byte[] createByteArray(int... bytes) {
    byte[] byteArray = new byte[bytes.length];
    for (int i = 0; i < byteArray.length; i++) {
      Assertions.checkState(0x00 <= bytes[i] && bytes[i] <= 0xFF);
      byteArray[i] = (byte) bytes[i];
    }
    return byteArray;
  }

  /**
   * Create a new {@link MetadataInputBuffer} and copy {@code data} into the backing {@link
   * ByteBuffer}.
   */
  // TODO(internal b/161804035): Use TestUtils when it's available in a dependency we can use here.
  private static MetadataInputBuffer createMetadataInputBuffer(byte[] data) {
    MetadataInputBuffer buffer = new MetadataInputBuffer();
    buffer.data = ByteBuffer.allocate(data.length).put(data);
    buffer.data.flip();
    return buffer;
  }
}
