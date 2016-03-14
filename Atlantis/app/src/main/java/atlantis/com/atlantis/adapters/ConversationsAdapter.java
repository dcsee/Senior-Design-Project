package atlantis.com.atlantis.adapters;

import android.content.Intent;
import android.os.Build;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.ConversationActivity;
import atlantis.com.atlantis.outlines.RoundClipOutlineProvider;
import atlantis.com.atlantis.utils.ContactUtils;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.model.Conversation;
import atlantis.com.model.Notebook;

/**
 * Adapter for showing list of conversations using the default conversation view
 * Created by ricmatsui on 2/8/2015.
 */
public class ConversationsAdapter  extends BasicAdapter {
    private static int selected;

    private final List<Conversation> mConversations;
    private RoundClipOutlineProvider mRoundClipOutlineProvider = null;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conversation_view, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Conversation conversation = mConversations.get(position);

        ((TextView) holder.getView().findViewById(R.id.conversation_name_text))
                .setText(conversation.getName());
        ((TextView)holder.getView().findViewById(R.id.conversation_message))
                .setText(conversation.getDescription());

        ImageView conversationPersonThumbnail = (ImageView) holder.getView().findViewById(R.id.conversation_person_thumbnail);
        Iterator<Notebook> notebooks = conversation.getNotebooks().iterator();
        if(notebooks.hasNext()) {
            ContactUtils.setContactImageViewForLookupKey(holder.getView().getContext(),
                    notebooks.next().getContact().getLookupKey(),
                    conversationPersonThumbnail);
        } else {
            conversationPersonThumbnail.setImageDrawable(holder.getView()
                    .getResources().getDrawable(R.drawable.person_icon));
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(mRoundClipOutlineProvider == null) {
                mRoundClipOutlineProvider = new RoundClipOutlineProvider();
            }
            mRoundClipOutlineProvider.setClipOutlineProvider(conversationPersonThumbnail);
        }

        holder.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent conversationIntent = new Intent(v.getContext(), ConversationActivity.class);
                conversationIntent.putExtra(
                        Extras.CONVERSATION_ID,
                        conversation.getId());
                v.getContext().startActivity(conversationIntent);
            }
        });
        holder.getView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                selected = mConversations.get(position).getId();
                menu.setHeaderTitle(R.string.conversation_context_menu_title);
                menu.add(0, v.getId(), 0, R.string.context_menu_delete);
            }
        });
    }

    public static int getSelectedId(){
        return selected;
    }

    @Override
    public int getItemCount() {
        return mConversations.size();
    }

    public void addConversation(Conversation conversation) {
        mConversations.add(conversation);
        notifyDataSetChanged();
    }

    public void removeConversationWithId(int conversationId) {
        int index = 0;
        for(Iterator<Conversation> iterator = mConversations.iterator(); iterator.hasNext();) {
            Conversation current = iterator.next();
            if(current.getId() == conversationId) {
                iterator.remove();
                break;
            }
            index++;
        }

        notifyItemRemoved(index);
    }

    public ConversationsAdapter(List<Conversation> conversations) {
        mConversations = conversations;
    }
}

