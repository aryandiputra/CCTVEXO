package com.kmklabs.videoplayer;

import android.view.Surface;
import com.google.android.exoplayer.ExoPlaybackException;

public interface KmkExoListener extends KmkVideoController.EventListener {
  void onBufferingStarted();
  void onBufferingFinished(long bufferDuration);
  void onLoadError(Throwable throwable);
  void onPlaybackError(ExoPlaybackException exception);
  void onDrawnToSurface(Surface surface);
  void onVideoStarted();
  void onVideoFinished();
}
