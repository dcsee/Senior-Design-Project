package atlantis.com.atlantis.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.fragments.ImageMessageFragment;
import atlantis.com.atlantis.fragments.TextMessageFragment;
import atlantis.com.atlantis.fragments.interfaces.MessageContentHolder;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Message;
import atlantis.com.model.MessageContent;

/**
 * Activity for showing the details of a single message
 */
public class MessageActivity extends BaseActivity implements MessageContentHolder {

    private MessageContent mMessageContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        DatabaseManager manager = DatabaseManager.getInstance(this);
        Message message = manager.getMessageWithId(
                getIntent().getIntExtra(Extras.MESSAGE_ID, 0));
        mMessageContent = message.getSerializedContent();

        if(savedInstanceState == null) {
            Fragment fragment = null;
            if(mMessageContent.getContentType() == MessageContent.CONTENT_TYPE_IMAGE) {
                fragment = new ImageMessageFragment();
            } else if(mMessageContent.getContentType() == MessageContent.CONTENT_TYPE_TEXT) {
                fragment = new TextMessageFragment();
            }
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    public MessageContent getMessageContent() {
        return mMessageContent;
    }
}
