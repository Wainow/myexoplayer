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
package androidx.mynewmedia4.demo.cast;

import androidx.multidex.MultiDexApplication;

// Note: Multidex is enabled in code not AndroidManifest.xml because the internal build system
// doesn't dejetify MultiDexApplication in AndroidManifest.xml.
/** Application for multidex support. */
public final class DemoApplication extends MultiDexApplication {}
