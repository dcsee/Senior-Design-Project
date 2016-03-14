package atlantis.com.atlantis.activities;

import android.os.Bundle;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.fragments.SettingsFragment;

/**
 * Created by Andrew on 2/28/2015.
 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new SettingsFragment())
                    .commit();
        }
    }
}
