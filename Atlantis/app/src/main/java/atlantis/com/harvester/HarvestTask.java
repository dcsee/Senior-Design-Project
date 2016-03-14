package atlantis.com.harvester;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


import atlantis.com.harvester.harvestSource.SensorEntropySource;

import atlantis.com.atlantis.R;

import atlantis.com.harvester.harvestSource.AudioHarvestSource;
import atlantis.com.harvester.harvestSource.HarvestSource;
import atlantis.com.harvester.harvestSource.HarvestSourceNotSupportedException;

/**
 * Harvest task does the harvesting in the background.
 */
public class HarvestTask extends Thread {

    public static final int STATUS_RUNNING = 0x02;
    public static final int STATUS_PRE_EXECUTE = 0x03;
    public static final int STATUS_POST_EXECUTE = 0x04;
    public static final int STATUS_CANCELLED = 0x05;

    private static final String TAG = "HarvestTask";

    // Timeout between data processing.
    private static final int TIMEOUT = 75;

    // Harvesting Sources.
    private List<HarvestSource> mHarvestSources;
    private OTPGenerator mOTPGenerator;
    private boolean mReadingData;
    private boolean mHarvesting;
    private Service mParentService;
    private int mStatus;
    private boolean mIsCancelled;
    private int mHarvesterMax;
    private final Context mContext;

    public HarvestTask(Service service, Context baseContext) throws FileNotFoundException {
        mHarvestSources = new ArrayList<>();
        mOTPGenerator = new OTPGenerator(baseContext);
        mReadingData = false;
        mHarvesting = false;
        mParentService = (Service) service;
        mContext = mParentService.getBaseContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        loadHarvesterMax(sharedPreferences);
    }



    @Override
    public void run() {
        super.run();
        onPreExecute();
        doInBackground();
        if(!mIsCancelled) {
            onPostExecute(null);
        } else {
            onCancelled(null);
        }
    }

    protected Void doInBackground(Void... params) {
        mStatus = STATUS_RUNNING;
        mHarvesting = true;
        SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(mContext.getResources().getString(R.string.harvester_key))) {
                    loadHarvesterMax(sharedPreferences);
                }
            }
        };
        try {
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
            while ((mOTPGenerator.getAvailableOTP() < mHarvesterMax) && (!mIsCancelled)) {
                try {
                    if (!mReadingData) {
                        byte[] bytesCollected = collectDataFromSources();
                        if(bytesCollected != null && bytesCollected.length > 0) {
                            mOTPGenerator.addRawData(bytesCollected, bytesCollected.length);
                        }
                        Thread.sleep(TIMEOUT);
                    }
                } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException
                        | InvalidAlgorithmParameterException | IllegalBlockSizeException
                        | BadPaddingException | InvalidKeyException | InvalidKeySpecException
                        | InterruptedException e) {
                    e.printStackTrace();
                    mParentService.stopSelf();
                }
            }
        } finally {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }

        return null;
    }

    private void loadHarvesterMax(SharedPreferences sharedPreferences) {
        String harvestMaxPreference = sharedPreferences.getString(mContext.getResources().getString(R.string.harvester_key),
                mContext.getResources().getString(R.string.harvester_max_default));
        mHarvesterMax = Integer.parseInt(harvestMaxPreference);
    }

    /**
     * Collects data from the resources available.
     * repeat pattern to the biggest source.
     * @return bytes collected from the resouces.
     */
    private byte[] collectDataFromSources() {
        int maxBytesCollected = 0;
        final List<byte[]> collectedByteList = new ArrayList<>(mHarvestSources.size());
        int indexOfLargestSource = -1;
        byte[] collectedBytes;
        for(int i = 0; i < mHarvestSources.size(); i++) {
            byte[] bytesFromSource = mHarvestSources.get(i).getBytesFromSource();
            if(bytesFromSource != null) {
                collectedByteList.add(bytesFromSource);
                if (bytesFromSource.length > maxBytesCollected) {
                    maxBytesCollected = bytesFromSource.length;
                    indexOfLargestSource = i;
                }
            }
        }
        if(indexOfLargestSource >= 0) {
            collectedBytes = collectedByteList.remove(indexOfLargestSource);
            for(byte[] bytes : collectedByteList) {
                if(bytes != null && bytes.length > 0) {
                    for (int i = 0; i < collectedBytes.length; i++) {
                        collectedBytes[i] = (byte) (collectedBytes[i] ^ bytes[i % bytes.length]);
                    }
                }
            }
            return collectedBytes;
        } else {
            // There was an error return empty array
            return new byte[] {};
        }
    }

    protected void onPreExecute() {
        mStatus = STATUS_PRE_EXECUTE;
        // If source is not supported just continue to next source.
        try {
            mHarvestSources.add(new AudioHarvestSource());
        } catch (HarvestSourceNotSupportedException e) {
            e.printStackTrace();
        }

        try {
            mHarvestSources.add(new SensorEntropySource(mParentService, Sensor.TYPE_ACCELEROMETER));
        } catch (HarvestSourceNotSupportedException e) {
            e.printStackTrace();
        }

        // If none of the sources worked notify that this device can't collect entropy.
        if(mHarvestSources.size() == 0) {
            try {
                throw new NoHarvestingSourcesFound("This device doesn't support any harvesting source");
            } catch (NoHarvestingSourcesFound noHarvestingSourcesFound) {
                // Was not sure what to do yet, if it can't find a source we have a problem.
                noHarvestingSourcesFound.printStackTrace();
            }
        }
        // Start collecting all the data.
        for(HarvestSource source : mHarvestSources) {
            source.startCollecting();
        }
    }

    protected void onPostExecute(Void aVoid) {
        mStatus = STATUS_POST_EXECUTE;
        try {
            stopTask();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void onCancelled(Void aVoid) {
        mStatus = STATUS_CANCELLED;
        try {
            stopTask();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kill() {
        this.cancel(true);
    }

    public void execute() {
        mIsCancelled = false;
        this.start();
    }

    public int getStatus() {
        return mStatus;
    }

    /**
     * Get OTP from the entropy collected.
     * @param size of entropy wanted
     * @param buffer to put the entropy in
     * @return the size of OTP that was actually found.
     * @throws IOException
     */
    public int getOTPFromEntropy(int size, byte[] buffer) throws IOException {
        synchronized (this) {
            mReadingData = true;
            int bytesRead = mOTPGenerator.getOTP(size, buffer);
            mReadingData = false;
            return bytesRead;
        }
    }

    public void cancel(boolean cancel) {
        if(cancel) {
            mIsCancelled = true;
        }
    }

    public long getAmountOTP() {
        long availableOTP = mOTPGenerator.getAvailableOTP();
        return availableOTP;
    }

    /**
     * Stop and clean up task.
     * @throws IOException
     */
    private void stopTask() throws IOException {
        turnOffSource();
        mHarvesting = false;
    }

    /**
     * Stop all resources from producing data.
     */
    private void turnOffSource() {
        for(HarvestSource source : mHarvestSources) {
            source.stopCollectingData();
        }
    }

    public boolean isHarvesting() {
        return mHarvesting;
    }
}
