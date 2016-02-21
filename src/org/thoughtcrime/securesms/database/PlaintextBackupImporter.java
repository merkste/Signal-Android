package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.util.Log;

import org.thoughtcrime.securesms.crypto.MasterCipher;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class PlaintextBackupImporter {

  public static void importPlaintextFromSd(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    Log.w("PlaintextBackupImporter", "Importing plaintext...");
    verifyExternalStorageForPlaintextImport();
    importPlaintext(context, masterSecret);
  }

  private static void verifyExternalStorageForPlaintextImport() throws NoExternalStorageException {
    if (!Environment.getExternalStorageDirectory().canRead() || !getPlaintextExportFile().exists())
      throw new NoExternalStorageException();
  }

  private static File getPlaintextExportFile() {
    File backup    = PlaintextBackupExporter.getPlaintextExportFile();
    File oldBackup = new File(Environment.getExternalStorageDirectory(), "TextSecurePlaintextBackup.xml");

    if (!backup.exists() && oldBackup.exists()) {
      return oldBackup;
    }
    return backup;
  }

  private static void importPlaintext(Context context, MasterSecret masterSecret)
      throws IOException
  {
    Log.w("PlaintextBackupImporter", "importPlaintext()");
    SmsDatabase    smsDatabase    = DatabaseFactory.getSmsDatabase(context);
    SQLiteDatabase smsTransaction = smsDatabase.beginTransaction();
    MmsDatabase    mmsDatabase    = DatabaseFactory.getMmsDatabase(context);
    SQLiteDatabase mmsTransaction = mmsDatabase.beginTransaction();

    try {
      ThreadDatabase threads         = DatabaseFactory.getThreadDatabase(context);
      XmlBackup      backup          = new XmlBackup(getPlaintextExportFile().getAbsolutePath());
      MasterCipher   masterCipher    = new MasterCipher(masterSecret);
      Set<Long>      modifiedThreads = new HashSet<>();
      XmlBackupItem  msg;

      while ((msg = backup.getNext()) != null) {
        if (msg.getAddress() == null || msg.getAddress().equals("null"))
          continue;
        if (!isAppropriateTypeForImport(msg.getType()))
          continue; // TODO: check whether this is sound for MMS as well

        long threadId;
        if (msg.getThreadAddress() != null) {
          threadId = threads.getThreadIdFor(RecipientFactory.getRecipientsFromString(context, msg.getThreadAddress(), false));
        } else {
          Recipients recipients = RecipientFactory.getRecipientsFromString(context, msg.getAddress(), false);
          threadId = threads.getThreadIdFor(recipients);
        }

        SQLiteStatement statement;
        if (msg instanceof XmlBackupItem.Sms) {
          statement = smsDatabase.createInsertStatement(smsTransaction);
          addMsgToStatement(statement, (XmlBackupItem.Sms) msg, threadId, masterCipher);
        } else {
          statement = smsDatabase.createInsertStatement(mmsTransaction);
          addMsgToStatement(statement, (XmlBackupItem.Mms) msg, threadId, masterCipher);
        }
        statement.execute();

        modifiedThreads.add(threadId);
      }

      for (long threadId : modifiedThreads) {
        threads.update(threadId, true);
      }

      Log.w("PlaintextBackupImporter", "Exited loop");
    } catch (XmlPullParserException e) {
      Log.w("PlaintextBackupImporter", e);
      throw new IOException("XML Parsing error!");
    } finally {
      smsDatabase.endTransaction(smsTransaction);
      mmsDatabase.endTransaction(mmsTransaction);
    }
  }

  private static void addMsgToStatement(SQLiteStatement statement, XmlBackupItem.Sms sms, long threadId, MasterCipher masterCipher) {
    addStringToStatement(statement, 1, sms.getAddress());
    addNullToStatement(statement, 2);
    addLongToStatement(statement, 3, sms.getDateSent());
    addLongToStatement(statement, 4, sms.getDate());
    addLongToStatement(statement, 5, sms.getProtocol());
    addLongToStatement(statement, 6, sms.getRead());
    addLongToStatement(statement, 7, sms.getStatus());
    addTranslatedTypeToStatement(statement, 8, sms.getType());
    addNullToStatement(statement, 9);
    addStringToStatement(statement, 10, sms.getSubject());
    addEncryptedStingToStatement(masterCipher, statement, 11, sms.getBody());
    addStringToStatement(statement, 12, sms.getServiceCenter());
    addLongToStatement(statement, 13, threadId);
  }

  private static void addMsgToStatement(SQLiteStatement statement, XmlBackupItem.Mms mms, long threadId, MasterCipher masterCipher) {
    addLongToStatement(statement, 1, threadId);                              // THREAD_ID
    addLongToStatement(statement, 2, mms.getDateSent());                     // DATE_SENT
    addLongToStatement(statement, 3, mms.getDate());                         // DATE_RECEIVED
    addTranslatedTypeToStatement(statement, 4, mms.getType());               // MESSAGE_BOX TODO: check mms/sms types
    addLongToStatement(statement, 5, mms.getRead());                         // READ                  DEFAULT 0
    addNullToStatement(statement, 6);                                        // MESSAGE_ID
    addStringToStatement(statement, 7, mms.getSubject());                    // SUBJECT
    addNullToStatement(statement, 8);                                        // SUBJECT_CHARSET
    addEncryptedStingToStatement(masterCipher, statement, 9, mms.getBody()); // BODY
    addLongToStatement(statement, 10, 1);                                    // PART_COUNT
    addNullToStatement(statement, 11);                                       // CONTENT_TYPE
    addNullToStatement(statement, 12);                                       // CONTENT_LOCATION
    addStringToStatement(statement, 13, mms.getAddress());                   // ADDRESS
    addNullToStatement(statement, 14);                                       // ADDRESS_DEVICE_ID
    addNullToStatement(statement, 15);                                       // EXPIRY
    addNullToStatement(statement, 16);                                       // MESSAGE_CLASS
    addNullToStatement(statement, 17);                                       // MESSAGE_TYPE
    addNullToStatement(statement, 18);                                       // MMS_VERSION
    addNullToStatement(statement, 19);                                       // MESSAGE_SIZE
    addNullToStatement(statement, 20);                                       // PRIORITY
    addNullToStatement(statement, 21);                                       // READ_REPORT
    addNullToStatement(statement, 22);                                       // REPORT_ALLOWED
    addNullToStatement(statement, 23);                                       // RESPONSE_STATUS
    addLongToStatement(statement, 24, mms.getStatus());                      // STATUS  TODO: check mms/sms status
    addNullToStatement(statement, 25);                                       // TRANSACTION_ID
    addNullToStatement(statement, 26);                                       // RETRIEVE_STATUS
    addNullToStatement(statement, 27);                                       // RETRIEVE_TEXT
    addNullToStatement(statement, 28);                                       // RETRIEVE_TEXT_CHARSET
    addNullToStatement(statement, 29);                                       // READ_STATUS
    addNullToStatement(statement, 30);                                       // CONTENT_CLASS
    addNullToStatement(statement, 31);                                       // RESPONSE_TEXT
    addNullToStatement(statement, 32);                                       // DELIVERY_TIME
    addNullToStatement(statement, 33);                                       // RECEIPT_COUNT,        DEFAULT 0
    addNullToStatement(statement, 34);                                       // MISMATCHED_IDENTITIES DEFAULT NULL
    addNullToStatement(statement, 35);                                       // NETWORK_FAILURE       DEFAULT NULL
    addNullToStatement(statement, 36);                                       // DELIVERY_REPORT
    addNullToStatement(statement, 37);                                       // SUBSCRIPTION_ID       DEFAULT -1
  }

  private static void addEncryptedStingToStatement(MasterCipher masterCipher, SQLiteStatement statement, int index, String value) {
    if (value == null || value.equals("null")) {
      statement.bindNull(index);
    } else {
      statement.bindString(index, masterCipher.encryptBody(value));
    }
  }

  private static void addTranslatedTypeToStatement(SQLiteStatement statement, int index, int type) {
    statement.bindLong(index, SmsDatabase.Types.translateFromSystemBaseType(type) | SmsDatabase.Types.ENCRYPTION_SYMMETRIC_BIT);
  }

  private static void addStringToStatement(SQLiteStatement statement, int index, String value) {
    if (value == null || value.equals("null")) statement.bindNull(index);
    else                                       statement.bindString(index, value);
  }

  private static void addNullToStatement(SQLiteStatement statement, int index) {
    statement.bindNull(index);
  }

  private static void addLongToStatement(SQLiteStatement statement, int index, long value) {
    statement.bindLong(index, value);
  }

  private static boolean isAppropriateTypeForImport(long theirType) {
    long ourType = SmsDatabase.Types.translateFromSystemBaseType(theirType);

    return ourType == MmsSmsColumns.Types.BASE_INBOX_TYPE ||
           ourType == MmsSmsColumns.Types.BASE_SENT_TYPE ||
           ourType == MmsSmsColumns.Types.BASE_SENT_FAILED_TYPE;
  }
}
