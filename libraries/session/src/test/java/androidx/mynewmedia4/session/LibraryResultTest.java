/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.mynewmedia4.session;

import static androidx.mynewmedia4.session.LibraryResult.RESULT_ERROR_NOT_SUPPORTED;
import static androidx.mynewmedia4.session.LibraryResult.UNKNOWN_TYPE_CREATOR;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.os.Bundle;
import androidx.mynewmedia4.common.MediaItem;
import androidx.mynewmedia4.common.MediaMetadata;
import androidx.mynewmedia4.session.MediaLibraryService.LibraryParams;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link LibraryResult}. */
@RunWith(AndroidJUnit4.class)
public class LibraryResultTest {

  @Test
  public void constructor_mediaItemWithoutMediaId_throwsIAE() {
    MediaMetadata metadata =
        new MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(true).build();
    MediaItem item = new MediaItem.Builder().setMediaMetadata(metadata).build();
    assertThrows(
        IllegalArgumentException.class, () -> LibraryResult.ofItem(item, /* params= */ null));
  }

  @Test
  public void constructor_mediaItemWithoutIsBrowsable_throwsIAE() {
    MediaMetadata metadata = new MediaMetadata.Builder().setIsPlayable(true).build();
    MediaItem item = new MediaItem.Builder().setMediaId("id").setMediaMetadata(metadata).build();
    assertThrows(
        IllegalArgumentException.class, () -> LibraryResult.ofItem(item, /* params= */ null));
  }

  @Test
  public void constructor_mediaItemWithoutIsPlayable_throwsIAE() {
    MediaMetadata metadata = new MediaMetadata.Builder().setIsBrowsable(true).build();
    MediaItem item = new MediaItem.Builder().setMediaId("id").setMediaMetadata(metadata).build();
    assertThrows(
        IllegalArgumentException.class, () -> LibraryResult.ofItem(item, /* params= */ null));
  }

  @Test
  public void toBundle_mediaItemLibraryResultThatWasUnbundledAsAnUnknownType_noException() {
    MediaItem mediaItem =
        new MediaItem.Builder()
            .setMediaId("rootMediaId")
            .setMediaMetadata(
                new MediaMetadata.Builder().setIsPlayable(false).setIsBrowsable(true).build())
            .build();
    LibraryParams params = new LibraryParams.Builder().build();
    LibraryResult<MediaItem> libraryResult = LibraryResult.ofItem(mediaItem, params);
    Bundle libraryResultBundle = libraryResult.toBundle();
    LibraryResult<?> libraryResultFromUntyped =
        UNKNOWN_TYPE_CREATOR.fromBundle(libraryResultBundle);

    Bundle bundleOfUntyped = libraryResultFromUntyped.toBundle();

    assertThat(UNKNOWN_TYPE_CREATOR.fromBundle(bundleOfUntyped).value).isEqualTo(mediaItem);
  }

  @Test
  public void toBundle_mediaItemListLibraryResultThatWasUnbundledAsAnUnknownType_noException() {
    MediaItem mediaItem =
        new MediaItem.Builder()
            .setMediaId("rootMediaId")
            .setMediaMetadata(
                new MediaMetadata.Builder().setIsPlayable(false).setIsBrowsable(true).build())
            .build();
    LibraryParams params = new LibraryParams.Builder().build();
    LibraryResult<ImmutableList<MediaItem>> libraryResult =
        LibraryResult.ofItemList(ImmutableList.of(mediaItem), params);
    Bundle libraryResultBundle = libraryResult.toBundle();
    LibraryResult<?> mediaItemLibraryResultFromUntyped =
        UNKNOWN_TYPE_CREATOR.fromBundle(libraryResultBundle);

    Bundle bundleOfUntyped = mediaItemLibraryResultFromUntyped.toBundle();

    assertThat(UNKNOWN_TYPE_CREATOR.fromBundle(bundleOfUntyped).value)
        .isEqualTo(ImmutableList.of(mediaItem));
  }

  @Test
  public void toBundle_errorResultThatWasUnbundledAsAnUnknownType_noException() {
    LibraryResult<ImmutableList<Error>> libraryResult =
        LibraryResult.ofError(LibraryResult.RESULT_ERROR_NOT_SUPPORTED);
    Bundle errorLibraryResultBundle = libraryResult.toBundle();
    LibraryResult<?> libraryResultFromUntyped =
        UNKNOWN_TYPE_CREATOR.fromBundle(errorLibraryResultBundle);

    Bundle bundleOfUntyped = libraryResultFromUntyped.toBundle();

    assertThat(UNKNOWN_TYPE_CREATOR.fromBundle(bundleOfUntyped).value).isNull();
    assertThat(UNKNOWN_TYPE_CREATOR.fromBundle(bundleOfUntyped).resultCode)
        .isEqualTo(RESULT_ERROR_NOT_SUPPORTED);
  }

  @Test
  public void toBundle_voidResultThatWasUnbundledAsAnUnknownType_noException() {
    LibraryResult<ImmutableList<Error>> libraryResult =
        LibraryResult.ofError(LibraryResult.RESULT_ERROR_NOT_SUPPORTED);
    Bundle errorLibraryResultBundle = libraryResult.toBundle();
    LibraryResult<?> libraryResultFromUntyped =
        UNKNOWN_TYPE_CREATOR.fromBundle(errorLibraryResultBundle);

    Bundle bundleOfUntyped = libraryResultFromUntyped.toBundle();

    assertThat(UNKNOWN_TYPE_CREATOR.fromBundle(bundleOfUntyped).value).isNull();
    assertThat(UNKNOWN_TYPE_CREATOR.fromBundle(bundleOfUntyped).resultCode)
        .isEqualTo(RESULT_ERROR_NOT_SUPPORTED);
  }
}
