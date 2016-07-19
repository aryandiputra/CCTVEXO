package com.kmklabs.videoplayer.test;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.action.MotionEvents;
import android.support.test.espresso.action.Swiper;
import android.support.test.espresso.contrib.CountingIdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.R;
import com.kmklabs.videoplayer.KmkExoListener;
import com.kmklabs.videoplayer.KmkExoVideoView;
import com.kmklabs.videoplayer.PlayerFactory;
import java.io.IOException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class KmkExoVideoViewTest {
  private static final String EXISTING_STREAM = "bipbop/bipbopall.m3u8";
  private static final String NON_EXISTING_STREAM = "unknown.m3u8";
  private static final String FOUR_K_STREAM = "skate_phantom_flex_4k_8288_2160p.m3u8";

  @Rule public final ActivityTestRule<MockActivity> rule =
      new ActivityTestRule<>(MockActivity.class, true, true);
  CountingIdlingResource idleResource = new CountingIdlingResource("Loading Video");
  KmkExoListener mockListener = mock(KmkExoListener.class);
  private MockWebServer server = new MockWebServer();

  @Before public void setUp() throws Throwable {
    Espresso.registerIdlingResources(idleResource);

    server.setDispatcher(new Dispatcher() {
      @Override public MockResponse dispatch(RecordedRequest recordedRequest)
          throws InterruptedException {
        if (recordedRequest.getPath().endsWith(NON_EXISTING_STREAM)) {
          return new MockResponse().setResponseCode(404);
        }
        if (recordedRequest.getPath().endsWith(FOUR_K_STREAM)) {
          return new MockResponse().setBody("#EXTM3U\n"
              + "\n"
              + "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=300000,RESOLUTION=480x270,NAME=\"270p 3G\"\n"
              + server.url("/literally-unplayable.mp4.m3u8"));
        }
        if (recordedRequest.getPath().endsWith("unplayable.mp4.m3u8")) {
          return new MockResponse().setBody("#EXTM3U\n"
              + "#EXT-X-PLAYLIST-TYPE:VOD\n"
              + "#EXT-X-TARGETDURATION:10\n"
              + "#EXTINF:10,\n"
              + server.url("unplayable-1.ts")
              + "#EXT-X-ENDLIST\n");
        }
        if (recordedRequest.getPath().endsWith("unplayable-1.ts")) {
          try {
            return new MockResponse().setBody(new Buffer().readFrom(InstrumentationRegistry.getContext().getAssets().open("hls/unplayable.ts")));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        if (recordedRequest.getPath().endsWith(EXISTING_STREAM)) {
          return new MockResponse().setBody("#EXTM3U\n"
              + "\n"
              + "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=300000,RESOLUTION=480x270,NAME=\"270p 3G\"\n"
              + server.url("/ivm_2-2144-b300.mp4.m3u8")
              + "\n"
              + "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=600000,RESOLUTION=640x360,NAME=\"360p SD\"\n"
              + server.url("/ivm_2-2144-b300.mp4.m3u8")
              + "\n"
              + "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1200000,RESOLUTION=1280x720,NAME=\"720p HD\"\n"
              + server.url("/ivm_2-2144-b300.mp4.m3u8"));
        } else if (recordedRequest.getPath().contains("mp4.m3u8")) {
          return new MockResponse().setBody("#EXTM3U\n"
              + "#EXT-X-TARGETDURATION:10\n"
              + "#EXTINF:10,\n"
              + server.url("mp4-1.ts") + "\n"
              + "#EXT-X-DISCONTINUITY"
              + "#EXTINF:10,\n"
              + server.url("mp4-1.ts") + "\n"
              + "#EXT-X-DISCONTINUITY"
              + "#EXTINF:10,\n"
              + server.url("mp4-1.ts") + "\n"
              + "#EXT-X-DISCONTINUITY"
              + "#EXTINF:10,\n"
              + server.url("mp4-1.ts") + "\n"
              + "#EXT-X-DISCONTINUITY"
              + "#EXTINF:10,\n"
              + server.url("mp4-1.ts") + "\n"
              + "#EXT-X-DISCONTINUITY"
              + "#EXTINF:10,\n"
              + server.url("mp4-1.ts") + "\n"
              + "#EXT-X-ENDLIST\n");
        } else if (recordedRequest.getPath().contains("mp4-1.ts")) {
          try {
            return new MockResponse().addHeader("Content-Type", "video/mp2t")
                .addHeader("Accept-Ranges", "bytes")
                .setBody(new Buffer().readFrom(
                    InstrumentationRegistry.getContext().getAssets().open("hls/sample.ts")));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        return new MockResponse();
      }
    });
    server.start();
    rule.runOnUiThread(new Runnable() {
      @Override public void run() {
        rule.getActivity()
            .setContentView(com.google.android.exoplayer.test.R.layout.layout_mock_kmkexo);
      }
    });
  }

  @After public void tearDown() throws Exception {
    Espresso.unregisterIdlingResources(idleResource);
    reset(mockListener);
    server.shutdown();
  }

  @Test public void loadExistingM3u8() throws Throwable {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        loadM3U8AndMarkIdleWhenSuccess(server.url(EXISTING_STREAM).toString()))
        .check(new ViewAssertion() {
          @Override public void check(View view, NoMatchingViewException noViewFoundException) {
            verify(mockListener).onVideoStarted();
            verify(mockListener).onDrawnToSurface(
                ((SurfaceView) view.findViewById(R.id.video_surface)).getHolder().getSurface());
            verify(mockListener, atLeast(1)).onBufferingStarted();
            verify(mockListener, atLeast(1)).onBufferingFinished(anyLong());
          }
        });
  }

  @Test public void loadNonExistingM3u8() throws Throwable {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        loadM3U8AndMarkIdleWhenLoadError(server.url(NON_EXISTING_STREAM).toString()))
        .check(new ViewAssertion() {
          @Override public void check(View view, NoMatchingViewException noViewFoundException) {
            verify(mockListener).onLoadError(any(ExoPlaybackException.class));
          }
        });
  }

  @Test public void load4kM3u8ShouldTriggerError() throws Throwable {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        loadM3U8AndMarkIdleWhenPlaybackError(server.url(FOUR_K_STREAM).toString())).check(new ViewAssertion() {
      @Override public void check(View view, NoMatchingViewException noViewFoundException) {
        verify(mockListener).onPlaybackError(any(ExoPlaybackException.class));
      }
    });
  }

  @Test public void pauseAndResumePlayback() {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        loadM3U8AndMarkIdleWhenSuccess(server.url(EXISTING_STREAM).toString()), click());

    onView(withId(R.id.image_pause)).perform(click());

    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).check(
        new ViewAssertion() {
          @Override public void check(View view, NoMatchingViewException noViewFoundException) {
            KmkExoVideoView kevv = (KmkExoVideoView) view;
            assertFalse("Video should be paused", kevv.isPlaying());
          }
        });

    onView(withId(R.id.image_pause)).perform(click());

    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).check(
        new ViewAssertion() {
          @Override public void check(View view, NoMatchingViewException noViewFoundException) {
            KmkExoVideoView kevv = (KmkExoVideoView) view;
            assertTrue("Video should be playing", kevv.isPlaying());
          }
        });
  }

  @Test public void surfaceCreatedWhenLoadingVideoShouldntLoadVideoMultipleTimes() {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        destroySurfaceView());

    final PlayerFactory factory = mock(PlayerFactory.class);
    onView(withId(com.google.android.exoplayer.test.R.id.container_kmkexo)).perform(
        recreateSurfaceView(factory)).check(new ViewAssertion() {
      @Override public void check(View view, NoMatchingViewException noViewFoundException) {
        verify(factory, times(1)).loadPlayer(any(Context.class), anyString(),
            any(PlayerFactory.LoaderCallback.class));
        verifyNoMoreInteractions(factory);
      }
    });
  }

  @NonNull private ViewAction recreateSurfaceView(final PlayerFactory factory) {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() {
        return isAssignableFrom(View.class);
      }

      @Override public String getDescription() {
        return null;
      }

      @Override public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        KmkExoVideoView videoView = new KmkExoVideoView(rule.getActivity());

        ViewGroup vg = (ViewGroup) view;

        SurfaceView sv = (SurfaceView) videoView.findViewById(R.id.video_surface);

        SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
          @Override public void surfaceCreated(SurfaceHolder holder) {
            idleResource.decrement();
          }

          @Override
          public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

          }

          @Override public void surfaceDestroyed(SurfaceHolder holder) {

          }
        };
        sv.getHolder().addCallback(callback);

        idleResource.increment();
        videoView.setPlayerFactory(factory);
        videoView.loadVideo(server.url(EXISTING_STREAM).toString());
        vg.addView(videoView);

        uiController.loopMainThreadUntilIdle();
        sv.getHolder().removeCallback(callback);
      }
    };
  }

  @NonNull private ViewAction destroySurfaceView() {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() {
        return isAssignableFrom(View.class);
      }

      @Override public String getDescription() {
        return null;
      }

      @Override public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        ViewGroup vg = (ViewGroup) view.getParent();

        SurfaceView sv = (SurfaceView) view.findViewById(R.id.video_surface);
        idleResource.increment();
        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
          @Override public void surfaceCreated(SurfaceHolder holder) {

          }

          @Override
          public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

          }

          @Override public void surfaceDestroyed(SurfaceHolder holder) {
            idleResource.decrement();
          }
        });

        vg.removeView(view);

        uiController.loopMainThreadUntilIdle();
      }
    };
  }

  @Test public void seekPlayback() throws InterruptedException {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        loadM3U8AndMarkIdleWhenSuccess(server.url(EXISTING_STREAM).toString()), click());

    onView(withId(R.id.seekbar)).perform(swipeSeekBarAndWait()).check(new ViewAssertion() {
      @Override public void check(View view, NoMatchingViewException noViewFoundException) {
        if (noViewFoundException != null) throw noViewFoundException;

        verify(mockListener).onSeek(anyInt(), anyInt());
      }
    });
  }

  @Test public void changeBitrate() throws Throwable {
    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).perform(
        loadM3U8AndMarkIdleWhenSuccess(server.url(EXISTING_STREAM).toString()), click());

    onView(withId(R.id.change_video_track)).perform(click());
    onView(withText("360p SD")).perform(click());

    onView(withId(com.google.android.exoplayer.test.R.id.kmk_exo_video_view)).check(
        new ViewAssertion() {
          @Override public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) throw noViewFoundException;
            KmkExoVideoView kv = (KmkExoVideoView) view;

            assertThat(kv.getSelectedVideoTrack(), equalTo(2));
          }
        });
  }

  private Swiper.Status sendLinearSwipe(UiController uiController, float[] startCoordinates,
      float[] endCoordinates, float[] precision, int duration) {
    float[][] steps = interpolate(startCoordinates, endCoordinates, 10);
    final int delayBetweenMovements = duration / steps.length;
    MotionEvent downEvent = MotionEvents.sendDown(uiController, steps[0], precision).down;
    try {
      for (int i = 1; i < steps.length; i++) {
        if (!MotionEvents.sendMovement(uiController, downEvent, steps[i])) {
          MotionEvents.sendCancel(uiController, downEvent);
          return Swiper.Status.FAILURE;
        }
        long desiredTime = downEvent.getDownTime() + delayBetweenMovements * i;
        long timeUntilDesired = desiredTime - SystemClock.uptimeMillis();
        if (timeUntilDesired > 10) {
          uiController.loopMainThreadForAtLeast(timeUntilDesired);
        }
      }
      if (!MotionEvents.sendUp(uiController, downEvent, endCoordinates)) {
        MotionEvents.sendCancel(uiController, downEvent);
        return Swiper.Status.FAILURE;
      }
    } finally {
      downEvent.recycle();
    }
    return Swiper.Status.SUCCESS;
  }

  private float[][] interpolate(float[] start, float[] end, int steps) {
    float[][] res = new float[steps][2];
    for (int i = 1; i < steps + 1; i++) {
      res[i - 1][0] = start[0] + (end[0] - start[0]) * i / (steps + 2f);
      res[i - 1][1] = start[1] + (end[1] - start[1]) * i / (steps + 2f);
    }
    return res;
  }

  @NonNull private ViewAction loadM3U8AndMarkIdleWhenSuccess(final String url) {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() {
        return withClassName(equalTo(KmkExoVideoView.class.getName()));
      }

      @Override public String getDescription() {
        return "Kmk Exo Video View";
      }

      @Override public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        KmkExoVideoView kv = (KmkExoVideoView) view;
        kv.addListener(mockListener);
        idleResource.increment();
        idleResource.increment();
        KmkExoListener idleMarker = new ListenerAdapter() {
          @Override public void onDrawnToSurface(Surface surface) {
            idleResource.decrement();
          }

          @Override public void onVideoStarted() {
            idleResource.decrement();
          }
        };
        kv.addListener(idleMarker);
        kv.loadVideo(url);

        uiController.loopMainThreadUntilIdle();
        kv.removeListener(idleMarker);
      }
    };
  }

  @NonNull private ViewAction loadM3U8AndMarkIdleWhenLoadError(final String url) {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() {
        return withClassName(equalTo(KmkExoVideoView.class.getName()));
      }

      @Override public String getDescription() {
        return "Kmk Exo Video View";
      }

      @Override public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        KmkExoVideoView kv = (KmkExoVideoView) view;
        kv.addListener(mockListener);
        idleResource.increment();
        kv.addListener(new ListenerAdapter() {
          @Override public void onLoadError(Throwable throwable) {
            idleResource.decrement();
          }
        });
        kv.loadVideo(url);

        uiController.loopMainThreadUntilIdle();
      }
    };
  }

  @NonNull private ViewAction loadM3U8AndMarkIdleWhenPlaybackError(final String url) {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() {
        return withClassName(equalTo(KmkExoVideoView.class.getName()));
      }

      @Override public String getDescription() {
        return "Kmk Exo Video View";
      }

      @Override public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        KmkExoVideoView kv = (KmkExoVideoView) view;
        kv.addListener(mockListener);
        idleResource.increment();
        kv.addListener(new ListenerAdapter() {
          @Override public void onPlaybackError(ExoPlaybackException exception) {
            idleResource.decrement();
          }
        });
        kv.loadVideo(url);

        uiController.loopMainThreadUntilIdle();
      }
    };
  }

  private ViewAction swipeSeekBarAndWait() {
    return new ViewAction() {
      @Override public Matcher<View> getConstraints() {
        return withId(R.id.seekbar);
      }

      @Override public String getDescription() {
        return "Swiping Seekbar";
      }

      @Override public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float[] start = { (float) location[0], (float) location[1] };
        start[0] += (float) view.getPaddingLeft();
        start[1] += (float) view.getHeight() / 2.0f;

        float[] end = {
            (float) location[0]
                + ((float) view.getWidth() - (float) view.getPaddingRight()) * 0.97f, start[1]
        };

        sendLinearSwipe(uiController, start, end, new float[] { 1.0f, 1.0f }, 1000);
        uiController.loopMainThreadUntilIdle();
      }
    };
  }

  static class ListenerAdapter implements KmkExoListener {

    @Override public void onBufferingStarted() {

    }

    @Override public void onBufferingFinished(long bufferDuration) {

    }

    @Override public void onLoadError(Throwable throwable) {

    }

    @Override public void onPlaybackError(ExoPlaybackException exception) {

    }

    @Override public void onDrawnToSurface(Surface surface) {

    }

    @Override public void onVideoStarted() {

    }

    @Override public void onVideoFinished() {

    }

    @Override public void onPlayPressed() {

    }

    @Override public void onPausePressed() {

    }

    @Override public void onSeek(int start, int offset) {

    }
  }
}