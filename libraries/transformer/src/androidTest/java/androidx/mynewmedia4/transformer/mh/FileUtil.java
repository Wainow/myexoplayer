/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.mynewmedia4.transformer.mh;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.ColorInfo;
import androidx.mynewmedia4.common.Format;
import androidx.mynewmedia4.common.MediaItem;
import androidx.mynewmedia4.common.TrackGroup;
import androidx.mynewmedia4.exoplayer.MetadataRetriever;
import androidx.mynewmedia4.exoplayer.source.TrackGroupArray;
import java.util.concurrent.ExecutionException;

/** Utilities for accessing details of media files. */
/* package */ class FileUtil {

  /**
   * Asserts that the file has a certain color transfer.
   *
   * @param context The current context.
   * @param filePath The path of the input file.
   * @param expectedColorTransfer The expected {@link C.ColorTransfer} for the input file.
   */
  public static void assertFileHasColorTransfer(
      Context context, @Nullable String filePath, @C.ColorTransfer int expectedColorTransfer) {
    TrackGroupArray trackGroupArray;
    try {
      trackGroupArray =
          MetadataRetriever.retrieveMetadata(context, MediaItem.fromUri("file://" + filePath))
              .get();
    } catch (ExecutionException | InterruptedException e) {
      throw new IllegalStateException(e);
    }

    int trackGroupCount = trackGroupArray.length;
    assertThat(trackGroupCount).isEqualTo(2);
    for (int i = 0; i < trackGroupCount; i++) {
      TrackGroup trackGroup = trackGroupArray.get(i);
      if (trackGroup.type == C.TRACK_TYPE_VIDEO) {
        assertThat(trackGroup.length).isEqualTo(1);
        @Nullable ColorInfo colorInfo = trackGroup.getFormat(0).colorInfo;
        @C.ColorTransfer
        int actualColorTransfer =
            colorInfo == null || colorInfo.colorTransfer == Format.NO_VALUE
                ? C.COLOR_TRANSFER_SDR
                : colorInfo.colorTransfer;
        assertThat(actualColorTransfer).isEqualTo(expectedColorTransfer);
        return;
      }
    }
    throw new IllegalStateException("Couldn't find video track");
  }

  private FileUtil() {}
}
