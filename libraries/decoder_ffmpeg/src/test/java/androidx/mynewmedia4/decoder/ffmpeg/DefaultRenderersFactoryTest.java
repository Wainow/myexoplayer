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
package androidx.mynewmedia4.decoder.ffmpeg;

import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.test.utils.DefaultRenderersFactoryAsserts;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for {@link DefaultRenderersFactoryTest} with {@link FfmpegAudioRenderer}.
 */
@RunWith(AndroidJUnit4.class)
public final class DefaultRenderersFactoryTest {

  @Test
  public void createRenderers_instantiatesFfmpegAudioRenderer() {
    DefaultRenderersFactoryAsserts.assertExtensionRendererCreated(
        FfmpegAudioRenderer.class, C.TRACK_TYPE_AUDIO);
  }
}