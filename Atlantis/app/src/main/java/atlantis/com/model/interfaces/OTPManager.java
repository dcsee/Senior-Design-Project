package atlantis.com.model.interfaces;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.harvester.HarvestService;
import atlantis.com.model.CipherMessage;
import atlantis.com.model.Message;
import atlantis.com.model.OTP;
import atlantis.com.model.exceptions.MessageAuthenticityFailedException;
import atlantis.com.model.exceptions.NotEnoughOTPException;
import atlantis.com.model.impls.OTPFileInputStream;
import atlantis.com.model.impls.OTPFileOutputStream;

/**
 * Interface for OTP creation and usage
 * Created by Daniel on 2/25/2015.
 */
public interface OTPManager {

    /**
     * Creates a new OTP with the given length
     * @param length The length of the OTP
     * @return The new OTP
     */
    OTP createOTP(int length, HarvestService harvestService) throws OTPFileOutputStream.OTPFileOutputStreamException, NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException, IOException;

    /**
     * Encrypts a message and move the OTP forward by the amount used
     * @param message The message to encrypt
     * @param homeOTP The OTP to use to encrypt
     * @return The encrypted cipher message
     * @throws NotEnoughOTPException Thrown when the OTP left is not enough to encrypt the message
     * @throws SQLException Thrown when there is an issue with the database
     */
    CipherMessage encryptMessage(Message message, OTP homeOTP) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException, SQLException, NoSuchAlgorithmException, InvalidKeyException;

    /**
     * Decrypts a cipher message and moves the OTP by the amount used (including the address)
     * @param cipherMessage The cipher to decrypt
     * @param foreignOTP The OTP to use to decrypt
     * @return The decrypted message
     * @throws NotEnoughOTPException Thrown when there is not enough OTP to decrypt (should not happen)
     * @throws SQLException Thrown when there is an issue with the database
     */
    Message decryptCipherMessage(CipherMessage cipherMessage, OTP foreignOTP) throws NotAuthenticatedException, PINNotCreatedException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException, SQLException, NoSuchAlgorithmException, InvalidKeyException, MessageAuthenticityFailedException;
}
