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
package androidx.mynewmedia4.exoplayer.dash.offline;

import static androidx.mynewmedia4.exoplayer.dash.offline.DashDownloadTestData.TEST_ID;
import static androidx.mynewmedia4.exoplayer.dash.offline.DashDownloadTestData.TEST_MPD;
import static androidx.mynewmedia4.exoplayer.dash.offline.DashDownloadTestData.TEST_MPD_URI;
import static androidx.mynewmedia4.test.utils.CacheAsserts.assertCacheEmpty;
import static androidx.mynewmedia4.test.utils.CacheAsserts.assertCachedData;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.MimeTypes;
import androidx.mynewmedia4.common.StreamKey;
import androidx.mynewmedia4.common.util.ConditionVariable;
import androidx.mynewmedia4.common.util.Util;
import androidx.mynewmedia4.datasource.DataSource;
import androidx.mynewmedia4.datasource.cache.CacheDataSource;
import androidx.mynewmedia4.datasource.cache.NoOpCacheEvictor;
import androidx.mynewmedia4.datasource.cache.SimpleCache;
import androidx.mynewmedia4.exoplayer.offline.DefaultDownloadIndex;
import androidx.mynewmedia4.exoplayer.offline.DefaultDownloaderFactory;
import androidx.mynewmedia4.exoplayer.offline.Download;
import androidx.mynewmedia4.exoplayer.offline.DownloadManager;
import androidx.mynewmedia4.exoplayer.offline.DownloadRequest;
import androidx.mynewmedia4.exoplayer.offline.DownloadService;
import androidx.mynewmedia4.exoplayer.scheduler.Requirements;
import androidx.mynewmedia4.exoplayer.scheduler.Scheduler;
import androidx.mynewmedia4.test.utils.DummyMainThread;
import androidx.mynewmedia4.test.utils.FakeDataSet;
import androidx.mynewmedia4.test.utils.FakeDataSource;
import androidx.mynewmedia4.test.utils.TestUtil;
import androidx.mynewmedia4.test.utils.robolectric.TestDownloadManagerListener;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link DownloadService}. */
@RunWith(AndroidJUnit4.class)
public class DownloadServiceDashTest {

  private SimpleCache cache;
  private File tempFolder;
  private FakeDataSet fakeDataSet;
  private StreamKey fakeStreamKey1;
  private StreamKey fakeStreamKey2;
  private Context context;
  private DownloadService dashDownloadService;
  private ConditionVariable pauseDownloadCondition;
  private TestDownloadManagerListener downloadManagerListener;
  private DummyMainThread testThread;

  @Before
  public void setUp() throws IOException {
    testThread = new DummyMainThread();
    context = ApplicationProvider.getApplicationContext();
    tempFolder = Util.createTempDirectory(context, "ExoPlayerTest");
    cache =
        new SimpleCache(tempFolder, new NoOpCacheEvictor(), TestUtil.getInMemoryDatabaseProvider());

    Runnable pauseAction =
        () -> {
          if (pauseDownloadCondition != null) {
            try {
              pauseDownloadCondition.block();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
        };
    fakeDataSet =
        new FakeDataSet()
            .setData(TEST_MPD_URI, TEST_MPD)
            .newData("audio_init_data")
            .appendReadAction(pauseAction)
            .appendReadData(TestUtil.buildTestData(10))
            .endData()
            .setRandomData("audio_segment_1", 4)
            .setRandomData("audio_segment_2", 5)
            .setRandomData("audio_segment_3", 6)
            .setRandomData("text_segment_1", 1)
            .setRandomData("text_segment_2", 2)
            .setRandomData("text_segment_3", 3);
    final DataSource.Factory fakeDataSourceFactory =
        new FakeDataSource.Factory().setFakeDataSet(fakeDataSet);
    fakeStreamKey1 = new StreamKey(0, 0, 0);
    fakeStreamKey2 = new StreamKey(0, 1, 0);

    testThread.runTestOnMainThread(
        () -> {
          DefaultDownloadIndex downloadIndex =
              new DefaultDownloadIndex(TestUtil.getInMemoryDatabaseProvider());
          DefaultDownloaderFactory downloaderFactory =
              new DefaultDownloaderFactory(
                  new CacheDataSource.Factory()
                      .setCache(cache)
                      .setUpstreamDataSourceFactory(fakeDataSourceFactory),
                  /* executor= */ Runnable::run);
          final DownloadManager dashDownloadManager =
              new DownloadManager(
                  ApplicationProvider.getApplicationContext(), downloadIndex, downloaderFactory);
          downloadManagerListener = new TestDownloadManagerListener(dashDownloadManager);
          dashDownloadManager.resumeDownloads();

          dashDownloadService =
              new DownloadService(DownloadService.FOREGROUND_NOTIFICATION_ID_NONE) {
                @Override
                protected DownloadManager getDownloadManager() {
                  return dashDownloadManager;
                }

                @Override
                @Nullable
                protected Scheduler getScheduler() {
                  return null;
                }

                @Override
                protected Notification getForegroundNotification(
                    List<Download> downloads,
                    @Requirements.RequirementFlags int notMetRequirements) {
                  throw new UnsupportedOperationException();
                }
              };
          dashDownloadService.onCreate();
        });
  }

  @After
  public void tearDown() {
    testThread.runOnMainThread(() -> dashDownloadService.onDestroy());
    Util.recursiveDelete(tempFolder);
    testThread.release();
  }

  @Ignore("Internal ref: b/78877092")
  @Test
  public void multipleDownloadRequest() throws Throwable {
    downloadKeys(fakeStreamKey1);
    downloadKeys(fakeStreamKey2);

    downloadManagerListener.blockUntilIdleAndThrowAnyFailure();

    assertCachedData(cache, fakeDataSet);
  }

  @Ignore("Internal ref: b/78877092")
  @Test
  public void removeAction() throws Throwable {
    downloadKeys(fakeStreamKey1, fakeStreamKey2);

    downloadManagerListener.blockUntilIdleAndThrowAnyFailure();

    removeAll();

    downloadManagerListener.blockUntilIdleAndThrowAnyFailure();

    assertCacheEmpty(cache);
  }

  @Ignore("Internal ref: b/78877092")
  @Test
  public void removeBeforeDownloadComplete() throws Throwable {
    pauseDownloadCondition = new ConditionVariable();
    downloadKeys(fakeStreamKey1, fakeStreamKey2);

    removeAll();

    downloadManagerListener.blockUntilIdleAndThrowAnyFailure();

    assertCacheEmpty(cache);
  }

  private void removeAll() {
    testThread.runOnMainThread(
        () -> {
          Intent startIntent =
              DownloadService.buildRemoveDownloadIntent(
                  context, DownloadService.class, TEST_ID, /* foreground= */ false);
          dashDownloadService.onStartCommand(startIntent, 0, 0);
        });
  }

  private void downloadKeys(StreamKey... keys) {
    ArrayList<StreamKey> keysList = new ArrayList<>();
    Collections.addAll(keysList, keys);
    DownloadRequest action =
        new DownloadRequest.Builder(TEST_ID, TEST_MPD_URI)
            .setMimeType(MimeTypes.APPLICATION_MPD)
            .setStreamKeys(keysList)
            .build();

    testThread.runOnMainThread(
        () -> {
          Intent startIntent =
              DownloadService.buildAddDownloadIntent(
                  context, DownloadService.class, action, /* foreground= */ false);
          dashDownloadService.onStartCommand(startIntent, 0, 0);
        });
  }
}
