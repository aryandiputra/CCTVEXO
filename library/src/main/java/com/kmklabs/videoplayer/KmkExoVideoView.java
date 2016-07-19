package com.kmklabs.videoplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.R;
import com.google.android.exoplayer.TrackRenderer;
import java.util.ArrayList;
import java.util.List;

public class KmkExoVideoView extends FrameLayout implements KmkMediaPlayerControl {
  public static final String PLAYER_NAME = "kmkexoplayer";
  public static final String VERSION = "0.1";
  public static final int UNKNOWN_TIME = -1;
  private static final String KEY_LOADED_URI = ".uri";
  private static final String KEY_LAST_POSITION = ".lastPosition";
  private static final String KEY_SUPER_STATE = ".super_state";
  private AspectRatioFrameLayout frameLayout;
  private SurfaceView surface;
  private ExoPlayer player;
  private ViewGroup anchor;
  private KmkVideoController videoController;
  private ListenerPool listenerPool = new ListenerPool();
  private TrackRenderer[] renderers;
  private String loadedUri;
  private PlayerFactory playerFactory = PlayerFactory.DEFAULT_HLS;
  // hack
  private int lastPosition = UNKNOWN_TIME;
  private boolean cancelled = false;
  private boolean detached;

  public KmkExoVideoView(Context context) {
    this(context, null);
  }

  public KmkExoVideoView(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public KmkExoVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    initUi(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public KmkExoVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    initUi(context);
  }

  public void setPlayerFactory(PlayerFactory playerFactory) {
    this.playerFactory = playerFactory;
  }

  @Override public List<MediaFormat> getVideoTracks() {
    List<MediaFormat> tracks = new ArrayList<>();
    if (player != null) {
      for (int i = 0; i < player.getTrackCount(PlayerFactory.TYPE_VIDEO); i++) {
        tracks.add(player.getTrackFormat(PlayerFactory.TYPE_VIDEO, i));
      }
    }
    return tracks;
  }

  @Override public int getSelectedVideoTrack() {
    if (player != null) {
      return player.getSelectedTrack(PlayerFactory.TYPE_VIDEO);
    } else {
      return -1;
    }
  }

  @Override public void setSelectedVideoTrack(int index) {
    if (player != null) {
      player.setSelectedTrack(PlayerFactory.TYPE_VIDEO, index);
    }
  }

  public void addListener(KmkExoListener listener) {
    listenerPool.addListener(listener);
  }

  public void removeListener(KmkExoListener listener) {
    listenerPool.remove(listener);
  }

  @Override public void start() {
    if (player != null) {
      player.setPlayWhenReady(true);
    }
  }

  @Override public void pause() {
    if (player != null) {
      player.setPlayWhenReady(false);
    }
  }

  @Override public int getDuration() {
    if (player != null) {
      return player.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0 : (int) player.getDuration();
    }
    return UNKNOWN_TIME;
  }

  @Override public int getCurrentPosition() {
    if (player != null) {
      return player.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0 : (int) player.getCurrentPosition();
    }
    return lastPosition;
  }

  @Override public void seekTo(int timeMillis) {
    if (player != null) {
      long seekPosition = player.getDuration() == player.UNKNOWN_TIME ? 0
          : Math.min(Math.max(0, timeMillis), getDuration());
      player.seekTo(seekPosition);
    }
  }

  @Override public boolean isPlaying() {
    if (player != null) {
      return player.getPlayWhenReady();
    }
    return false;
  }

  @Override public int getBufferPercentage() {
    if (player != null) {
      return player.getBufferedPercentage();
    }
    return -1;
  }

  //region playercontrol
  @Override public boolean canPause() {
    return true;
  }

  @Override public boolean canSeekBackward() {
    return true;
  }

  @Override public boolean canSeekForward() {
    return true;
  }

  /**
   * This is an unsupported operation.
   * <p>
   * Application of audio effects is dependent on the audio renderer used. When using
   * {@link com.google.android.exoplayer.MediaCodecAudioTrackRenderer}, the recommended approach is
   * to extend the class and override
   * {@link com.google.android.exoplayer.MediaCodecAudioTrackRenderer#onAudioSessionId}.
   *
   * @throws UnsupportedOperationException Always thrown.
   */
  @Override public int getAudioSessionId() {
    throw new UnsupportedOperationException();
  }

  public KmkVideoController getVideoController() {
    return videoController;
  }

  public void setVideoController(KmkVideoController controller) {
    this.anchor.removeAllViews();
    this.anchor.setVisibility(GONE);
    this.anchor.setOnTouchListener(null);
    this.videoController = controller;
    this.videoController.setListener(listenerPool);
    this.videoController.setMediaPlayer(this);
  }

  public void loadVideo(String url) {
    loadVideo(url, 0);
  }
  //endregion playercontrol

  public void loadVideo(final String url, final int startingPosition) {
    releasePlayer();
    cancelled = false;
    playerFactory.loadPlayer(getContext(), url, new PlayerFactory.LoaderCallback() {
      @Override
      public void onSuccess(TrackRenderer[] renderers, ListenerManagers managers) {
        if (cancelled || detached) {
          releasePlayer();
        } else {
          KmkExoVideoView.this.loadedUri = url;
          KmkExoVideoView.this.lastPosition = startingPosition;

          KmkExoVideoView.this.renderers = renderers;
          setPlayer(ExoPlayer.Factory.newInstance(4, 1000, 5000));
          player.seekTo(startingPosition);

          listenerPool.onSuccess(renderers, managers);

          managers.addVideoEventListener(new MediaCodecVideoTrackRenderer.EventListener() {

            @Override public void onDecoderInitializationError(
                MediaCodecTrackRenderer.DecoderInitializationException e) {
              listenerPool.onError(e);
            }

            @Override public void onCryptoError(MediaCodec.CryptoException e) {
              listenerPool.onError(e);
            }

            @Override public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                long initializationDurationMs) {

            }

            @Override public void onDroppedFrames(int count, long elapsed) {

            }

            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                float pixelWidthHeightRatio) {
              if(height > 0) {
                frameLayout.setAspectRatio((float) width / (float) height);
              }
            }

            @Override public void onDrawnToSurface(Surface surface) {

            }
          });
        }
      }

      @Override public void onError(Throwable throwable) {
        if (!cancelled) {
          listenerPool.onError(throwable);
        }
      }
    });
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    detached = true;
    releasePlayer();
  }

  @Override @CallSuper protected Parcelable onSaveInstanceState() {
    Bundle bundle = new Bundle();
    bundle.putString(KEY_LOADED_URI, loadedUri);
    bundle.putInt(KEY_LAST_POSITION, getCurrentPosition());
    bundle.putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState());
    return bundle;
  }

  @Override @CallSuper protected void onRestoreInstanceState(Parcelable state) {
    if (state instanceof Bundle) {
      Bundle bundle = (Bundle) state;
      if (bundle.getString(KEY_LOADED_URI) != null) {
        loadVideo(bundle.getString(KEY_LOADED_URI), bundle.getInt(KEY_LAST_POSITION, 0));
      }
      super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPER_STATE));
    } else {
      super.onRestoreInstanceState(state);
    }
  }

  private void initUi(Context context) {
    LayoutInflater.from(context).inflate(R.layout.kmk_exo_video_view, this, true);

    frameLayout = (AspectRatioFrameLayout) findViewById(R.id.video_frame);
    surface = (SurfaceView) findViewById(R.id.video_surface);
    anchor = (ViewGroup) findViewById(R.id.controller_anchor);

    surface.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
          KmkExoVideoView.this.player.sendMessage(renderers[0],
              MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, holder.getSurface());
        } else if (loadedUri != null) {
          loadVideo(loadedUri, lastPosition);
        }
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

      }

      @Override public void surfaceDestroyed(SurfaceHolder holder) {
        cancelled = true;
        releasePlayer();
      }
    });

    videoController = new KmkVideoController(getContext());
    videoController.setListener(listenerPool);
    videoController.setAnchorView(anchor);
    videoController.setMediaPlayer(this);

    setBackgroundColor(getResources().getColor(android.R.color.black));
  }

  private void releasePlayer() {
    if (player != null) {
      lastPosition = (int) player.getCurrentPosition();
      player.sendMessage(renderers[0], MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, null);
      player.release();
      player = null;
      renderers = null;
    }
  }

  protected void setPlayer(ExoPlayer player) {
    this.player = player;
    this.player.addListener(new ControllerHiderWhenBuffering());
    this.player.addListener(listenerPool);

    KmkExoVideoView.this.player.setPlayWhenReady(true);
    KmkExoVideoView.this.player.prepare(renderers);
    KmkExoVideoView.this.player.sendMessage(renderers[0],
        MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface.getHolder().getSurface());
  }

  private class ControllerHiderWhenBuffering implements ExoPlayer.Listener {
    @Override public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
      switch (playbackState) {
        case ExoPlayer.STATE_BUFFERING:
          videoController.hidePlayButton();
          break;
        case ExoPlayer.STATE_READY:
          videoController.showPlayButton();
          break;
        case ExoPlayer.STATE_IDLE:
          videoController.showPlayButton();
          break;
      }
    }

    @Override public void onPlayWhenReadyCommitted() {

    }

    @Override public void onPlayerError(ExoPlaybackException error) {
      videoController.showPlayButton();
    }
  }
}
