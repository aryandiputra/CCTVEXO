package com.kmklabs.videoplayer;

import android.media.MediaCodec;
import android.view.Surface;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.hls.HlsSampleSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KmkPlayerFactory {
  public interface PlayerLoader {
    void onSuccess(TrackRenderer[] renderers, ListenerManagers listenerManager);

    void onError(Throwable throwable);
  }

  public static class ListenerManagers
      implements HlsSampleSource.EventListener, MediaCodecVideoTrackRenderer.EventListener {
    List<HlsSampleSource.EventListener> sampleSourceListeners = new ArrayList<>();
    List<MediaCodecVideoTrackRenderer.EventListener> videoEventListeners = new ArrayList<>();

    public void addHlsSampleSourceListener(HlsSampleSource.EventListener listener) {
      this.sampleSourceListeners.add(listener);
    }

    @Override
    public void onLoadStarted(int sourceId, long length, int type, int trigger, Format format,
        long mediaStartTimeMs, long mediaEndTimeMs) {
      for (HlsSampleSource.EventListener lis : sampleSourceListeners) {
        lis.onLoadStarted(sourceId, length, type, trigger, format, mediaStartTimeMs,
            mediaEndTimeMs);
      }
    }

    @Override public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger,
        Format format, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs) {
      for (HlsSampleSource.EventListener lis : sampleSourceListeners) {
        lis.onLoadCompleted(sourceId, bytesLoaded, type, trigger, format, mediaStartTimeMs,
            mediaEndTimeMs, elapsedRealtimeMs, loadDurationMs);
      }
    }

    @Override public void onLoadCanceled(int sourceId, long bytesLoaded) {
      for (HlsSampleSource.EventListener lis : sampleSourceListeners) {
        lis.onLoadCanceled(sourceId, bytesLoaded);
      }
    }

    @Override public void onLoadError(int sourceId, IOException e) {
      for (HlsSampleSource.EventListener lis : sampleSourceListeners) {
        lis.onLoadError(sourceId, e);
      }
    }

    @Override
    public void onUpstreamDiscarded(int sourceId, long mediaStartTimeMs, long mediaEndTimeMs) {
      for (HlsSampleSource.EventListener lis : sampleSourceListeners) {
        lis.onUpstreamDiscarded(sourceId, mediaStartTimeMs, mediaEndTimeMs);
      }
    }

    @Override public void onDownstreamFormatChanged(int sourceId, Format format, int trigger,
        long mediaTimeMs) {
      for (HlsSampleSource.EventListener lis : sampleSourceListeners) {
        lis.onDownstreamFormatChanged(sourceId, format, trigger, mediaTimeMs);
      }
    }

    // region MediaCodecVideoTrackRenderer
    @Override public void onDroppedFrames(int count, long elapsed) {
      for (MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
        l.onDroppedFrames(count, elapsed);
      }
    }

    @Override public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
        float pixelWidthHeightRatio) {
      for (MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
        l.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
      }
    }

    @Override public void onDrawnToSurface(Surface surface) {
      for (MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
        l.onDrawnToSurface(surface);
      }
    }

    @Override public void onDecoderInitializationError(
        MediaCodecTrackRenderer.DecoderInitializationException e) {
      for (MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
        l.onDecoderInitializationError(e);
      }
    }

    @Override public void onCryptoError(MediaCodec.CryptoException e) {
      for (MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
        l.onCryptoError(e);
      }
    }

    @Override public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
        long initializationDurationMs) {
      for (MediaCodecVideoTrackRenderer.EventListener l : videoEventListeners) {
        l.onDecoderInitialized(decoderName, elapsedRealtimeMs, initializationDurationMs);
      }
    }

    public void addVideoEventListener(MediaCodecVideoTrackRenderer.EventListener eventListener) {
      this.videoEventListeners.add(eventListener);
    }

    // endregion
  }
}
