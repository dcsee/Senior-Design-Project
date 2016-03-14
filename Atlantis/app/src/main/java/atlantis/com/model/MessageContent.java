package atlantis.com.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import atlantis.com.atlantis.R;


/**
 * Text and image holder that serializes and deserializes from a byte array
 * Created by ricardo on 5/3/15.
 */
public class MessageContent {

    /*
     * Content types
     */
    public static final byte CONTENT_TYPE_ACK = 0;
    public static final byte CONTENT_TYPE_TEXT = 1;
    public static final byte CONTENT_TYPE_IMAGE = 2;

    private static final Pair<Byte, Byte> CONTENT_ENCODING_BASE64_ACK    = Pair.create((byte)0, CONTENT_TYPE_ACK);
    private static final Pair<Byte, Byte> CONTENT_ENCODING_PLAIN_TEXT    = Pair.create((byte)1, CONTENT_TYPE_TEXT);
    private static final Pair<Byte, Byte> CONTENT_ENCODING_DEFLATED_TEXT = Pair.create((byte)2, CONTENT_TYPE_TEXT);
    private static final Pair<Byte, Byte> CONTENT_ENCODING_JPEG          = Pair.create((byte)3, CONTENT_TYPE_IMAGE);

    private static final List<Pair<Byte, Byte>> ENCODINGS = Arrays.asList(
            CONTENT_ENCODING_BASE64_ACK,
            CONTENT_ENCODING_PLAIN_TEXT,
            CONTENT_ENCODING_DEFLATED_TEXT,
            CONTENT_ENCODING_JPEG);

    /**
     * The encoding of content
     */
    private Pair<Byte, Byte> mEncoding;

    /**
     * The serialized content
     */
    private byte[] mContent;

    /**
     * The quality to send images at
     */
    private static final int DEFAULT_IMAGE_MAX_DIMENSION = 1080;

    /**
     * Initialize message content with text
     * @param content The text to send
     */
    public MessageContent(String content) throws IOException {
        byte[] contentBytes = content.getBytes();

        // Compress the content
        ByteArrayOutputStream textDataOutputStream = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(textDataOutputStream);
        deflaterOutputStream.write(contentBytes);
        deflaterOutputStream.close();
        byte[] compressedContent = textDataOutputStream.toByteArray();

        // If compression resulted in a bigger message, then send the uncompressed version
        if(contentBytes.length <= compressedContent.length) {
            mEncoding = CONTENT_ENCODING_PLAIN_TEXT;
            mContent = contentBytes;
        } else {
            mEncoding = CONTENT_ENCODING_DEFLATED_TEXT;
            mContent = compressedContent;
        }
    }

    /**
     * Initialize message content with bitmap
     * @param content The image to send
     * @throws IOException Thrown when problem with compression
     */
    public MessageContent(Bitmap content, Context context) throws IOException, NumberFormatException {
        mEncoding = CONTENT_ENCODING_JPEG;

        int scaledWidth = content.getWidth();
        int scaledHeight = content.getHeight();
        if(content.getWidth() > DEFAULT_IMAGE_MAX_DIMENSION || content.getHeight() > DEFAULT_IMAGE_MAX_DIMENSION) {
            if (content.getWidth() >= content.getHeight()) {
                scaledWidth = DEFAULT_IMAGE_MAX_DIMENSION;
                scaledHeight = (int) (content.getHeight() / (double) content.getWidth()
                        * DEFAULT_IMAGE_MAX_DIMENSION);
            } else {
                scaledHeight = DEFAULT_IMAGE_MAX_DIMENSION;
                scaledWidth = (int) (content.getWidth() / (double) content.getHeight()
                        * DEFAULT_IMAGE_MAX_DIMENSION);
            }
        }

        ByteArrayOutputStream imageDataOutputStream = new ByteArrayOutputStream();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(content, scaledWidth, scaledHeight, true);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String imageQualityPreference = sharedPreferences.getString(context.getResources().getString(R.string.picture_quality_key), context.getResources().getString(R.string.picture_quality_default));
        int imageQuality = Integer.parseInt(imageQualityPreference);
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, imageDataOutputStream);
        mContent = imageDataOutputStream.toByteArray();
        imageDataOutputStream.close();
    }

    public MessageContent(CipherAddress address) {
        mEncoding = CONTENT_ENCODING_BASE64_ACK;

        mContent = address.getBytes();
    }

    /**
     * Initializes from a byte array in the following format
     * Byte 0: Header byte indicating type
     * Remaining bytes: Content
     * @param serializedContent The byte array in the described format
     */
    public MessageContent(byte[] serializedContent) {
        if (serializedContent.length == 0) {
            mEncoding = CONTENT_ENCODING_PLAIN_TEXT;
            mContent = new byte[0];
        } else {
            mEncoding = ENCODINGS.get(serializedContent[0]);
            mContent = new byte[serializedContent.length - 1];
            System.arraycopy(serializedContent, 1, mContent, 0, mContent.length);
        }
    }

    /**
     * Get the content type of the message
     * @return mType
     */
    public int getContentType() {
        return mEncoding.second;
    }

    public CipherAddress getAckContent() {
        return new CipherAddress(mContent);
    }

    /**
     * Get the text content of the message if one exists
     * @return The text of the message or null
     */
    public String getStringContent() throws IOException {
        if (mEncoding == CONTENT_ENCODING_PLAIN_TEXT) {
            return new String(mContent);
        } else if(mEncoding == CONTENT_ENCODING_DEFLATED_TEXT) {
            ByteArrayOutputStream textDataOutputStream = new ByteArrayOutputStream();
            InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(textDataOutputStream);
            inflaterOutputStream.write(mContent);
            inflaterOutputStream.close();
            return new String(textDataOutputStream.toByteArray());
        } else {
            return null;
        }
    }

    /**
     * Get the image content of the message if one exists
     * @return The image in the message or null
     * @throws IOException Thrown when there is a problem decoding
     */
    public Bitmap getImageContent() throws IOException {
        if (mEncoding == CONTENT_ENCODING_JPEG) {
            ByteArrayInputStream imageDataInputStream = new ByteArrayInputStream(mContent);
            Bitmap bitmap = BitmapFactory.decodeStream(imageDataInputStream);
            imageDataInputStream.close();
            return bitmap;
        } else {
            return null;
        }
    }

    /**
     * Serialize message content to byte array for sending and database in the following format
     * Byte 0: Message type
     * Remaining bytes: Message content
     * @return The serialized byte array
     */
    public byte[] toByteArray() {
        byte[] serializedContent = new byte[mContent.length + 1];
        serializedContent[0] = mEncoding.first;
        System.arraycopy(mContent, 0, serializedContent, 1, mContent.length);
        return serializedContent;
    }
}
