package id.ryandzhunter.cctvexo;

import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.exoplayer.ExoPlaybackException;
import com.kmklabs.videoplayer.KmkExoListener;
import com.kmklabs.videoplayer.KmkExoVideoView;


public class MainActivity extends AppCompatActivity{

    private KmkExoVideoView videoView;
    private EditText url;
    private Button hit;
    private TextView status;
    private String link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupVideoView();
    }

    private void setupVideoView() {
        url = (EditText) findViewById(R.id.url);
        videoView = (KmkExoVideoView) findViewById(R.id.video_view);
        hit  = (Button) findViewById(R.id.hit);
        status = (TextView) findViewById(R.id.status);
//        emVideoView.setOnPreparedListener(this);

        link = getIntent().getStringExtra("url");

        hit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                videoView.loadVideo(link);
            }
        });

        videoView.addListener(new KmkExoListener() {
            @Override public void onBufferingStarted() {
                status.setText("Buffering Started");
            }

            @Override public void onBufferingFinished(long bufferDuration) {
                status.setText("Buffering Finished");
            }

            @Override public void onLoadError(Throwable throwable) {
                status.setText("Load Error " + throwable.getMessage());
            }

            @Override public void onPlaybackError(ExoPlaybackException exception) {
                status.setText("Playback Error " + exception.getMessage());
            }

            @Override public void onDrawnToSurface(Surface surface) {
                status.setText("Rendered on " + surface);
            }

            @Override public void onVideoStarted() {
                status.setText("Video Started");
            }

            @Override public void onVideoFinished() {
                status.setText("Video Finished");
            }

            @Override public void onPlayPressed() {
            }

            @Override public void onPausePressed() {

            }

            @Override public void onSeek(int start, int offset) {

            }
        });
    }

        //For now we just picked an arbitrary item to play.  More can be found at
        //https://archive.org/details/more_animation
//        emVideoView.setVideoURI(Uri.parse(link));

//        emVideoView.setVideoURI(Uri.parse("http://www.mediacollege.com/video-gallery/testclips/20051210-w50s_56K.flv"));

//        emVideoView.setVideoURI(Uri.parse("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4"));

}
