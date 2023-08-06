package com.yehia.record_view;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;

import java.io.IOException;

public class AudioRecorder {
    private MediaRecorder mediaRecorder;

    private void initMediaRecorder(Context context) {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(320000);
        mediaRecorder.setAudioSamplingRate(44100);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int audioSessionId = audioManager.generateAudioSessionId();
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor.create(audioSessionId);
        }
    }


    void start(String filePath, Context context) throws IOException {
        if (mediaRecorder == null) {
            initMediaRecorder(context);
        }
        try {
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
        }
    }

    void stop() {
        try {
            mediaRecorder.stop();
            destroyMediaRecorder();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void destroyMediaRecorder() {
        mediaRecorder.release();
        mediaRecorder = null;
    }

}
