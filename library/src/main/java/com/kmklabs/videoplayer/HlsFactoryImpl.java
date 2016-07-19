package com.kmklabs.videoplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Looper;
import com.google.android.exoplayer.DefaultLoadControl;
import com.google.android.exoplayer.LoadControl;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.hls.DefaultHlsTrackSelector;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.hls.PtsTimestampAdjusterProvider;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.TextRenderer;
import com.google.android.exoplayer.text.eia608.Eia608TrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HlsFactoryImpl implements PlayerFactory {
  @Override
  public void loadPlayer(Context context, String url, LoaderCallback playerLoader) {
    new ManifestFetcher<>(url, new DefaultUriDataSource(context, DEFAULT_USER_AGENT),
        new HlsPlaylistParser()).singleLoad(Looper.getMainLooper(),
        callback(context, url, playerLoader));
  }

  private static ManifestFetcher.ManifestCallback<HlsPlaylist> callback(final Context context,
      final String url, final LoaderCallback playerLoader) {
    return new ManifestFetcher.ManifestCallback<HlsPlaylist>() {
      @Override public void onSingleManifest(HlsPlaylist manifest) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(BUFFER_SEGMENT_SIZE));
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        PtsTimestampAdjusterProvider timestampAdjusterProvider = new PtsTimestampAdjusterProvider();

        com.kmklabs.videoplayer.ListenerManagers listenerManagers = new com.kmklabs.videoplayer.ListenerManagers();
        DataSource dataSource =
            new DefaultUriDataSource(context, bandwidthMeter, DEFAULT_USER_AGENT);
        HlsChunkSource chunkSource =
            new HlsChunkSource(true, dataSource, url, manifest, DefaultHlsTrackSelector.newDefaultInstance(context), bandwidthMeter, timestampAdjusterProvider,
                HlsChunkSource.ADAPTIVE_MODE_SPLICE);
        HlsSampleSource sampleSource =
            new HlsSampleSource(chunkSource, loadControl, BUFFER_SEGMENTS * BUFFER_SEGMENT_SIZE,
                mainHandler, new HlsSampleSource.EventListener() {
              @Override public void onLoadStarted(int sourceId, long length, int type, int trigger,
                  Format format, long mediaStartTimeMs, long mediaEndTimeMs) {

              }

              @Override
              public void onLoadCompleted(int sourceId, long bytesLoaded, int type, int trigger,
                  Format format, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
                  long loadDurationMs) {

              }

              @Override public void onLoadCanceled(int sourceId, long bytesLoaded) {

              }

              @Override public void onLoadError(int sourceId, IOException e) {

              }

              @Override public void onUpstreamDiscarded(int sourceId, long mediaStartTimeMs,
                  long mediaEndTimeMs) {

              }

              @Override
              public void onDownstreamFormatChanged(int sourceId, Format format, int trigger,
                  long mediaTimeMs) {

              }
            }, TYPE_VIDEO);
        MediaCodecVideoTrackRenderer videoRenderer =
            new MediaCodecVideoTrackRenderer(context, sampleSource, MediaCodecSelector.DEFAULT,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, mainHandler, listenerManagers, 50);
        MediaCodecAudioTrackRenderer audioRenderer =
            new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT, null, true, mainHandler,
                new MediaCodecAudioTrackRenderer.EventListener() {
                  @Override public void onAudioTrackInitializationError(
                      AudioTrack.InitializationException e) {

                  }

                  @Override public void onAudioTrackWriteError(AudioTrack.WriteException e) {

                  }

                  @Override public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs,
                      long elapsedSinceLastFeedMs) {

                  }

                  @Override public void onDecoderInitializationError(
                      MediaCodecTrackRenderer.DecoderInitializationException e) {

                  }

                  @Override public void onCryptoError(MediaCodec.CryptoException e) {

                  }

                  @Override
                  public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs,
                      long initializationDurationMs) {

                  }
                }, AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);
        MetadataTrackRenderer<Map<String, Object>> id3Renderer =
            new MetadataTrackRenderer<>(sampleSource, new Id3Parser(),
                new MetadataTrackRenderer.MetadataRenderer<Map<String, Object>>() {
                  @Override public void onMetadata(Map<String, Object> metadata) {

                  }
                }, mainHandler.getLooper());
        Eia608TrackRenderer closedCaptionRenderer =
            new Eia608TrackRenderer(sampleSource, new TextRenderer() {
              @Override public void onCues(List<Cue> cues) {

              }
            }, mainHandler.getLooper());

        TrackRenderer[] renderers = new TrackRenderer[RENDERER_COUNT];
        renderers[TYPE_VIDEO] = videoRenderer;
        renderers[TYPE_AUDIO] = audioRenderer;
        renderers[TYPE_METADATA] = id3Renderer;
        renderers[TYPE_TEXT] = closedCaptionRenderer;

        playerLoader.onSuccess(renderers, listenerManagers);
      }

      @Override public void onSingleManifestError(IOException e) {
        playerLoader.onError(e);
      }
    };
  }
}
