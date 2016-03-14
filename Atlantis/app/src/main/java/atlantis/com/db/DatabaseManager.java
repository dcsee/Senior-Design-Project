package atlantis.com.db;

import android.content.Context;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import atlantis.com.model.CipherMessage;
import atlantis.com.model.Conversation;
import atlantis.com.model.Message;
import atlantis.com.model.Notebook;
import atlantis.com.model.OTP;
import atlantis.com.model.OutgoingMessage;
import atlantis.com.model.Person;

/**
 * Singleton for database access
 * Created by Andrew on 2/14/2015.
 */
public class DatabaseManager {

    static private DatabaseManager instance;

    static public DatabaseManager getInstance(Context ctx) {
        if (null == instance) {
            instance = new DatabaseManager(ctx);
        }
        return instance;
    }

    private final DatabaseHelper helper;

    private DatabaseManager(Context ctx) {
        helper = new DatabaseHelper(ctx);
    }

    private DatabaseHelper getHelper() {
        return helper;
    }

    /**
     * Get the connection source for transactions
     * @return The connection source
     */
    public ConnectionSource getConnectionSource() {
        return helper.getConnectionSource();
    }

    public List<Conversation> getAllConversations() throws SQLException {
        List<Conversation> conversations = null;
        conversations = getHelper().getConversationDao().queryBuilder()
                .orderBy(Conversation.ID_FIELD_NAME, false).query();
        return conversations;
    }

    public List<Notebook> getAllNotebooks() throws SQLException {
        List<Notebook> notebooks = null;
        notebooks = getHelper().getNotebookDao().queryForAll();
        return notebooks;
    }

    public Notebook getNotebookForOTP(OTP otp) throws SQLException {
        List<Notebook> results = getNotebookQueryBuilderMatchingOTP(otp).query();
        return results.size() > 0 ? results.get(0) : null;
    }

    public List<Notebook> getNotebooksInConversation(Conversation conversation) throws SQLException {
        return getHelper().getNotebookDao().queryBuilder()
                .where().eq(Notebook.CONVERSATION_FIELD_NAME, conversation).query();
    }

    public List<OTP> getOTPsInConversation(Conversation conversation) throws SQLException {
        List<Notebook> notebooks = getNotebooksInConversation(conversation);
        List<OTP> otps = new ArrayList<>(notebooks.size() * Notebook.OTP_COUNT_PER_NOTEBOOK);
        for(Notebook notebook : notebooks) {
            otps.add(notebook.getSendingOTP());
            otps.add(notebook.getReceivingOTP());
        }
        return otps;
    }

    public List<OTP> getSelfOTPsInConversation(Conversation conversation) throws SQLException {
        List<Notebook> notebooks = getNotebooksInConversation(conversation);
        List<OTP> otps = new ArrayList<>(notebooks.size());
        for(Notebook notebook : notebooks) {
            otps.add(notebook.getSendingOTP());
        }
        return otps;
    }

    public List<OTP> getNonSelfOTPsInConversation(Conversation conversation) throws SQLException {
        List<Notebook> notebooks = getNotebooksInConversation(conversation);
        List<OTP> otps = new ArrayList<>(notebooks.size());
        for(Notebook notebook : notebooks) {
            otps.add(notebook.getReceivingOTP());
        }
        return otps;
    }

    public Person getOwnerOfOTP(OTP otp) throws SQLException {
        List<Person> results = getHelper().getPersonDao().queryBuilder()
                .leftJoin(getNotebookQueryBuilderMatchingOTP(otp)).query();
        return results.size() > 0 ? results.get(0) : null;
    }

    private QueryBuilder<Notebook, Integer> getNotebookQueryBuilderMatchingOTP(OTP otp) throws SQLException {
        QueryBuilder<Notebook, Integer> notebookQueryBuilder = getHelper()
                .getNotebookDao().queryBuilder();
        notebookQueryBuilder.where()
                .eq(Notebook.RECEIVING_OTP_FIELD_NAME, otp)
                .or()
                .eq(Notebook.SENDING_OTP_FIELD_NAME, otp);
        return notebookQueryBuilder;
    }

    /**
     * These functions can search the database (for ID and such)
     */
    public Message getMessageWithId(int messageId) {
        Message message = null;
        try {
            message = getHelper().getMessageDao().queryForId(messageId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return message;
    }

    public Notebook getNotebookWithId(int notebookId) throws SQLException {
        return getHelper().getNotebookDao().queryForId(notebookId);
    }

    public Conversation getConversationWithId(int conversationId) {
        Conversation conversation = null;
        try {
            conversation = getHelper().getConversationDao().queryForId(conversationId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conversation;
    }

    public OTP getOTPWithId(int otpId) {
        OTP otp = null;
        try {
            otp = getHelper().getOTPDao().queryForId(otpId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return otp;
    }

    public void refreshOTP(OTP otp) throws SQLException {
        getHelper().getOTPDao().refresh(otp);
    }

    public void refreshNotebook(Notebook notebook) throws SQLException {
        getHelper().getNotebookDao().refresh(notebook);
    }

    public void refreshMessage(Message message) throws SQLException {
        getHelper().getMessageDao().refresh(message);
    }

    public void refreshConversation(Conversation conversation) throws SQLException {
        getHelper().getConversationDao().refresh(conversation);
    }

    /**
     * These are for adding/removing class objects to the table, and modifying existing entries.
     */
    public void addConversation(Conversation l) {
        try {
            getHelper().getConversationDao().create(l);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateOutgoingMessage(OutgoingMessage outgoingMessage) throws SQLException {
        getHelper().getOutgoingMessageDao().update(outgoingMessage);
    }

    public void updateConversation(Conversation conversation) {
        try {
            getHelper().getConversationDao().update(conversation);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateOTP(OTP otp) {
        try {
            getHelper().getOTPDao().update(otp);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeMessage(int messageId) throws SQLException {
        Message message = getMessageWithId(messageId);
        getHelper().getMessageDao().delete(message);

    }

    public void addOTP(OTP otp) {
        try {
            getHelper().getOTPDao().create(otp);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeOTP(OTP otp) throws SQLException {
        getHelper().getOTPDao().delete(otp);
    }

    public void addMessage(Message message) {
        try {
            getHelper().getMessageDao().create(message);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateMessage(Message message) throws SQLException {
        getHelper().getMessageDao().update(message);
    }

    public void addCipherMessage(CipherMessage cipherMessage) throws SQLException {
        getHelper().getCipherMessageDao().create(cipherMessage);
    }

    public void updateCipherMessage(CipherMessage cipherMessage) throws SQLException {
        getHelper().getCipherMessageDao().update(cipherMessage);
    }

    public void removeCipherMessage(CipherMessage cipherMessage) throws SQLException {
        getHelper().getCipherMessageDao().delete(cipherMessage);
    }

    public void addPerson(Person person) {
        try {
            getHelper().getPersonDao().create(person);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePerson(Person person) throws SQLException {
        getHelper().getPersonDao().delete(person);
    }

    public void addNotebook(Notebook notebook) {
        try {
            getHelper().getNotebookDao().create(notebook);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addOutgoingMessage(OutgoingMessage outgoingMessage) throws SQLException {
        getHelper().getOutgoingMessageDao().create(outgoingMessage);
    }

    public void removeOutgoingMessage(OutgoingMessage outgoingMessage) throws SQLException {
        getHelper().getOutgoingMessageDao().delete(outgoingMessage);
    }

    public void removeNotebook(Notebook notebook) throws SQLException {
        getHelper().getNotebookDao().delete(notebook);
    }

    public void removeConversation(int conversationId) throws SQLException {
        Conversation conversation = getConversationWithId(conversationId);
        Collection<Message> messageList = conversation.getMessages();
        Collection<Notebook> mNotebookList = conversation.getNotebooks();
        getHelper().getMessageDao().delete(messageList);
        getHelper().getNotebookDao().delete(mNotebookList);
        getHelper().getConversationDao().delete(conversation);

    }
}
