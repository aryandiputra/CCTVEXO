package com.kmklabs.videoplayer;

import android.widget.MediaController;
import com.google.android.exoplayer.MediaFormat;
import java.util.List;

public interface KmkMediaPlayerControl extends MediaController.MediaPlayerControl {
  List<MediaFormat> getVideoTracks();
  int getSelectedVideoTrack();
  void setSelectedVideoTrack(int index);
}
