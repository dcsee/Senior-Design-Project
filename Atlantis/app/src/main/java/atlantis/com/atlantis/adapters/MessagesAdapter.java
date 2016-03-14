package atlantis.com.atlantis.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.MessageActivity;
import atlantis.com.atlantis.outlines.RoundClipOutlineProvider;
import atlantis.com.atlantis.utils.ContactUtils;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Message;
import atlantis.com.model.MessageContent;
import atlantis.com.model.Person;

/**
 * Adapter for showing messages in a conversation
 * Created by ricardo on 2/22/15.
 */
public class MessagesAdapter extends BasicAdapter {

    private final List<Message> mMessageList;
    private RoundClipOutlineProvider mRoundClipOutlineProvider = null;
    private final Person mSelf;
    private final Context mParentContext;

    private static int selected;

    private static final int LEFT_MESSAGE = 0;
    private static final int RIGHT_MESSAGE = 1;

    public MessagesAdapter(List<Message> mMessageList, Person self, Context context) {
        this.mMessageList = mMessageList;
        this.mSelf = self;
        this.mParentContext = context;
    }

    @Override
    public int getItemViewType(int position) {
        return mMessageList.get(position).getSender().getId() == mSelf.getId() ? RIGHT_MESSAGE : LEFT_MESSAGE;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if(viewType == LEFT_MESSAGE) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.left_message_view, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.right_message_view, parent, false);
        }

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Message message = mMessageList.get(position);

        TextView textView = ((TextView)holder.getView().findViewById(R.id.message_content_text));
        ImageView imageView = ((ImageView)holder.getView().findViewById(R.id.message_content_image));

        MessageContent messageContent = message.getSerializedContent();

        if(messageContent.getContentType() == MessageContent.CONTENT_TYPE_TEXT) {
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);

            try {
                textView.setText(messageContent.getStringContent());
            } catch (IOException e) {
                e.printStackTrace();

                textView.setText(holder.getView().getResources().getString(R.string.error_loading_text));
            }
            imageView.setImageBitmap(null);
        } else if(messageContent.getContentType() == MessageContent.CONTENT_TYPE_IMAGE) {
            try {
                textView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);

                textView.setText(null);
                imageView.setImageBitmap(messageContent.getImageContent());
            } catch (IOException e) {
                e.printStackTrace();

                textView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);

                textView.setText(holder.getView().getResources().getString(R.string.error_loading_image));
            }
        }

        if(getItemViewType(position) == LEFT_MESSAGE){
            ((TextView)holder.getView().findViewById(R.id.message_person_name_text))
                .setText(message.getSender().getDisplay());
        } else {
            holder.getView().findViewById(R.id.message_delivered_indicator).setVisibility(
                    message.isDelivered() ? View.VISIBLE : View.INVISIBLE);
        }

        ImageView senderImageView = ((ImageView) holder.getView().findViewById(R.id.message_person_thumbnail));
        ContactUtils.setContactImageViewForLookupKey(holder.getView().getContext(),
                message.getSender().getLookupKey(),
                senderImageView);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(mRoundClipOutlineProvider == null) {
                mRoundClipOutlineProvider = new RoundClipOutlineProvider();
            }
            mRoundClipOutlineProvider.setClipOutlineProvider(senderImageView);
        }

        DateFormat dateFormat = getDateFormat(message.getTimestamp());
        String messageTimeStamp = dateFormat.format(message.getTimestamp());
        ((TextView)holder.getView().findViewById(R.id.message_timestamp_text))
                .setText(messageTimeStamp);
        holder.getView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                selected = message.getId();
                menu.setHeaderTitle(R.string.message_context_menu_title);
                menu.add(0, v.getId(), 0, R.string.context_menu_delete);
            }
        });
        holder.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent messageIntent = new Intent(v.getContext(), MessageActivity.class);
                messageIntent.putExtra(
                        Extras.MESSAGE_ID,
                        message.getId());
                v.getContext().startActivity(messageIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public void addMessage(Message message) {
        mMessageList.add(message);
        notifyDataSetChanged();
    }

    public void removeMessageWithId(int messageId) {
        int index = 0;
        for(Iterator<Message> iterator = mMessageList.iterator(); iterator.hasNext();) {
            Message current = iterator.next();
            if (current.getId() == messageId) {
                iterator.remove();
                break;
            }
            index++;
        }

        notifyItemRemoved(index);
    }

    /**
     * Update the message with the ID
     * @param messageId The id of the message
     * @throws SQLException
     */
    public void updateMessageWithId(int messageId) throws SQLException {
        for(Message message : mMessageList) {
            // If the OTP is part of this notebook
            if(message.getId() == messageId) {
                // Refresh data and notify
                DatabaseManager manager = DatabaseManager.getInstance(mParentContext);
                manager.refreshMessage(message);
                int index = mMessageList.indexOf(message);
                mMessageList.set(index, message);
                notifyItemChanged(index);
            }
        }
    }

    public static int getSelectedId(){
        return selected;
    }

    private DateFormat getDateFormat(Date messageTimestamp) {
        Calendar sentTime = Calendar.getInstance();
        sentTime.setTime(messageTimestamp);
        Calendar currentTime = Calendar.getInstance();
        sentTime.add(Calendar.DATE, 1);
        sentTime.set(Calendar.HOUR, 0);
        sentTime.set(Calendar.MINUTE, 0);
        if(sentTime.after(currentTime)) {
            return DateFormat.getTimeInstance(DateFormat.SHORT);
        } else {
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        }
    }

}
