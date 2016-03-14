package atlantis.com.atlantis.communications.nearcommunications.test;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import java.util.Arrays;

import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMessage;
import atlantis.com.atlantis.utils.HashingUtils;

/**
 * Created by jvronsky on 4/12/15.
 */
public class TestNearCommunicationMessage extends TestCase{

    private NearCommunicationMessage mNearCommunicationMessage;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mNearCommunicationMessage = new NearCommunicationMessage();
    }

    @SmallTest
    public void testParseHeader() throws Exception {
        String message = "123";
        byte[] dataChecksum = {0x40, (byte) 0xbd, 0x00, 0x15, 0x63, 0x08, 0x5f, (byte) 0xc3, 0x51, 0x65, 0x32,
                (byte) 0x9e, (byte) 0xa1, (byte) 0xff, 0x5c, 0x5e, (byte) 0xcb,
                (byte) 0xdb, (byte) 0xbe, (byte) 0xef};
        byte[] headerChecksum = {0x6f, (byte) 0xe7, 0x73, 0x56, (byte) 0xaf, (byte) 0xa7, (byte) 0xb7, 0x5b, 0x77, 0x28, (byte) 0xd1,
                0x18, 0x48, (byte) 0x90, (byte) 0xcd, 0x4f, 0x5e, 0x3e, (byte) 0xd1, (byte) 0xd1};
        byte[] header = Arrays.copyOfRange(NearCommunicationMessage.buildDataPieceMessage(
                message.getBytes()), 0, NearCommunicationMessage.HEADER_LENGTH);
        NearCommunicationMessage.MessageHeader messageHeader = mNearCommunicationMessage.parseHeader(header);
        assertEquals(NearCommunicationMessage.DATA_PIECE, messageHeader.getStatusCode());
        assertEquals(message.length(), messageHeader.getDataLength());
        assertTrue(messageHeader.isHeaderIntegrityValid());
        assertTrue(messageHeader.isValidHeader());
        assertTrue(Arrays.equals(messageHeader.getDataChecksum(), dataChecksum));
        assertTrue(Arrays.equals(messageHeader.getHeaderChecksum(), headerChecksum));

    }

    @SmallTest
    public void testcheckDataIntegrity() throws Exception {
        String testString = "Hello123456^&*";
        byte[] hashedValue = HashingUtils.generateCheckSum(testString.getBytes());
        boolean result = NearCommunicationMessage.checkDataIntegrity(
                testString.getBytes(), hashedValue);
        assertTrue(result);
        testString += "Some extra value";
        result = NearCommunicationMessage.checkDataIntegrity(
                testString.getBytes(), hashedValue);
        assertFalse(result);
    }
}
