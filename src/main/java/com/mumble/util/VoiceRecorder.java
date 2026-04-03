package com.mumble.util;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

/**
 * VoiceRecorder — captures microphone audio in WAV format using the Java Sound API.
 *
 * Lifecycle:
 *   VoiceRecorder r = new VoiceRecorder();
 *   r.startRecording();        // begin capturing microphone
 *   ...user speaks...
 *   byte[] wav = r.stopAndGetWav();   // returns WAV bytes
 *
 * The recorder is non-blocking: recording happens on a daemon thread.
 * On completion, the raw PCM is encoded to a WAV byte array suitable for:
 *   - Sending over a socket (Base64 encoded in a TextMessage)
 *   - Playback via VoicePlayer
 */
public class VoiceRecorder {

    // Mono, 16-bit, 16 kHz — good quality with minimal size
    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000f,  // sample rate
            16,      // sample size bits
            1,       // channels (mono)
            2,       // frame size (16-bit mono = 2 bytes)
            16000f,  // frame rate
            false    // little-endian
    );

    private TargetDataLine line;
    private ByteArrayOutputStream pcmBuffer;
    private Thread captureThread;
    private volatile boolean recording = false;

    // ─── State ────────────────────────────────────────────────────

    public boolean isRecording() { return recording; }

    // ─── API ──────────────────────────────────────────────────────

    /**
     * Starts recording from the default microphone.
     * @throws LineUnavailableException if no microphone is available.
     */
    public void startRecording() throws LineUnavailableException {
        if (recording) return;

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported on this system.");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(FORMAT);
        line.start();

        pcmBuffer = new ByteArrayOutputStream();
        recording = true;

        captureThread = new Thread(() -> {
            byte[] chunk = new byte[1024];
            while (recording) {
                int read = line.read(chunk, 0, chunk.length);
                if (read > 0) pcmBuffer.write(chunk, 0, read);
            }
        }, "VoiceRecorder-Capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Stops recording.
     * @return WAV-encoded bytes of the captured audio, or empty array on error.
     */
    public byte[] stopAndGetWav() {
        if (!recording) return new byte[0];
        recording = false;
        line.stop();
        line.close();

        try {
            captureThread.join(2000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        byte[] pcm = pcmBuffer.toByteArray();
        return pcmToWav(pcm, FORMAT);
    }

    /**
     * Returns the duration in seconds of a WAV byte array.
     * Works by reading the data sub-chunk size from the WAV header.
     */
    public static double wavDurationSeconds(byte[] wav) {
        if (wav == null || wav.length < 44) return 0;
        // Bytes 40-43: data sub-chunk size (little-endian int)
        int dataSize = ((wav[43] & 0xFF) << 24) | ((wav[42] & 0xFF) << 16)
                     | ((wav[41] & 0xFF) << 8)  |  (wav[40] & 0xFF);
        // Bytes 28-31: byte rate (little-endian int)
        int byteRate = ((wav[31] & 0xFF) << 24) | ((wav[30] & 0xFF) << 16)
                     | ((wav[29] & 0xFF) << 8)  |  (wav[28] & 0xFF);
        return byteRate > 0 ? (double) dataSize / byteRate : 0;
    }

    // ─── WAV encoding ─────────────────────────────────────────────

    /**
     * Wraps raw PCM bytes in a minimal RIFF/WAV container header.
     * The resulting byte[] can be decoded by any WAV player.
     */
    private static byte[] pcmToWav(byte[] pcm, AudioFormat fmt) {
        int sampleRate  = (int) fmt.getSampleRate();
        int channels    = fmt.getChannels();
        int bitsPerSamp = fmt.getSampleSizeInBits();
        int byteRate    = sampleRate * channels * bitsPerSamp / 8;
        int blockAlign  = channels * bitsPerSamp / 8;
        int dataSize    = pcm.length;
        int totalSize   = 36 + dataSize;

        byte[] wav = new byte[44 + dataSize];

        // RIFF header
        wav[0]='R'; wav[1]='I'; wav[2]='F'; wav[3]='F';
        intToLE(wav,  4, totalSize);
        wav[8]='W'; wav[9]='A'; wav[10]='V'; wav[11]='E';

        // fmt sub-chunk
        wav[12]='f'; wav[13]='m'; wav[14]='t'; wav[15]=' ';
        intToLE(wav, 16, 16);            // chunk size
        shortToLE(wav, 20, (short)1);    // PCM = 1
        shortToLE(wav, 22, (short)channels);
        intToLE(wav, 24, sampleRate);
        intToLE(wav, 28, byteRate);
        shortToLE(wav, 32, (short)blockAlign);
        shortToLE(wav, 34, (short)bitsPerSamp);

        // data sub-chunk
        wav[36]='d'; wav[37]='a'; wav[38]='t'; wav[39]='a';
        intToLE(wav, 40, dataSize);
        System.arraycopy(pcm, 0, wav, 44, dataSize);

        return wav;
    }

    private static void intToLE(byte[] b, int off, int v) {
        b[off]   = (byte)(v);
        b[off+1] = (byte)(v >> 8);
        b[off+2] = (byte)(v >> 16);
        b[off+3] = (byte)(v >> 24);
    }

    private static void shortToLE(byte[] b, int off, short v) {
        b[off]   = (byte)(v);
        b[off+1] = (byte)(v >> 8);
    }
}
