package org.thoughtcrime.securesms.database;


import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.model.MediaMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.NotificationMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.thoughtcrime.securesms.recipients.Recipients;

import java.io.File;
import java.io.IOException;

public class PlaintextBackupExporter {

  private static final String FILENAME = "SignalPlaintextBackup.xml";

  public static void exportPlaintextToSd(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    verifyExternalStorageForPlaintextExport();
    exportPlaintext(context, masterSecret);
  }

  private static void verifyExternalStorageForPlaintextExport() throws NoExternalStorageException {
    if (!Environment.getExternalStorageDirectory().canWrite())
      throw new NoExternalStorageException();
  }

  public static File getPlaintextExportFile() {
    return new File(Environment.getExternalStorageDirectory(), FILENAME);
  }

  private static void exportPlaintext(Context context, MasterSecret masterSecret)
      throws IOException
  {
    int count               = DatabaseFactory.getSmsDatabase(context).getMessageCount();
    XmlBackup.Writer writer = new XmlBackup.Writer(getPlaintextExportFile().getAbsolutePath(), count);
    ThreadDatabase threads  = DatabaseFactory.getThreadDatabase(context);

    SmsMessageRecord record;
    EncryptingSmsDatabase.Reader reader = null;
    int skip                            = 0;
    final int ROW_LIMIT                 = 500;

    do {
      if (reader != null)
        reader.close();

      reader = DatabaseFactory.getEncryptingSmsDatabase(context).getMessages(masterSecret, skip, ROW_LIMIT);

      while ((record = reader.getNext()) != null) {
        String threadAddress = getThreadAddress(record, threads);
        writer.writeItem(new XmlBackupItem.Sms(record, threadAddress));
      }

      skip += ROW_LIMIT;
    } while (reader.getCount() > 0);

    skip = 0;
    MmsDatabase.Reader mmsReader = null;
    do {
      if (mmsReader != null)
        mmsReader.close();

      mmsReader = DatabaseFactory.getMmsDatabase(context).getMessages(masterSecret, skip, ROW_LIMIT);

      MessageRecord mmsRecord;
      while ((mmsRecord = mmsReader.getNext()) != null) {
        String threadAddress = getThreadAddress(mmsRecord, threads);
        if (mmsRecord instanceof MediaMmsMessageRecord) {
          writer.writeItem(new XmlBackupItem.Mms((MediaMmsMessageRecord) mmsRecord, threadAddress));
        } else if (mmsRecord instanceof NotificationMmsMessageRecord) {
          writer.writeItem(new XmlBackupItem.Mms((NotificationMmsMessageRecord) mmsRecord, threadAddress));
        }
      }

      skip += ROW_LIMIT;
    } while (mmsReader.getCount() > 0);

    writer.close();
  }

  @Nullable
  private static String getThreadAddress(MessageRecord record, ThreadDatabase threads) {
    Recipients threadRecipients = threads.getRecipientsForThreadId(record.getThreadId());
    if (threadRecipients == null || threadRecipients.isEmpty()) {
      return null;
    }
    return threadRecipients.getPrimaryRecipient().getNumber();
  }
}
