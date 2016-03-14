package atlantis.com.atlantis.activities.test;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.MainActivity;
import atlantis.com.atlantis.activities.NewConversationActivity;

/**
 * Created by jvronsky on 4/12/15.
 */
public class TestMainActivity extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final long TIME_TO_WAIT_BETWEEN_ACTIVITY = 5000;

    private Activity mMainActivity;
    private ImageButton mNewConversationButton;

    public TestMainActivity() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        setActivityInitialTouchMode(true);
        mMainActivity = getActivity();
        mNewConversationButton = (ImageButton) mMainActivity.findViewById(R.id.fab_add);
    }

    @MediumTest
    public void testCreateNewConversationButton() throws Exception {
        final View decorView = mMainActivity.getWindow().getDecorView();

        ViewAsserts.assertOnScreen(decorView, mNewConversationButton);

        final ViewGroup.LayoutParams layoutParams = mNewConversationButton.getLayoutParams();
        float density = mMainActivity.getResources().getDisplayMetrics().density;
        assertNotNull(layoutParams);
        assertEquals(56.0, layoutParams.width / density, 0.01);
        assertEquals(56.0, layoutParams.height / density, 0.01);

        ActivityMonitor reciveverActivityMonitor = getInstrumentation().addMonitor(
                NewConversationActivity.class.getName(), null, false);
        performClick(mNewConversationButton);
        NewConversationActivity newConversationActivity = (NewConversationActivity)
                reciveverActivityMonitor.waitForActivityWithTimeout(TIME_TO_WAIT_BETWEEN_ACTIVITY);
        assertNotNull("NewMessageActivity is null", newConversationActivity);
        assertEquals("Monitor has not been called", 1, reciveverActivityMonitor.getHits());
        getInstrumentation().removeMonitor(reciveverActivityMonitor);
    }




    private void performClick(final View v) {
        mMainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                v.performClick();
            }
        });
    }
}
