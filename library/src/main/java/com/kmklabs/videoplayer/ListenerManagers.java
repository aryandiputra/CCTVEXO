package com.kmklabs.videoplayer;

import android.media.MediaCodec;
import android.view.Surface;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import java.util.ArrayList;
import java.util.List;

public class ListenerManagers implements MediaCodecVideoTrackRenderer.EventListener {
  List<MediaCodecVideoTrackRenderer.EventListener> videoEventListeners = new ArrayList<>();

  // region MediaCodecVideoTrackRenderer
  @Override public void onDroppedFrames(int count, long elapsed) {
    for(MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
      l.onDroppedFrames(count, elapsed);
    }
  }

  @Override public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
      float pixelWidthHeightRatio) {
    for(MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
      l.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
    }
  }

  @Override public void onDrawnToSurface(Surface surface) {
    for(MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
      l.onDrawnToSurface(surface);
    }
  }

  @Override public void onDecoderInitializationError(
      MediaCodecTrackRenderer.DecoderInitializationException e) {
    for(MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
      l.onDecoderInitializationError(e);
    }
  }

  @Override public void onCryptoError(MediaCodec.CryptoException e) {
    for(MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
      l.onCryptoError(e);
    }
  }

  @Override public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
      long initializationDurationMs) {
    for(MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
      l.onDecoderInitialized(decoderName, elapsedRealtimeMs, initializationDurationMs);
    }
  }

  public void addVideoEventListener(MediaCodecVideoTrackRenderer.EventListener eventListener) {
    this.videoEventListeners.add(eventListener);
  }

  // endregion
}
