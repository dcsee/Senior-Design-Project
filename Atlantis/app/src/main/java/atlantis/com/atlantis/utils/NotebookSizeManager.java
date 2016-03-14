package atlantis.com.atlantis.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.fragments.NewConversationFragment;

/**
 * Created by ricardo on 6/2/15.
 * Manager for the UI for selecting a notebook size
 */
public class NotebookSizeManager {

    private static final int TEXT_SCALE = 200;
    private static final int PICTURE_SCALE = 10000;
    private static final int BAR_RESOLUTION = 1000;
    private static final double LOG_BAR_OFFSET = Math.log(PICTURE_SCALE);
    private static final int BASE_10 = 10;

    private final SeekBar mSizeSeekBar;
    private final ProgressBar mAvailableProgressBar;
    private final TextView mTextEstimate;
    private final TextView mPictureEstimate;
    private final Activity mOTPAmountHolderActivity;
    private double mLogAmountAvailable;

    public NotebookSizeManager(SeekBar sizeSeekBar, ProgressBar availableProgressBar,
                               TextView textEstimate, TextView pictureEstimate,
                               Activity mOTPAmountHolderActivity) {
        this.mSizeSeekBar = sizeSeekBar;
        this.mAvailableProgressBar = availableProgressBar;
        this.mTextEstimate = textEstimate;
        this.mPictureEstimate = pictureEstimate;
        this.mOTPAmountHolderActivity = mOTPAmountHolderActivity;

        mTextEstimate.setText("");
        mPictureEstimate.setText("");
    }

    private final SeekBar.OnSeekBarChangeListener mSizeSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if((mSizeSeekBar.getProgress() / (double)BAR_RESOLUTION) > (mLogAmountAvailable - LOG_BAR_OFFSET)) {
                mSizeSeekBar.setProgress((int) ((mLogAmountAvailable - LOG_BAR_OFFSET) * BAR_RESOLUTION));
            }
            updateEstimates();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };


    /**
     * Returns the selected length for the notebook size
     * @return The length
     */
    public int getSelectedLength() {
        return (int) Math.pow(Math.E, (mSizeSeekBar.getProgress() / (double) BAR_RESOLUTION) + LOG_BAR_OFFSET);
    }

    public void updateAmount() {
        long amountAvailable = ((NewConversationFragment.OTPAmountHolder)mOTPAmountHolderActivity).getOTPAmount();
        mLogAmountAvailable = Math.log(amountAvailable);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mOTPAmountHolderActivity);
        String harvestMax = sharedPreferences.getString(mOTPAmountHolderActivity.getResources().getString(R.string.harvester_key),
                mOTPAmountHolderActivity.getResources().getString(R.string.harvester_max_default));

        double logHarvestMax = Math.log(Integer.parseInt(harvestMax));
        int barLogMax = (int) ((logHarvestMax - LOG_BAR_OFFSET) * BAR_RESOLUTION);
        mSizeSeekBar.setMax(barLogMax);
        mAvailableProgressBar.setMax(barLogMax);
        mAvailableProgressBar.setProgress((int) ((mLogAmountAvailable - LOG_BAR_OFFSET) * BAR_RESOLUTION));

        double logHalfAmountAvailable = Math.log(amountAvailable / 2);
        mSizeSeekBar.setProgress((int) ((logHalfAmountAvailable - LOG_BAR_OFFSET) * BAR_RESOLUTION));
        mSizeSeekBar.setOnSeekBarChangeListener(mSizeSeekBarChangeListener);

        updateEstimates();
    }

    private void updateEstimates() {
        int selectedAmount = getSelectedLength();
        mTextEstimate.setText(formatRoundedEstimate(selectedAmount / TEXT_SCALE,
                R.plurals.text_estimate, mOTPAmountHolderActivity));
        mPictureEstimate.setText(formatRoundedEstimate(selectedAmount / PICTURE_SCALE,
                R.plurals.picture_estimate, mOTPAmountHolderActivity));
    }

    private String formatRoundedEstimate(int count, int stringFormatResource, Context context) {
        // Round to one significant digit
        int rounder = (int) Math.pow(BASE_10, (int) Math.log10(count));
        int roundedQuantity = (count / rounder) * rounder;
        return context.getResources().getQuantityString(stringFormatResource,
                roundedQuantity, roundedQuantity);
    }

}
