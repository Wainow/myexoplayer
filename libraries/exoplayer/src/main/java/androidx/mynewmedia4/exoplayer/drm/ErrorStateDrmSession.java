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
package androidx.mynewmedia4.exoplayer.drm;

import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.util.Assertions;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.decoder.CryptoConfig;
import java.util.Map;
import java.util.UUID;

/** A {@link DrmSession} that's in a terminal error state. */
@UnstableApi
public final class ErrorStateDrmSession implements DrmSession {

  private final DrmSessionException error;

  public ErrorStateDrmSession(DrmSessionException error) {
    this.error = Assertions.checkNotNull(error);
  }

  @Override
  public int getState() {
    return STATE_ERROR;
  }

  @Override
  public boolean playClearSamplesWithoutKeys() {
    return false;
  }

  @Override
  @Nullable
  public DrmSessionException getError() {
    return error;
  }

  @Override
  public final UUID getSchemeUuid() {
    return C.UUID_NIL;
  }

  @Override
  @Nullable
  public CryptoConfig getCryptoConfig() {
    return null;
  }

  @Override
  @Nullable
  public Map<String, String> queryKeyStatus() {
    return null;
  }

  @Override
  @Nullable
  public byte[] getOfflineLicenseKeySetId() {
    return null;
  }

  @Override
  public boolean requiresSecureDecoder(String mimeType) {
    return false;
  }

  @Override
  public void acquire(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher) {
    // Do nothing.
  }

  @Override
  public void release(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher) {
    // Do nothing.
  }
}
