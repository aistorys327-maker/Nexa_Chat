package com.nexachat.app.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class AudioRecorderHelper {
    private static final String TAG = "AudioRecorderHelper";
    private MediaRecorder mediaRecorder;
    private File currentOutputFile;
    private long startTime;
    private boolean isRecording = false;

    public boolean startRecording(Context context) {
        try {
            File outputDir = new File(context.getCacheDir(), "voice_notes");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            currentOutputFile = new File(outputDir, "rec_" + System.currentTimeMillis() + ".m4a");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(context);
            } else {
                mediaRecorder = new MediaRecorder();
            }

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentOutputFile.getAbsolutePath());

            mediaRecorder.prepare();
            mediaRecorder.start();
            startTime = System.currentTimeMillis();
            isRecording = true;
            return true;
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Failed to start audio recording", e);
            release();
            return false;
        }
    }

    public File stopRecording() {
        if (!isRecording) return null;
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recorder", e);
        } finally {
            release();
        }
        return currentOutputFile;
    }

    public void cancelRecording() {
        if (!isRecording) return;
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (Exception ignored) {
        } finally {
            release();
            if (currentOutputFile != null && currentOutputFile.exists()) {
                currentOutputFile.delete();
            }
        }
    }

    public int getElapsedDurationSeconds() {
        if (!isRecording) return 0;
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    public boolean isRecording() {
        return isRecording;
    }

    private void release() {
        isRecording = false;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.reset();
                mediaRecorder.release();
            } catch (Exception ignored) {
            }
            mediaRecorder = null;
        }
    }
}
