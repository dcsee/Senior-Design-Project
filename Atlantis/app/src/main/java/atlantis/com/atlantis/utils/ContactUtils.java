package atlantis.com.atlantis.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.ImageView;

import java.io.InputStream;

import atlantis.com.atlantis.R;

/**
 * Created by ricardo on 5/4/15.
 */
public class ContactUtils {

    /**
     * Sets the image on the image view for the thumbnail of a contact. If not found, use the default image
     * @param context The mParentActivity loading from
     * @param lookupKey The lookup key for the contact
     * @param imageView The image view to set the image for
     */
    public static void setContactImageViewForLookupKey(Context context, String lookupKey, ImageView imageView) {
        if(lookupKey != null && lookupKey != "") {
            ContentResolver contentResolver = context.getContentResolver();
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(contentResolver,
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey));
            imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream));
        } else {
            imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.person_icon));
        }
    }

    /**
     * Gets the lookup key associated with the user's profile
     * @param context The mParentActivity to load in
     * @return The lookup key or null
     */
    public static String getSelfLookupKey(Context context) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI,
                new String[] { ContactsContract.Profile.LOOKUP_KEY }, null, null, null);
        String lookupKey = null;
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.LOOKUP_KEY));
        }
        cursor.close();
        return lookupKey;
    }

}
