package com.kmklabs.videoplayer;

import android.content.Context;
import com.google.android.exoplayer.TrackRenderer;

public interface PlayerFactory {
  static final int RENDERER_COUNT = 4;
  static final String DEFAULT_USER_AGENT = "kmklabs/exoPlayer";
  static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
  static final int BUFFER_SEGMENTS = 256;
  public static final int TYPE_VIDEO = 0;
  public static final int TYPE_AUDIO = 1;
  public static final int TYPE_TEXT = 2;
  public static final int TYPE_METADATA = 3;
  PlayerFactory DEFAULT_HLS = new HlsFactoryImpl();

  void loadPlayer(Context context, String path, LoaderCallback loader);

  interface LoaderCallback {
    void onSuccess(TrackRenderer[] renderers, ListenerManagers listenerManager);

    void onError(Throwable throwable);
  }
}
