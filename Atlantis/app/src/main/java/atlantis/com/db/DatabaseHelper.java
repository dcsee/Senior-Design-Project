package atlantis.com.db;


/**
 * Created by Andrew on 2/14/2015.
 */

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.util.ArrayList;
import java.util.List;

import atlantis.com.model.CipherMessage;
import atlantis.com.model.Conversation;
import atlantis.com.model.Message;
import atlantis.com.model.Notebook;
import atlantis.com.model.OTP;
import atlantis.com.model.OutgoingMessage;
import atlantis.com.model.Person;

class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    @SuppressWarnings("SpellCheckingInspection")
    private static final String DATABASE_NAME = "ConversationDB.sqlite";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final Class[] MODEL_CLASSES = new Class[]{
            Conversation.class,
            CipherMessage.class,
            Message.class,
            Notebook.class,
            OTP.class,
            Person.class,
            OutgoingMessage.class
    };

    /**
     * This is the main Database Access.
     * This creates the tables for each class.
     */
    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            for (Class modelClass : MODEL_CLASSES) {
                TableUtils.createTable(connectionSource, modelClass);
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is in case we need to add a class/table to an existing database. (unlikely but could be useful.)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            List<String> allSql = new ArrayList<>();
            switch (oldVersion) {
                case 1:
                    //allSql.add("alter table AdData add column `new_col` VARCHAR");
                    //allSql.add("alter table AdData add column `new_col2` VARCHAR");
            }
            for (String sql : allSql) {
                db.execSQL(sql);
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "exception during onUpgrade", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * These functions return the database access objects,
     * which handle the writing to the table.
     */

    public Dao<Message, Integer> getMessageDao() throws java.sql.SQLException {
        return getDao(Message.class);
    }

    public Dao<CipherMessage, Integer> getCipherMessageDao() throws java.sql.SQLException {
        return getDao(CipherMessage.class);
    }

    public Dao<Conversation, Integer> getConversationDao() throws java.sql.SQLException {
        return getDao(Conversation.class);
    }

    public Dao<OTP, Integer> getOTPDao() throws java.sql.SQLException {
        return getDao(OTP.class);
    }

    public Dao<Person, Integer> getPersonDao() throws java.sql.SQLException {
        return getDao(Person.class);
    }

    public Dao<Notebook, Integer> getNotebookDao() throws java.sql.SQLException {
        return getDao(Notebook.class);
    }

    public Dao<OutgoingMessage, Integer> getOutgoingMessageDao() throws java.sql.SQLException {
        return getDao(OutgoingMessage.class);
    }
}

