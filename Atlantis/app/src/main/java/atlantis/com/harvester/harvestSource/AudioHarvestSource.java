package atlantis.com.harvester.harvestSource;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.Arrays;

/**
 * Created by jvronsky on 5/23/15.
 * AudioHarvestSource collection entropy from audio.
 */
public class AudioHarvestSource extends Thread implements HarvestSource {

    private static final String TAG = "AUDIO_COLLECTOR";
    // Timeout used between microphone sampling.
    private static final int TIMEOUT = 100;
    // Sample rate of 44.1k Hz.
    private static final int SAMPLE_RATE = 44100;
    // Keep collecting audio data.
    private boolean mKeepAlive;
    // Available bytes to give the harvester.
    private int mAvailableBytes;
    // Buffer size for audio results.
    private int mBufferSize;

    private byte[] mBuffer;
    private AudioRecord mMicrophone;

    public AudioHarvestSource() throws HarvestSourceNotSupportedException {
        int minBufferSize = 0;
        mKeepAlive = false;
        mAvailableBytes = 0;
        minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        // Check for errors.
        if(minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
            throw new HarvestSourceNotSupportedException("Audio is not supported on device");
        }
        this.mBufferSize = minBufferSize;
        mBuffer = new byte[mBufferSize];
        mMicrophone = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
        // AudioRecord was not initialized.
        if(mMicrophone == null) {
            throw new HarvestSourceNotSupportedException("Audio is not supported on device");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getBytesFromSource() {
        byte[] result;
        if(mAvailableBytes > 0) {
            result = Arrays.copyOfRange(mBuffer, 0, mAvailableBytes);
        } else {
            result = new byte[] {};
        }
        mAvailableBytes = 0;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startCollecting() {
        this.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopCollectingData() {
        mKeepAlive = false;
        mMicrophone.stop();
        mMicrophone.release();
    }

    @Override
    public void run() {
        super.run();
        mKeepAlive = true;
        mMicrophone.startRecording();
        while(mKeepAlive) {
            mAvailableBytes = mMicrophone.read(mBuffer, 0, mBufferSize);
            // Short sleep for performance.
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
