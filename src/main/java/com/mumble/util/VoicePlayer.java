package com.mumble.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

/**
 * VoicePlayer — plays WAV audio from a byte array using the Java Sound API.
 *
 * Playback runs on a daemon thread so it never blocks the EDT.
 * Supports stop() to interrupt mid-playback.
 *
 * Usage:
 *   VoicePlayer player = new VoicePlayer(wavBytes);
 *   player.play();    // non-blocking
 *   player.stop();    // can be called at any time
 */
public class VoicePlayer {

    private final byte[] wavBytes;
    private volatile Clip clip;
    private volatile boolean playing = false;

    /** Listener called when playback completes (or is stopped). */
    public interface OnCompleteListener {
        void onComplete();
    }

    private OnCompleteListener onCompleteListener;

    public VoicePlayer(byte[] wavBytes) {
        this.wavBytes = wavBytes;
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.onCompleteListener = listener;
    }

    public boolean isPlaying() { return playing; }

    /**
     * Starts audio playback on a daemon thread.
     * Safe to call from the EDT.
     */
    public void play() {
        if (playing) return;
        playing = true;

        Thread t = new Thread(() -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                        new ByteArrayInputStream(wavBytes));
                clip = AudioSystem.getClip();
                clip.open(ais);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playing = false;
                        clip.close();
                        if (onCompleteListener != null) onCompleteListener.onComplete();
                    }
                });
                clip.start();
            } catch (Exception e) {
                playing = false;
                if (onCompleteListener != null) onCompleteListener.onComplete();
            }
        }, "VoicePlayer-Playback");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Stops ongoing playback immediately.
     */
    public void stop() {
        playing = false;
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
}
