package com.nexachat.app.utils;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

public class AudioPlayerHelper {
    private static final String TAG = "AudioPlayerHelper";
    private static AudioPlayerHelper instance;

    private MediaPlayer mediaPlayer;
    private String currentlyPlayingUrl;
    private OnPlaybackListener currentListener;

    public interface OnPlaybackListener {
        void onStart();
        void onStop();
        void onError();
    }

    private AudioPlayerHelper() {
    }

    public static synchronized AudioPlayerHelper getInstance() {
        if (instance == null) {
            instance = new AudioPlayerHelper();
        }
        return instance;
    }

    public void play(String url, OnPlaybackListener listener) {
        if (currentlyPlayingUrl != null && currentlyPlayingUrl.equals(url) && isPlaying()) {
            pause();
            return;
        }

        stop();
        this.currentlyPlayingUrl = url;
        this.currentListener = listener;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                if (currentListener != null) {
                    currentListener.onStart();
                }
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                if (currentListener != null) {
                    currentListener.onStop();
                }
                stop();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what);
                if (currentListener != null) {
                    currentListener.onError();
                }
                stop();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source for media player", e);
            if (listener != null) {
                listener.onError();
            }
            stop();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (currentListener != null) {
                currentListener.onStop();
            }
        }
    }

    public void stop() {
        if (currentListener != null) {
            currentListener.onStop();
            currentListener = null;
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
        currentlyPlayingUrl = null;
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentlyPlayingUrl() {
        return currentlyPlayingUrl;
    }
}
