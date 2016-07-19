package com.kmklabs.videoplayer;

import android.media.MediaCodec;
import android.view.Surface;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import java.util.ArrayList;
import java.util.List;

public class ListenerPool
    implements ExoPlayer.Listener, PlayerFactory.LoaderCallback, KmkVideoController.EventListener {
  private List<KmkExoListener> listeners = new ArrayList<>();
  private long startBufferTime = System.currentTimeMillis();
  private boolean isFirstReady = true;
  private boolean fromBuffering = false;

  public void addListener(KmkExoListener listener) {
    listeners.add(listener);
  }

  public void remove(KmkExoListener listener) {
    this.listeners.remove(listener);
  }

  @Override public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    switch (playbackState) {
      case ExoPlayer.STATE_BUFFERING:
        startBufferTime = System.currentTimeMillis();
        onBufferingStarted();
        fromBuffering = true;
        break;
      case ExoPlayer.STATE_READY:
        if (isFirstReady) {
          onStarted();
        }
        onBufferingFinished(System.currentTimeMillis() - startBufferTime);
        startBufferTime = System.currentTimeMillis();
        break;
      case ExoPlayer.STATE_PREPARING:
        startBufferTime = System.currentTimeMillis();
        break;
      case ExoPlayer.STATE_ENDED:
        onVideoFinished();
        break;
    }

    // hack for buffering issues
    if (playbackState != ExoPlayer.STATE_BUFFERING) {
      if (fromBuffering) onBufferingFinished(System.currentTimeMillis() - startBufferTime);
      fromBuffering = false;
    }
  }

  private void onVideoFinished() {
    for (KmkExoListener listener : listeners) {
      listener.onVideoFinished();
    }
  }

  private void onBufferingFinished(long bufferingDuration) {
    for (KmkExoListener listener : listeners) {
      listener.onBufferingFinished(bufferingDuration);
    }
  }

  private void onStarted() {
    for (KmkExoListener listener : listeners) {
      listener.onVideoStarted();
    }
    isFirstReady = false;
  }

  private void onBufferingStarted() {
    for (KmkExoListener listener : listeners) {
      listener.onBufferingStarted();
    }
  }

  @Override public void onPlayWhenReadyCommitted() {

  }

  @Override public void onPlayerError(ExoPlaybackException error) {
    for (KmkExoListener listener : listeners) {
      listener.onPlaybackError(error);
    }
  }

  //region PlayerLoader

  @Override public void onSuccess(TrackRenderer[] renderers,
      ListenerManagers listenerManager) {
    listenerManager.addVideoEventListener(new MediaCodecVideoTrackRenderer.EventListener() {
      @Override public void onDroppedFrames(int count, long elapsed) {

      }

      @Override public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
          float pixelWidthHeightRatio) {

      }

      @Override public void onDrawnToSurface(Surface surface) {
        for(KmkExoListener listener : listeners) {
          listener.onDrawnToSurface(surface);
        }
      }

      @Override public void onDecoderInitializationError(
          MediaCodecTrackRenderer.DecoderInitializationException e) {

      }

      @Override public void onCryptoError(MediaCodec.CryptoException e) {

      }

      @Override public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
          long initializationDurationMs) {

      }
    });
  }

  @Override public void onError(Throwable throwable) {
    for (KmkExoListener listener : listeners) {
      listener.onLoadError(throwable);
    }
  }
  //endregion

  //region playercontroller
  @Override public void onPlayPressed() {
    for (KmkExoListener listener : listeners) {
      listener.onPlayPressed();
    }
  }

  @Override public void onPausePressed() {
    for (KmkExoListener listener : listeners) {
      listener.onPausePressed();
    }
  }

  @Override public void onSeek(int start, int offset) {
    for (KmkExoListener listener : listeners) {
      listener.onSeek(start, offset);
    }
  }
  //endregion
}
