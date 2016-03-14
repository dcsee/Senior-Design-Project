package atlantis.com.atlantis.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.fragments.MessagesFragment;
import atlantis.com.atlantis.fragments.NotebooksFragment;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Conversation;

/**
 * Activity for syncing notebooks in a conversation
 */
public class NotebooksActivity extends BaseActivity implements MessagesFragment.ConversationHolder {

    private Conversation mConversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notebooks);

        DatabaseManager manager = DatabaseManager.getInstance(this);
        mConversation = manager.getConversationWithId(
                getIntent().getIntExtra(Extras.CONVERSATION_ID, 0));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new NotebooksFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_notebooks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Conversation getConversation() {
        return mConversation;
    }
}
