/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.mynewmedia4.test.session.common.TestUtils.TIMEOUT_MS;
import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.MediaItem;
import androidx.mynewmedia4.common.MediaMetadata;
import androidx.mynewmedia4.common.Player;
import androidx.mynewmedia4.session.MediaLibraryService.LibraryParams;
import androidx.mynewmedia4.session.MediaLibraryService.MediaLibrarySession;
import androidx.mynewmedia4.session.MediaSession.ControllerInfo;
import androidx.mynewmedia4.test.session.common.HandlerThreadTestRule;
import androidx.mynewmedia4.test.session.common.MainLooperTestRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link MediaLibrarySession.Callback}.
 *
 * <p>TODO: Make this class extend MediaSessionCallbackTest. TODO: Create MediaLibrarySessionTest
 * which extends MediaSessionTest.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class MediaLibrarySessionCallbackTest {

  @ClassRule public static MainLooperTestRule mainLooperTestRule = new MainLooperTestRule();

  @Rule
  public final HandlerThreadTestRule threadTestRule =
      new HandlerThreadTestRule("MediaLibrarySessionCallbackTest");

  @Rule public final RemoteControllerTestRule controllerTestRule = new RemoteControllerTestRule();

  @Rule public final MediaSessionTestRule sessionTestRule = new MediaSessionTestRule();

  private Context context;
  private MockPlayer player;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    player =
        new MockPlayer.Builder()
            .setApplicationLooper(threadTestRule.getHandler().getLooper())
            .build();
  }

  @Test
  public void onSubscribe() throws Exception {
    String testParentId = "testSubscribeId";
    LibraryParams testParams = MediaTestUtils.createLibraryParams();

    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback sessionCallback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<LibraryResult<Void>> onSubscribe(
              MediaLibrarySession session,
              ControllerInfo browser,
              String parentId,
              @Nullable LibraryParams params) {
            assertThat(parentId).isEqualTo(testParentId);
            MediaTestUtils.assertLibraryParamsEquals(testParams, params);
            latch.countDown();
            return Futures.immediateFuture(LibraryResult.ofVoid(params));
          }
        };

    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);

    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, sessionCallback)
                .setId("testOnSubscribe")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());
    browser.subscribe(testParentId, testParams);
    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
  }

  @Test
  public void onUnsubscribe() throws Exception {
    String testParentId = "testUnsubscribeId";

    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback sessionCallback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<LibraryResult<Void>> onUnsubscribe(
              MediaLibrarySession session, ControllerInfo browser, String parentId) {
            assertThat(parentId).isEqualTo(testParentId);
            latch.countDown();
            return Futures.immediateFuture(LibraryResult.ofVoid());
          }
        };

    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);

    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, sessionCallback)
                .setId("testOnUnsubscribe")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());
    browser.unsubscribe(testParentId);
    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
  }

  @Test
  public void onGetLibraryRoot_callForRecentRootNonSystemUiPackageName_notIntercepted()
      throws Exception {
    MediaItem mediaItem =
        new MediaItem.Builder()
            .setMediaId("rootMediaId")
            .setMediaMetadata(
                new MediaMetadata.Builder().setIsPlayable(false).setIsBrowsable(true).build())
            .build();
    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);
    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback callback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
              MediaLibrarySession session, ControllerInfo browser, @Nullable LibraryParams params) {
            if (params != null && params.isRecent) {
              latch.countDown();
            }
            return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, params));
          }
        };
    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, callback)
                .setId("onGetChildren_callForRecentRootNonSystemUiPackageName_notIntercepted")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());

    LibraryResult<MediaItem> libraryRoot =
        browser.getLibraryRoot(new LibraryParams.Builder().setRecent(true).build());

    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(libraryRoot.value).isEqualTo(mediaItem);
  }

  @Test
  public void onGetChildren_systemUiCallForRecentItemsWhenIdle_callsOnPlaybackResumption()
      throws Exception {
    ArrayList<MediaItem> mediaItems = MediaTestUtils.createMediaItems(/* size= */ 3);
    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);
    CountDownLatch latch = new CountDownLatch(2);
    MediaLibrarySession.Callback callback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
              MediaSession mediaSession, ControllerInfo controller) {
            latch.countDown();
            return Futures.immediateFuture(
                new MediaSession.MediaItemsWithStartPosition(
                    mediaItems, /* startIndex= */ 1, /* startPositionMs= */ 1000L));
          }

          @Override
          public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
              MediaLibrarySession session,
              ControllerInfo browser,
              String parentId,
              int page,
              int pageSize,
              @Nullable LibraryParams params) {
            latch.countDown();
            return Futures.immediateFuture(
                LibraryResult.ofItemList(mediaItems, /* params= */ null));
          }
        };
    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, callback)
                .setId("onGetChildren_systemUiCallForRecentItems_returnsRecentItems")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());

    LibraryResult<ImmutableList<MediaItem>> recentItem =
        browser.getChildren(
            "androidx.mynewmedia4.session.recent.root",
            /* page= */ 0,
            /* pageSize= */ 100,
            /* params= */ null);
    // Load children of a non recent root that must not be intercepted.
    LibraryResult<ImmutableList<MediaItem>> children =
        browser.getChildren("children", /* page= */ 0, /* pageSize= */ 100, /* params= */ null);

    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(recentItem.resultCode).isEqualTo(LibraryResult.RESULT_SUCCESS);
    assertThat(Lists.transform(recentItem.value, (item) -> item.mediaId))
        .containsExactly("mediaItem_2");
    assertThat(children.value).isEqualTo(mediaItems);
  }

  @Test
  public void
      onGetChildren_systemUiCallForRecentItemsWhenIdleWithEmptyResumptionPlaylist_resultInvalidState()
          throws Exception {
    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);
    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback callback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
              MediaSession mediaSession, ControllerInfo controller) {
            latch.countDown();
            return Futures.immediateFuture(
                new MediaSession.MediaItemsWithStartPosition(
                    ImmutableList.of(), /* startIndex= */ 11, /* startPositionMs= */ 1000L));
          }
        };
    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, callback)
                .setId("onGetChildren_systemUiCallForRecentItems_returnsRecentItems")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());

    LibraryResult<ImmutableList<MediaItem>> recentItem =
        browser.getChildren(
            "androidx.mynewmedia4.session.recent.root",
            /* page= */ 0,
            /* pageSize= */ 100,
            /* params= */ null);

    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(recentItem.resultCode).isEqualTo(LibraryResult.RESULT_ERROR_INVALID_STATE);
  }

  @Test
  public void
      onGetChildren_systemUiCallForRecentItemsWhenIdleStartIndexTooHigh_setToLastItemItemInList()
          throws Exception {
    ArrayList<MediaItem> mediaItems = MediaTestUtils.createMediaItems(/* size= */ 3);
    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);
    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback callback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
              MediaSession mediaSession, ControllerInfo controller) {
            latch.countDown();
            return Futures.immediateFuture(
                new MediaSession.MediaItemsWithStartPosition(
                    mediaItems, /* startIndex= */ 11, /* startPositionMs= */ 1000L));
          }
        };
    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, callback)
                .setId("onGetChildren_systemUiCallForRecentItems_returnsRecentItems")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());

    LibraryResult<ImmutableList<MediaItem>> recentItem =
        browser.getChildren(
            "androidx.mynewmedia4.session.recent.root",
            /* page= */ 0,
            /* pageSize= */ 100,
            /* params= */ null);

    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(recentItem.resultCode).isEqualTo(LibraryResult.RESULT_SUCCESS);
    assertThat(Lists.transform(recentItem.value, (item) -> item.mediaId))
        .containsExactly("mediaItem_3");
  }

  @Test
  public void onGetChildren_systemUiCallForRecentItemsWhenIdleStartIndexNegative_setToZero()
      throws Exception {
    ArrayList<MediaItem> mediaItems = MediaTestUtils.createMediaItems(/* size= */ 3);
    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);
    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback callback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(
              MediaSession mediaSession, ControllerInfo controller) {
            latch.countDown();
            return Futures.immediateFuture(
                new MediaSession.MediaItemsWithStartPosition(
                    mediaItems, /* startIndex= */ -11, /* startPositionMs= */ 1000L));
          }
        };
    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, callback)
                .setId("onGetChildren_systemUiCallForRecentItems_returnsRecentItems")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());

    LibraryResult<ImmutableList<MediaItem>> recentItem =
        browser.getChildren(
            "androidx.mynewmedia4.session.recent.root",
            /* page= */ 0,
            /* pageSize= */ 100,
            /* params= */ null);

    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(recentItem.resultCode).isEqualTo(LibraryResult.RESULT_SUCCESS);
    assertThat(Lists.transform(recentItem.value, (item) -> item.mediaId))
        .containsExactly("mediaItem_1");
  }

  @Test
  public void onGetChildren_systemUiCallForRecentItemsWhenNotIdle_returnsRecentItems()
      throws Exception {
    ArrayList<MediaItem> mediaItems = MediaTestUtils.createMediaItems(/* size= */ 3);
    MockMediaLibraryService service = new MockMediaLibraryService();
    service.attachBaseContext(context);
    CountDownLatch latch = new CountDownLatch(1);
    MediaLibrarySession.Callback callback =
        new MediaLibrarySession.Callback() {
          @Override
          public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
              MediaLibrarySession session,
              ControllerInfo browser,
              String parentId,
              int page,
              int pageSize,
              @Nullable LibraryParams params) {
            latch.countDown();
            return Futures.immediateFuture(
                LibraryResult.ofItemList(mediaItems, /* params= */ null));
          }
        };
    player.playbackState = Player.STATE_READY;
    MediaLibrarySession session =
        sessionTestRule.ensureReleaseAfterTest(
            new MediaLibrarySession.Builder(service, player, callback)
                .setId("onGetChildren_systemUiCallForRecentItems_returnsRecentItems")
                .build());
    RemoteMediaBrowser browser = controllerTestRule.createRemoteBrowser(session.getToken());

    LibraryResult<ImmutableList<MediaItem>> recentItem =
        browser.getChildren(
            "androidx.mynewmedia4.session.recent.root",
            /* page= */ 0,
            /* pageSize= */ 100,
            /* params= */ null);
    // Load children of a non recent root that must not be intercepted.
    LibraryResult<ImmutableList<MediaItem>> children =
        browser.getChildren("children", /* page= */ 0, /* pageSize= */ 100, /* params= */ null);

    assertThat(latch.await(TIMEOUT_MS, MILLISECONDS)).isTrue();
    assertThat(recentItem.resultCode).isEqualTo(LibraryResult.RESULT_SUCCESS);
    assertThat(Lists.transform(recentItem.value, (item) -> item.mediaId))
        .containsExactly("androidx.mynewmedia4.session.recent.item");
    assertThat(children.value).isEqualTo(mediaItems);
  }
}
