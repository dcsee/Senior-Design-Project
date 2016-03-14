package atlantis.com.atlantis.fragments;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

import atlantis.com.atlantis.R;

/**
 * Created by Andrew on 5/24/2015.
 * Fragment for selecting preferences from XML file.
 * Invoked from Settings Activity
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
