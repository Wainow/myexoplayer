/*
 * Copyright (C) 2019 The Android Open Source Project
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
package androidx.mynewmedia4.extractor.metadata.vorbis;

import android.os.Parcel;
import androidx.mynewmedia4.common.util.UnstableApi;

/** A vorbis comment, extracted from a FLAC or Ogg file. */
@SuppressWarnings("deprecation") // Extending deprecated type for backwards compatibility.
@UnstableApi
public final class VorbisComment extends androidx.mynewmedia4.extractor.metadata.flac.VorbisComment {

  /**
   * @param key The key.
   * @param value The value.
   */
  public VorbisComment(String key, String value) {
    super(key, value);
  }

  /* package */ VorbisComment(Parcel in) {
    super(in);
  }

  public static final Creator<VorbisComment> CREATOR =
      new Creator<VorbisComment>() {

        @Override
        public VorbisComment createFromParcel(Parcel in) {
          return new VorbisComment(in);
        }

        @Override
        public VorbisComment[] newArray(int size) {
          return new VorbisComment[size];
        }
      };
}
