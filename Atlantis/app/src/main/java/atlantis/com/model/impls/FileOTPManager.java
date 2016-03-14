package atlantis.com.model.impls;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.LogUtils;
import atlantis.com.db.DatabaseManager;
import atlantis.com.harvester.HarvestService;
import atlantis.com.model.CipherMessage;
import atlantis.com.model.Message;
import atlantis.com.model.OTP;
import atlantis.com.model.exceptions.MessageAuthenticityFailedException;
import atlantis.com.model.exceptions.NotEnoughOTPException;
import atlantis.com.model.exceptions.OTPFileCouldNotBeDeletedException;
import atlantis.com.model.interfaces.OTPManager;

/**
 * Created by Daniel on 2/25/2015.
 * File OTP Manager deals with saving and reading OTP from files.
 */
public class FileOTPManager implements OTPManager {

    public final static String TAG = "FILE_OTP_MANGER";
    private final static String RELATIVE_OTP_LOCATION = "OTP";
    private final static int ADDRESS_LENGTH = 16;

    private static final int AUTHENTICITY_SECRET_KEY_LENGTH = 20;
    private static final int AUTHENTICITY_CODE_LENGTH = 20;

    private static final String AUTHENTICITY_ALGORITHM = "HmacSHA1";

    private static final Object mOTPMutex = new Object();

    private final Context context;

    public FileOTPManager(Context context) throws NotAuthenticatedException, PINNotCreatedException{
        this.context = context;
    }

    private byte[] readBytesFromFile(OTP otp, int bytesToSkip, int length) throws NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException, OTPFileInputStream.InvalidBufferException, IOException, NotEnoughOTPException {
        byte[] fileData = new byte[length];
        int bytesRead = 0;
        //opens the file where the OTP is, and creates a new input stream out of that

        System.out.println("Reading From: " +
                getOTPFileFromOTP(context, otp.getDataId()).getAbsolutePath() + "\n");
        System.out.println("Read start: Start " +
                bytesToSkip + " length " + length + " -------");

        OTPFileInputStream otpFileInputStream = new OTPFileInputStream(context, otp);
        otpFileInputStream.skip(bytesToSkip);
        bytesRead = otpFileInputStream.read(fileData);

        if(bytesRead != length){
            System.out.println("ReadBytesFromFile:\n");
            System.out.println("Read " + bytesRead + ", needed " + length + "\n");
            throw new NotEnoughOTPException();
        }

        return fileData;
    }

    public byte[] getAddressFromOTP(OTP otp) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException {
        return peekOTP(ADDRESS_LENGTH, otp);
    }

    private byte[] peekOTP(int length, OTP otp) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException {
        int startPosition = otp.getPosition();
        return readBytesFromFile(otp, startPosition, length);
    }

    public static File getOTPFileFromOTP(Context context, OTP otp){
        return getOTPFileFromOTP(context, otp.getDataId());
    }

    public static File getOTPFileFromOTP(Context context, Integer otpId){
        return new File(getDir(context), otpId.toString());
    }

    public OTP createOTP(int length, HarvestService harvestService) throws OTPFileOutputStream.OTPFileOutputStreamException, NotAuthenticatedException, PINCreationFailedException, PINNotCreatedException, IOException {

        Integer myID = createOTPFile();
        OTP myOTP = new OTP();

        //now, initialize the relevant fields of the new OTP
        myOTP.setDataId(myID);
        myOTP.setLength(length);
        myOTP.setPosition(0);
        writeOtpToFile(myOTP, length, harvestService);
        return myOTP;
    }

    public int createOTPFile() {
        SecureRandomGenerator secureRandomGenerator = new SecureRandomGenerator();
        Integer otpDataId = secureRandomGenerator.getInteger();
        File otpDir = getDir(context);
        String fileName = RELATIVE_OTP_LOCATION + "/" + otpDataId;
        System.out.println("Writing to OTP File: " + fileName);
        while(doesFileExist(fileName)) {
            otpDataId = secureRandomGenerator.getInteger();
            fileName = RELATIVE_OTP_LOCATION + "/" + otpDataId;
        }
        File otpFile = new File(otpDir, otpDataId.toString());
        return otpDataId;
    }

    public void deleteOTPFile(OTP otp) throws OTPFileCouldNotBeDeletedException {
        File otpFile = getOTPFileFromOTP(context, otp.getDataId());
        if(!otpFile.delete()) {
            throw new OTPFileCouldNotBeDeletedException();
        }
    }

    private void writeOtpToFile(OTP otp, int length, HarvestService harvestService) throws OTPFileOutputStream.OTPFileOutputStreamException, NotAuthenticatedException, IOException, PINNotCreatedException, PINCreationFailedException {
        SecureRandomGenerator secureRandomGenerator = new SecureRandomGenerator();
        byte[] buffer = new byte[length];
        OTPFileOutputStream outputStream = new OTPFileOutputStream(context, otp);
        int OTPAvailable = harvestService.getAvailableOTP(length, buffer);
        outputStream.write(buffer, 0, OTPAvailable);
        outputStream.close();
        setOTPLength(otp, OTPAvailable);
    }

    private static File getDir(Context context) {
        return context.getDir(RELATIVE_OTP_LOCATION, Context.MODE_PRIVATE);
    }

    /**
     * Check if file name already exist
     * @param newFileName to check
     * @return
     */
    private boolean doesFileExist(String newFileName) {
        String[] existingFiles = context.fileList();
        for(String existingFileName : existingFiles) {
            if(existingFileName.equals(newFileName)) {
                return true;
            }
        }
        return false;
    }

    private byte[] applyCipher(byte[] otpBytes, byte[] targetBytes){
        byte[] cipherBytes = new byte[targetBytes.length];

        for(int i = 0; i < cipherBytes.length; i++){
            cipherBytes[i] = (byte) (otpBytes[i] ^ targetBytes[i]);
        }
        return cipherBytes;
    }

    private byte[] readFromOTPFileAndMovePosition(int length, OTP otp) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException {

        if(length > otp.getLength()){
            System.out.println("Read Request Length: " + length + "\n");
            System.out.println("File Length: " + otp.getLength());
            throw new NotEnoughOTPException();
        }

        int startPosition = otp.getPosition();

        byte[] otpBytes = readBytesFromFile(otp, startPosition, length);

        if(otpBytes.length == length){
            otp.setPosition(startPosition + otpBytes.length);
            System.out.println("Incremented OTP Pointer by: " + startPosition);
            System.out.println("Moved OTP Pointer by " + length + " bytes\n");
            return otpBytes;
        } else{
            throw new NotEnoughOTPException();
        }
    }

    private byte[] decryptCipherText(byte[] cipherText, OTP foreignOTP) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException {
        System.out.println("====Decrypting");
        int lengthToRead = cipherText.length;

        byte[] foreignOTPBytes = readFromOTPFileAndMovePosition(lengthToRead, foreignOTP);
        LogUtils.logBytes("Foreign OTP", foreignOTPBytes);
        byte[] plainText = applyCipher(foreignOTPBytes, cipherText);
        LogUtils.logBytes("Plain Text", plainText);
        System.out.println("========");
        System.out.println("Decrypted Message\n");
        return plainText;
    }

    private byte[] encryptMessageText(byte[] plainTextContent, OTP otp) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException {
        System.out.println("=====Encrypting");
        int lengthToRead = plainTextContent.length;
        byte[] otpBytes = readFromOTPFileAndMovePosition(lengthToRead, otp);
        LogUtils.logBytes("Plain text", plainTextContent);
        LogUtils.logBytes("Otp text", otpBytes);
        byte[] cipherText = applyCipher(otpBytes, plainTextContent);
        LogUtils.logBytes("Cipher text", cipherText);
        System.out.println("=====");
        System.out.println("Encrypted Message\n");
        return cipherText;
    }

    /**
     * Generates the code for content to verify authenticity
     * @param content The content to generate the authenticity for
     * @param otp The OTP to be used to generate the shared secret key
     * @return The authenticity code of length AUTHENTICITY_CODE_LENGTH
     * @throws InvalidKeyException Should not throw
     * @throws NoSuchAlgorithmException Should not throw
     * @throws NotEnoughOTPException Thrown when there is not enough OTP to generate the authenticity code
     */
    private byte[] getAuthenticityCodeForContent(byte[] content, OTP otp) throws InvalidKeyException,
            NoSuchAlgorithmException, NotEnoughOTPException, NotAuthenticatedException, PINCreationFailedException, OTPFileInputStream.InvalidBufferException, PINNotCreatedException, IOException {
        
        Mac mac = Mac.getInstance(AUTHENTICITY_ALGORITHM);
        SecretKeySpec secret = new SecretKeySpec(
                peekOTP(AUTHENTICITY_SECRET_KEY_LENGTH, otp),
                mac.getAlgorithm());
        mac.init(secret);
        return mac.doFinal(content);
    }

    /**
     * Encrypts a message and move the OTP forward by the amount used
     * @param message The message to encrypt
     * @param homeOTP The OTP to use to encrypt
     * @return The encrypted cipher message
     * @throws NotEnoughOTPException Thrown when the OTP left is not enough to encrypt the message
     * @throws SQLException Thrown when there is an issue with the database
     */
    @Override
    public CipherMessage encryptMessage(Message message, OTP homeOTP) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException, SQLException, NoSuchAlgorithmException, InvalidKeyException {
        synchronized (mOTPMutex) {
            DatabaseManager manager = DatabaseManager.getInstance(context);
            manager.refreshOTP(homeOTP);

            CipherMessage cipherMessage = new CipherMessage();

            cipherMessage.setAddressBytes(this.getAddressFromOTP(homeOTP));
            homeOTP.setPosition(homeOTP.getPosition() + ADDRESS_LENGTH);

            // Encrypt message
            byte[] cipheredMessageContent = this.encryptMessageText(message.getContent(), homeOTP);

            // Generate authenticity code for encrypted message
            byte[] authenticityCode = getAuthenticityCodeForContent(cipheredMessageContent, homeOTP);
            homeOTP.setPosition(homeOTP.getPosition() + AUTHENTICITY_SECRET_KEY_LENGTH);

            // Combine data
            byte[] cipherContent = new byte[AUTHENTICITY_CODE_LENGTH + cipheredMessageContent.length];
            System.arraycopy(authenticityCode, 0,
                    cipherContent, 0,
                    AUTHENTICITY_CODE_LENGTH);
            System.arraycopy(cipheredMessageContent, 0,
                    cipherContent, AUTHENTICITY_CODE_LENGTH,
                    cipheredMessageContent.length);
            cipherMessage.setContent(cipherContent);

            manager.updateOTP(homeOTP);
            return cipherMessage;
        }
    }

    /**
     * Decrypts a cipher message and moves the OTP by the amount used (including the address)
     * @param cipherMessage The cipher to decrypt
     * @param foreignOTP The OTP to use to decrypt
     * @return The decrypted message
     * @throws NotEnoughOTPException Thrown when there is not enough OTP to decrypt (should not happen)
     * @throws SQLException Thrown when there is an issue with the database
     */
    @Override
    public Message decryptCipherMessage(CipherMessage cipherMessage, OTP foreignOTP) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException, SQLException, NoSuchAlgorithmException, InvalidKeyException, MessageAuthenticityFailedException {
        synchronized (mOTPMutex) {
            DatabaseManager manager = DatabaseManager.getInstance(context);

            manager.refreshOTP(foreignOTP);
            foreignOTP.setPosition(foreignOTP.getPosition() + ADDRESS_LENGTH);

            byte[] cipherContent = cipherMessage.getContent();

            // Check content is long enough to be valid
            if(cipherContent.length < AUTHENTICITY_CODE_LENGTH) {
                throw new MessageAuthenticityFailedException();
            }

            // Parse content
            byte[] receivedAuthenticityCode = new byte[AUTHENTICITY_CODE_LENGTH];
            byte[] cipheredMessageContent = new byte[cipherContent.length - AUTHENTICITY_CODE_LENGTH];
            System.arraycopy(cipherContent, 0,
                    receivedAuthenticityCode, 0,
                    AUTHENTICITY_CODE_LENGTH);
            System.arraycopy(cipherContent, AUTHENTICITY_CODE_LENGTH,
                    cipheredMessageContent, 0,
                    cipheredMessageContent.length);

            // Decrypt content first to move OTP to correct position
            byte[] content = this.decryptCipherText(cipheredMessageContent, foreignOTP);

            // Generate authenticity code for received content
            byte[] authenticityCode = getAuthenticityCodeForContent(cipheredMessageContent, foreignOTP);
            foreignOTP.setPosition(foreignOTP.getPosition() + AUTHENTICITY_SECRET_KEY_LENGTH);

            // Verify authenticity
            if(!Arrays.equals(receivedAuthenticityCode, authenticityCode)) {
                throw new MessageAuthenticityFailedException();
            }

            Message message = new Message();
            message.setContent(content);

            manager.updateOTP(foreignOTP);
            return message;
        }
    }

    public void setOTPLength(OTP otp, int length) {
        DatabaseManager manager = DatabaseManager.getInstance(context);
        otp.setLength(length);
        manager.updateOTP(otp);
    }

    public void setOTPDataId(OTP otp, int dataId) {
        DatabaseManager manager = DatabaseManager.getInstance(context);
        otp.setDataId(dataId);
        manager.updateOTP(otp);
    }
}
