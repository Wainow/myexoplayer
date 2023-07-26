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
package androidx.mynewmedia4.effect;

import androidx.mynewmedia4.common.GlTextureInfo;
import androidx.mynewmedia4.effect.GlShaderProgram.InputListener;
import androidx.mynewmedia4.effect.GlShaderProgram.OutputListener;

/**
 * Connects a producing and a consuming {@link GlShaderProgram} instance.
 *
 * <p>This listener should be set as {@link InputListener} on the consuming {@link GlShaderProgram}
 * and as {@link OutputListener} on the producing {@link GlShaderProgram}.
 */
/* package */ final class ChainingGlShaderProgramListener
    implements GlShaderProgram.InputListener, GlShaderProgram.OutputListener {

  private final GlShaderProgram producingGlShaderProgram;
  private final FrameConsumptionManager frameConsumptionManager;
  private final VideoFrameProcessingTaskExecutor videoFrameProcessingTaskExecutor;

  /**
   * Creates a new instance.
   *
   * @param producingGlShaderProgram The {@link GlShaderProgram} for which this listener will be set
   *     as {@link OutputListener}.
   * @param consumingGlShaderProgram The {@link GlShaderProgram} for which this listener will be set
   *     as {@link InputListener}.
   * @param videoFrameProcessingTaskExecutor The {@link VideoFrameProcessingTaskExecutor} that is
   *     used for OpenGL calls. All calls to the producing/consuming {@link GlShaderProgram} will be
   *     executed by the {@link VideoFrameProcessingTaskExecutor}. The caller is responsible for
   *     releasing the {@link VideoFrameProcessingTaskExecutor}.
   */
  public ChainingGlShaderProgramListener(
      GlShaderProgram producingGlShaderProgram,
      GlShaderProgram consumingGlShaderProgram,
      VideoFrameProcessingTaskExecutor videoFrameProcessingTaskExecutor) {
    this.producingGlShaderProgram = producingGlShaderProgram;
    frameConsumptionManager =
        new FrameConsumptionManager(consumingGlShaderProgram, videoFrameProcessingTaskExecutor);
    this.videoFrameProcessingTaskExecutor = videoFrameProcessingTaskExecutor;
  }

  @Override
  public synchronized void onReadyToAcceptInputFrame() {
    frameConsumptionManager.onReadyToAcceptInputFrame();
  }

  @Override
  public void onInputFrameProcessed(GlTextureInfo inputTexture) {
    videoFrameProcessingTaskExecutor.submit(
        () -> producingGlShaderProgram.releaseOutputFrame(inputTexture));
  }

  @Override
  public synchronized void onFlush() {
    frameConsumptionManager.onFlush();
    videoFrameProcessingTaskExecutor.submit(producingGlShaderProgram::flush);
  }

  @Override
  public synchronized void onOutputFrameAvailable(
      GlTextureInfo outputTexture, long presentationTimeUs) {
    frameConsumptionManager.queueInputFrame(outputTexture, presentationTimeUs);
  }

  @Override
  public synchronized void onCurrentOutputStreamEnded() {
    frameConsumptionManager.signalEndOfCurrentStream();
  }
}