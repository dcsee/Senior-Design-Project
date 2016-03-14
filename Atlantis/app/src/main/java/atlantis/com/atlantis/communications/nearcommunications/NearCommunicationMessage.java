package atlantis.com.atlantis.communications.nearcommunications;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import atlantis.com.atlantis.utils.BytesUtils;
import atlantis.com.atlantis.utils.HashingUtils;

/**
 * Created by jvronsky on 3/3/15.
 * This class is used for generating messages used for near communications.
 * All methods will use the same protocol format.
 */
public class NearCommunicationMessage {

    // Length of message header.
    public static final int HEADER_LENGTH = 47;
    /*
    The header contain 3 important pieces of data: status code, message length and data checksum
    This is the length that these three take in the header. These values are checksumed to ensure
    integrity.
     */
    private static final int HEADER_DATA_LENGTH = 25;
    // Number of bytes of data sent has to be a 4 byte number.
    public static final int MAX_LENGTH_OF_DATA_IN_BYTES = 4;
    // Tokens to go in the head and tail of header.
    private static final byte HEADER_HEAD_TOKEN = 0x01;
    private static final byte HEADER_TAIL_TOKEN = 0x02;
    // Indexes of major components in the header.
    private static final int HEADER_HEAD_INDEX = 0;
    private static final int STATUS_CODE_INDEX = 1;
    // DATA_LENGTH refers to the part of the header that describes the length of the data attached.
    private static final int DATA_LENGTH_START_INDEX = 2;
    private static final int DATA_LENGTH_END_INDEX = 5;
    private static final int DATA_CHECKSUM_START_INDEX = 6;
    private static final int DATA_CHECKSUM_END_INDEX= 25;
    private static final int HEADER_CHECKSUM_START_INDEX = 26;
    private static final int HEADER_CHECKSUM_END_INDEX = 45;
    private static final int HEADER_TAIL_INDEX = 46;

    // Messages status codes.
    public static final int INIT_CONNECTION = 0x01;
    public static final int READY = 0x02;
    public static final int DATA_PIECE = 0x03;
    public static final int RECEIVED_SUCCESSFULLY = 0x04;
    public static final int RESEND = 0x05;
    public static final int STORY_RECEIVED = 0x06;
    public static final int WAITING_FOR_YOUR_REQUEST = 0x07;
    public static final int DONE_SENDING = 0x08;
    /**
     * MessageHeader class used for easier access to variables after parsing a message.
     */
    public class MessageHeader {
        int statusCode;
        int dataLength;
        byte[] dataChecksum;
        byte[] headerChecksum;
        boolean isHeaderIntegrityValid;
        final boolean validHeader;

        public MessageHeader() {
            this.validHeader = false;
        }

        /**
         * Constructor for message header class.
         * @param statusCode of header
         * @param dataLength in the body
         * @param dataChecksum
         * @param headerChecksum
         */
        public MessageHeader(int statusCode, int dataLength, byte[] dataChecksum,
                             byte[] headerChecksum) {
            this.statusCode = statusCode;
            this.dataLength = dataLength;
            this.dataChecksum = dataChecksum;
            this.headerChecksum = headerChecksum;
            this.isHeaderIntegrityValid = checkDataIntegrity(
                    generateHeaderData(statusCode, dataLength, dataChecksum), headerChecksum);
            this.validHeader = isHeaderIntegrityValid;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public int getDataLength() {
            return dataLength;
        }

        public byte[] getDataChecksum() {
            return dataChecksum;
        }

        public byte[] getHeaderChecksum() {
            return headerChecksum;
        }

        public boolean isHeaderIntegrityValid() {
            return isHeaderIntegrityValid;
        }

        public boolean isValidHeader() {
            return validHeader;
        }
    }

    /**
     * Parses header in MessageHeader object for easier access.
     * @param header to be parsed
     * @return MessaeHeader object with the header values
     */
    public MessageHeader parseHeader(byte[] header) {
        if(isHeaderValidSize(header)) {
            int statusCode = header[STATUS_CODE_INDEX];
            int dataLength = BytesUtils.byteArrayToInt(
                    Arrays.copyOfRange(header, DATA_LENGTH_START_INDEX, DATA_LENGTH_END_INDEX + 1));
            byte[] dataChecksum = Arrays.copyOfRange(
                    header, DATA_CHECKSUM_START_INDEX, DATA_CHECKSUM_END_INDEX + 1);
            byte[] headerChecksum = Arrays.copyOfRange(
                    header, HEADER_CHECKSUM_START_INDEX, HEADER_CHECKSUM_END_INDEX + 1);
            return new MessageHeader(statusCode, dataLength, dataChecksum, headerChecksum);
        }
        return new MessageHeader();
    }

    public static boolean isEmptyMessageCode(int code) {
        return (code != DATA_PIECE && code != INIT_CONNECTION && code != READY);
    }

    private static boolean isHeaderValidSize(byte[] header) {
        //noinspection SimplifiableIfStatement
        if(header == null || header.length != HEADER_LENGTH) {
            return false;
        } else {
            return header[HEADER_HEAD_INDEX] == HEADER_HEAD_TOKEN
                    && header[HEADER_TAIL_INDEX] == HEADER_TAIL_TOKEN;
        }
    }

    /**
     * Build init connection message.
     * @param storySize of the story to be sent across
     * @return message of init connection with the story length as body in byte[] format
     */
    public static byte[] buildInitConnectionMessage(Integer storySize) {
        return buildMessage(INIT_CONNECTION, storySize);
    }

    /**
     * Builds the ready message.
     * @return ready message in byte[] format
     */
    public static byte[] buildReadyMessage(int nextBytesIndex) {
        return buildMessage(READY, nextBytesIndex);
    }

    /**
     * Creates a message with a data piece.
     * @param data to be put in the message.
     * @return message with data in byte[] format
     */
    public static byte[] buildDataPieceMessage(byte[] data) {
        return buildMessage(DATA_PIECE, data);
    }

    /**
     * Builds built successfully message.
     * @return message of received successfully in byte[] format.
     */
    public static byte[] buildReceivedSuccesfullyMessage() {
        return buildEmptyMessage(RECEIVED_SUCCESSFULLY);
    }

    public static byte[] buildDoneSendingMessage() {
        return buildEmptyMessage(DONE_SENDING);
    }

    /**
     * Builds resend message.
     * @return message of resend in byte[] format
     */
    public static byte[] buildResendMessage() {
        return buildEmptyMessage(RESEND);
    }

    /**
     * Builds story received message.
     * @return message of story received in byte[] format
     */
    public static byte[] buildStoryReceived() {
        return buildEmptyMessage(STORY_RECEIVED);
    }

    public static byte[] buildWaitingForYourRequest() {
        return buildEmptyMessage(WAITING_FOR_YOUR_REQUEST);
    }

    /**
     * Checks whether data matches its checksum.
     * @param data to check
     * @param checksum to use for comparison
     * @return true if data matches the checksum false otherwise
     */
    public static boolean checkDataIntegrity(byte[] data, byte[] checksum) {
        return Arrays.equals(generateChecksum(data), checksum);
    }

    public static String messageCodeToText(int messageCode) {
        switch (messageCode) {
            case INIT_CONNECTION: return "INIT_CONNECTION";
            case READY: return "READY";
            case DATA_PIECE: return "DATA_PIECE";
            case RECEIVED_SUCCESSFULLY: return "RECEIVED_SUCCESSFULLY";
            case RESEND: return "RESEND";
            case STORY_RECEIVED: return "STORY_RECEIVED";
            case WAITING_FOR_YOUR_REQUEST: return "WAITING_FOR_YOUR_REQUEST";
            case DONE_SENDING: return "DONE_SENDING";
            default: return "UNKNOWN";
        }
    }

    /**
     * Creates a message without a body, The checksum of the data is initiated to be all 0's.
     * @param statusCode of the message
     * @return a message contianing only a header in byte[] format
     */
    private static byte[] buildEmptyMessage(Integer statusCode) {
        byte[] dataChecksum  = new byte[HashingUtils.CHECKSUM_LENGTH_IN_BYTES];
        for(int i = 0; i < HashingUtils.CHECKSUM_LENGTH_IN_BYTES; i++) {
            dataChecksum[i] = 0x00;
        }
        return buildHeader(statusCode, 0, dataChecksum);
    }

    /**
     * Creates message with the data being an integer value.
     * @param statusCode of the message
     * @param integerDataValue the integer to be sent across
     * @return message of byte[] with an integer in the datsa
     */
    private static byte[] buildMessage(int statusCode, Integer integerDataValue) {
        return buildMessage(statusCode, BytesUtils.intToByteArray(integerDataValue));
    }

    /**
     * Builds a message to send over communication channel.
     * @param statusCode to attach to the message.
     * @param data data to attach to the message.
     * @return the message in byte[] format.
     */
    private static byte[] buildMessage(Integer statusCode, byte[] data) {
        ByteBuffer message = ByteBuffer.allocate(HEADER_LENGTH + data.length);
        message.put(buildHeader(statusCode, data.length, generateChecksum(data)));
        message.put(data);
        return message.array();
    }

    /**
     * Builds header to the message.
     * @param statusCode of the message
     * @param dataLength the length of the data to be sent
     * @param dataChecksum the checksum generated for the data
     * @return header in byte[] format
     */
    private static byte[] buildHeader(Integer statusCode, Integer dataLength, byte[] dataChecksum) {
        ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH);
        byte[] headerData = generateHeaderData(statusCode, dataLength, dataChecksum);
        header.put(HEADER_HEAD_TOKEN);
        header.put(headerData);
        header.put(generateChecksum(headerData));
        header.put(HEADER_TAIL_TOKEN);
        return header.array();
    }

    /**
     * Build the data part of the header with the: status code, data length and data checksum.
     * @param statusCode of the message
     * @param dataLength the length of the data to be sent
     * @param dataChecksum the checksum generated for the data
     * @return the data part of the header
     */
    private static byte[] generateHeaderData(Integer statusCode, Integer dataLength, byte[] dataChecksum) {
        ByteBuffer headerData = ByteBuffer.allocate(HEADER_DATA_LENGTH);
        headerData.put(statusCode.byteValue());
        headerData.put(BytesUtils.intToByteArray(dataLength));
        headerData.put(dataChecksum);
        return headerData.array();
    }

    /**
     * Generates checksum for a byte[].
     * @param data to generate checksum for
     * @return byte[] of checksum
     */
    private static byte[] generateChecksum(byte[] data) {
        try {
            return HashingUtils.generateCheckSum(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            //TODO: Add a valid way to deal with this exception.
            return null;
        }
    }
}
