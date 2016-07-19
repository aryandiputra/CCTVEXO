package com.kmklabs.videoplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.R;
import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

/**
 * Modified from our dearest Russian bratka (http://dajver.blogspot.com.br/2014/10/custom-mediacontroller-android.html)
 */
public class KmkVideoController extends FrameLayout {

  private static final String TAG = "VideoControllerView";
  private static final int PROGRESS_MAX = 1000;

  private MediaController.MediaPlayerControl mPlayer;
  private Context mContext;
  private ViewGroup mAnchor;

  private View mRoot;
  private TextView mEndTime, mCurrentTime;
  private boolean mShowing;
  private boolean mDragging;
  public static final int DEFAULT_TIMEOUT = 3000;
  private static final int FADE_OUT = 1;
  private static final int SHOW_PROGRESS = 2;
  StringBuilder mFormatBuilder;
  Formatter mFormatter;
  private ImageView mPauseButton;
  private SeekBar mProgress;
  private Handler mHandler = new MessageHandler(this);
  private EventListener listener;
  private ControllerListener controllerListener;
  private View mPopupAnchor;
  private ImageView trackSelector;
  private ImageView mFullScreen;
  private ExoPlayer exoPlayer;
  private PopupMenu popup;

  public KmkVideoController(Context context, AttributeSet attrs) {
    super(context, attrs);
    mRoot = null;
    mContext = context;

    Log.i(TAG, TAG);
    LayoutParams frameParams =
        new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    View v = makeControllerView();
    addView(v, frameParams);
  }

  public KmkVideoController(Context context) {
    this(context, null);

    Log.i(TAG, TAG);
  }

  @Override public void onFinishInflate() {
    super.onFinishInflate();
    if (mRoot != null) initControllerView(mRoot);
  }

  @Override protected void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    switch (newConfig.orientation) {
      case Configuration.ORIENTATION_LANDSCAPE:
        mFullScreen.setImageResource(R.drawable.ic_white_exit);
        break;
      case Configuration.ORIENTATION_PORTRAIT:
        mFullScreen.setImageResource(R.drawable.ic_white_fullscreen);
        break;
    }
  }

  public void setMediaPlayer(MediaController.MediaPlayerControl player) {
    mPlayer = player;
    popup = null;
  }

  void setListener(EventListener listener) {
    this.listener = listener;
  }

  /**
   * Устанавливает якорь на ту вьюху на которую вы хотите разместить контролы
   * Это может быть например VideoView или SurfaceView
   */
  public void setAnchorView(ViewGroup view) {
    if(mAnchor != null) mAnchor.setOnTouchListener(null);
    mAnchor = view;
    mAnchor.setOnTouchListener(new OnTouchListener() {
      @Override public boolean onTouch(View v, MotionEvent event) {
        return onTouchEvent(event);
      }
    });
  }

  public void hidePlayButton() {
    mPauseButton.setVisibility(INVISIBLE);
  }

  public void showPlayButton() {
    mPauseButton.setVisibility(VISIBLE);
  }

  public void setControllerListener(ControllerListener controllerListener) {
    this.controllerListener = controllerListener;
  }

  /**
   * Создает вьюху которая будет находится поверх вашего VideoView или другого контролла
   */
  protected View makeControllerView() {
    LayoutInflater inflate =
        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    mRoot = inflate.inflate(R.layout.kmk_video_controller, null);

    initControllerView(mRoot);

    return mRoot;
  }

  private void initControllerView(View v) {
    v.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        hide();
      }
    });
    mPauseButton = (ImageView) v.findViewById(R.id.image_pause);
    if (mPauseButton != null) {
      mPauseButton.requestFocus();
      mPauseButton.setOnClickListener(mPauseListener);
    }

    mProgress = (SeekBar) v.findViewById(R.id.seekbar);
    mProgress.setOnSeekBarChangeListener(mSeekListener);
    mProgress.setMax(PROGRESS_MAX);

    mEndTime = (TextView) v.findViewById(R.id.text_total_time);
    mCurrentTime = (TextView) v.findViewById(R.id.text_current_time);
    mFormatBuilder = new StringBuilder();
    mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    mPopupAnchor = v.findViewById(R.id.kvv_popup_anchor);
    mFullScreen = (ImageView) v.findViewById(R.id.request_fullscreen);
    trackSelector = (ImageView) v.findViewById(R.id.change_video_track);
    mFullScreen.setVisibility(GONE);
  }

  /**
   * Показывает контроллер на экране
   * Он будет убран через 3 секуунды
   */
  public void show() {
    show(DEFAULT_TIMEOUT);
  }

  /**
   * Отключить паузу или seek button, если поток не может быть приостановлена
   * Это требует интерфейс управления MediaPlayerControlExt
   */
  private void disableUnsupportedButtons() {
    if (mPlayer == null) {
      return;
    }

    try {
      if (mPauseButton != null && !mPlayer.canPause()) {
        mPauseButton.setEnabled(false);
      }
    } catch (IncompatibleClassChangeError ex) {
      //выводите в лог что хотите из ex
      Log.w(TAG, ex);
    }
  }

  /**
   * Показывает контроллы на экране
   * Он исчезнет автоматически после своего таймаута
   */
  public void show(int timeout) {
    if (!mShowing && mAnchor != null) {
      setProgress();
      if (mPauseButton != null) {
        mPauseButton.requestFocus();
      }
      disableUnsupportedButtons();

      LayoutParams tlp =
          new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
              Gravity.BOTTOM);

      mAnchor.addView(this, tlp);
      mShowing = true;
    }
    updatePausePlay();
    updateTracks();

    mHandler.sendEmptyMessage(SHOW_PROGRESS);

    Message msg = mHandler.obtainMessage(FADE_OUT);
    if (timeout != 0) {
      mHandler.removeMessages(FADE_OUT);
      mHandler.sendMessageDelayed(msg, timeout);
    }

    if (controllerListener != null) controllerListener.onShown();
  }

  public void setFullscreenListener(OnClickListener listener) {
    if (listener != null) {
      mFullScreen.setVisibility(VISIBLE);
      mFullScreen.setOnClickListener(listener);
    } else {
      mFullScreen.setVisibility(GONE);
      mFullScreen.setOnClickListener(null);
    }
  }

  public boolean isShowing() {
    return mShowing;
  }

  /**
   * Удаляем контроллы с экрана.
   */
  public void hide() {
    if (mAnchor == null) {
      return;
    }

    try {
      mHandler.removeMessages(SHOW_PROGRESS);
      mAnchor.removeView(this);
    } catch (IllegalArgumentException ex) {
      Log.w("MediaController", "already removed");
    }
    mShowing = false;

    if (controllerListener != null) controllerListener.onHidden();
  }

  private String stringForTime(int timeMs) {
    int totalSeconds = timeMs / 1000;

    int seconds = totalSeconds % 60;
    int minutes = (totalSeconds / 60) % 60;
    int hours = totalSeconds / 3600;

    mFormatBuilder.setLength(0);
    if (hours > 0) {
      return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
    } else {
      return mFormatter.format("%02d:%02d", minutes, seconds).toString();
    }
  }

  private int setProgress() {
    if (mPlayer == null || mDragging) {
      return 0;
    }

    int position = mPlayer.getCurrentPosition();
    int duration = mPlayer.getDuration();
    if (mProgress != null) {
      if (duration > 0) {
        // use long to avoid overflow
        double currentPosition = (double) position / (double) duration;
        int perMille = (int) ((double) PROGRESS_MAX * currentPosition);
        mProgress.setProgress(perMille);
      }
      int percent = mPlayer.getBufferPercentage();
      mProgress.setSecondaryProgress(percent * 10);
    }

    if (mEndTime != null) mEndTime.setText(stringForTime(duration));
    if (mCurrentTime != null) mCurrentTime.setText(stringForTime(position));

    return position;
  }

  @Override public boolean onTouchEvent(MotionEvent event) {
    show(DEFAULT_TIMEOUT);

    return true;
  }

  @Override public boolean onTrackballEvent(MotionEvent ev) {
    show(DEFAULT_TIMEOUT);
    return false;
  }

  @Override public boolean dispatchKeyEvent(KeyEvent event) {
    if (mPlayer == null) {
      return true;
    }

    int keyCode = event.getKeyCode();
    final boolean uniqueDown =
        event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
    if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
        || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        || keyCode == KeyEvent.KEYCODE_SPACE) {
      if (uniqueDown) {
        doPauseResume();
        show(DEFAULT_TIMEOUT);
        if (mPauseButton != null) {
          mPauseButton.requestFocus();
        }
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
      if (uniqueDown && !mPlayer.isPlaying()) {
        mPlayer.start();
        updatePausePlay();
        show(DEFAULT_TIMEOUT);
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
      if (uniqueDown && mPlayer.isPlaying()) {
        mPlayer.pause();
        updatePausePlay();
        show(DEFAULT_TIMEOUT);
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
      return super.dispatchKeyEvent(event);
    } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
      if (uniqueDown) {
        hide();
      }
      return true;
    }

    show(DEFAULT_TIMEOUT);
    return super.dispatchKeyEvent(event);
  }

  private OnClickListener mPauseListener = new OnClickListener() {
    public void onClick(View v) {
      doPauseResume();
      show(DEFAULT_TIMEOUT);
    }
  };

  @TargetApi(Build.VERSION_CODES.KITKAT) public void updateTracks() {
    if (mPlayer instanceof KmkMediaPlayerControl && popup == null) {
      final KmkMediaPlayerControl player = (KmkMediaPlayerControl) mPlayer;
      List<MediaFormat> tracks = player.getVideoTracks();

      if (tracks.size() > 0 && player.getSelectedVideoTrack() >= 0) {
        final int[] resId = new int[tracks.size()];
        this.popup = new PopupMenu(getContext(), mPopupAnchor);
        Menu menu = popup.getMenu();
        for (int i = 0; i < tracks.size(); i++) {
          resId[i] = R.drawable.ic_white_auto;
          if (tracks.get(i).adaptive) {
            menu.add(1, i, i, R.string.kvv_adaptive_track).setCheckable(true);
          } else {
            if (tracks.get(i).trackId != null) {
              String trackid = tracks.get(i).trackId.toLowerCase();
              if (trackid.contains("sd")) {
                resId[i] = R.drawable.ic_white_sd;
              } else if (trackid.contains("hd")) {
                resId[i] = R.drawable.ic_white_hd;
              } else if (trackid.contains("3g") || trackid.contains("3 g")) {
                resId[i] = R.drawable.ic_white_3_g;
              } else {
                resId[i] = R.drawable.ic_white_sd;
              }
            }
            menu.add(1, i, i, tracks.get(i).trackId).setCheckable(true);
          }
        }
        menu.setGroupCheckable(1, true, true);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
          @Override public boolean onMenuItemClick(MenuItem item) {
            player.setSelectedVideoTrack(item.getItemId());
            trackSelector.setImageResource(resId[player.getSelectedVideoTrack()]);
            show();
            return true;
          }
        });

        trackSelector.setImageResource(resId[player.getSelectedVideoTrack()]);
        trackSelector.setOnClickListener(new OnClickListener() {
          @Override public void onClick(View v) {
            show(3600000);
            popup.getMenu().getItem(player.getSelectedVideoTrack()).setChecked(true);
            popup.show();
          }
        });
      }
    }

    if (popup != null) {
      trackSelector.setVisibility(VISIBLE);
    } else {
      trackSelector.setVisibility(INVISIBLE);
    }
  }

  public void updatePausePlay() {
    if (mRoot == null || mPauseButton == null || mPlayer == null) {
      return;
    }

    if (mPlayer.getCurrentPosition() >= mPlayer.getDuration()) {
      // TODO retry
      mPauseButton.setImageResource(R.drawable.ic_white_play_arrow);
    } else if (mPlayer.isPlaying()) {
      mPauseButton.setImageResource(R.drawable.ic_white_pause);
    } else {
      mPauseButton.setImageResource(R.drawable.ic_white_play_arrow);
    }
  }

  private void doPauseResume() {
    if (mPlayer == null) {
      return;
    }

    if (mPlayer.getCurrentPosition() >= mPlayer.getDuration()) {
      mPlayer.seekTo(0);
      mPlayer.start();
      if (listener != null) listener.onPlayPressed();
    } else if (mPlayer.isPlaying()) {
      mPlayer.pause();
      if (listener != null) listener.onPausePressed();
    } else {
      mPlayer.start();
      if (listener != null) listener.onPlayPressed();
    }
    updatePausePlay();
  }

  private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
    int lastPosition;

    public void onStartTrackingTouch(SeekBar bar) {
      show(3600000);

      mDragging = true;
      mHandler.removeMessages(SHOW_PROGRESS);
      lastPosition = bar.getProgress();
    }

    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
      if (mPlayer == null) {
        return;
      }

      if (!fromuser) {
        return;
      }

      long duration = mPlayer.getDuration();
      long newposition = (duration * progress) / PROGRESS_MAX;
      mPlayer.seekTo((int) newposition);
      if (mCurrentTime != null) {
        mCurrentTime.setText(stringForTime((int) newposition));
      }
    }

    public void onStopTrackingTouch(SeekBar bar) {
      mDragging = false;
      setProgress();
      updatePausePlay();
      show(DEFAULT_TIMEOUT);

      int duration = mPlayer.getDuration();
      int offsetPercentage = bar.getProgress() - lastPosition;

      int offsetTime = (offsetPercentage * duration) / PROGRESS_MAX;
      int startTime = (lastPosition * duration) / PROGRESS_MAX;
      if (listener != null) listener.onSeek(startTime, offsetTime);

      mHandler.sendEmptyMessage(SHOW_PROGRESS);
    }
  };

  @Override public void setEnabled(boolean enabled) {
    if (mPauseButton != null) {
      mPauseButton.setEnabled(enabled);
    }
    if (mProgress != null) {
      mProgress.setEnabled(enabled);
    }
    if (mFullScreen != null) {
      mFullScreen.setEnabled(enabled);
    }
    disableUnsupportedButtons();
    super.setEnabled(enabled);
  }

  private static class MessageHandler extends Handler {
    private final WeakReference<KmkVideoController> mView;

    MessageHandler(KmkVideoController view) {
      mView = new WeakReference<KmkVideoController>(view);
    }

    @Override public void handleMessage(Message msg) {
      KmkVideoController view = mView.get();
      if (view == null || view.mPlayer == null) {
        return;
      }

      int pos;
      switch (msg.what) {
        case FADE_OUT:
          view.hide();
          break;
        case SHOW_PROGRESS:
          view.updatePausePlay();
          pos = view.setProgress();
          if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {
            msg = obtainMessage(SHOW_PROGRESS);
            sendMessageDelayed(msg, 1000 - (pos % 1000));
          }
          break;
      }
    }
  }

  public interface ControllerListener {
    void onShown();

    void onHidden();
  }

  interface EventListener {
    void onPlayPressed();

    void onPausePressed();

    void onSeek(int start, int offset);
  }
}