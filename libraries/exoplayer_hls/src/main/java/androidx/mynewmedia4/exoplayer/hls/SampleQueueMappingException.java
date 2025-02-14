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
package androidx.mynewmedia4.exoplayer.hls;

import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.TrackGroup;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.exoplayer.source.SampleQueue;
import java.io.IOException;

/** Thrown when it is not possible to map a {@link TrackGroup} to a {@link SampleQueue}. */
@UnstableApi
public final class SampleQueueMappingException extends IOException {

  /**
   * @param mimeType The MIME type of the track group whose mapping failed.
   */
  public SampleQueueMappingException(@Nullable String mimeType) {
    super("Unable to bind a sample queue to TrackGroup with MIME type " + mimeType + ".");
  }
}
