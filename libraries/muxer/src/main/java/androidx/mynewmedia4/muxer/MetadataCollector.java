/*
 * Copyright 2022 The Android Open Source Project
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

import static androidx.mynewmedia4.common.util.Assertions.checkState;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** Collects and provides metadata: location, FPS, XMP data, etc. */
/* package */ final class MetadataCollector {
  public int orientation;
  public @MonotonicNonNull Mp4Location location;
  public Map<String, Object> metadataPairs;
  public long modificationDateUnixMs;
  public @MonotonicNonNull ByteBuffer xmpData;

  public MetadataCollector() {
    orientation = 0;
    metadataPairs = new LinkedHashMap<>();
    modificationDateUnixMs = System.currentTimeMillis();
  }

  public void addXmp(ByteBuffer xmpData) {
    checkState(this.xmpData == null);
    this.xmpData = xmpData;
  }

  public void setOrientation(int orientation) {
    this.orientation = orientation;
  }

  public void setLocation(float latitude, float longitude) {
    location = new Mp4Location(latitude, longitude);
  }

  public void setCaptureFps(float captureFps) {
    metadataPairs.put("com.android.capture.fps", captureFps);
  }

  public void addMetadata(String key, Object value) {
    metadataPairs.put(key, value);
  }

  public void setModificationTime(long modificationDateUnixMs) {
    this.modificationDateUnixMs = modificationDateUnixMs;
  }
}
