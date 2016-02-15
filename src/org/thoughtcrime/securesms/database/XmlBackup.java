package org.thoughtcrime.securesms.database;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class XmlBackup {
  private final XmlPullParser parser;

  public XmlBackup(String path) throws XmlPullParserException, FileNotFoundException {
    this.parser = XmlPullParserFactory.newInstance().newPullParser();
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
    parser.setInput(new FileInputStream(path), null);
  }

  public XmlBackupItem getNext() throws IOException, XmlPullParserException {
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
      if (parser.getEventType() != XmlPullParser.START_TAG) {
        continue;
      }
      if (!parser.getName().equalsIgnoreCase("sms")) {
        continue;
      }
      if (parser.getAttributeCount() <= 0) {
        continue;
      }
      return new XmlBackupItem(parser);
    }
    return null;
  }

  public static class Writer {

    private static final String XML_HEADER      = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>";
    private static final String CREATED_BY      = "<!-- File Created By Signal -->";
    private static final String OPEN_TAG_SMSES  = "<smses count=\"%d\">";
    private static final String CLOSE_TAG_SMSES = "</smses>";

    private final BufferedWriter bufferedWriter;

    public Writer(String path, int count) throws IOException {
      bufferedWriter = new BufferedWriter(new FileWriter(path, false));

      bufferedWriter.write(XML_HEADER);
      bufferedWriter.newLine();
      bufferedWriter.write(CREATED_BY);
      bufferedWriter.newLine();
      bufferedWriter.write(String.format(OPEN_TAG_SMSES, count));
    }

    public void writeItem(XmlBackupItem item) throws IOException {
      bufferedWriter.newLine();
      item.storeOn(bufferedWriter);
    }

    public void close() throws IOException {
      bufferedWriter.newLine();
      bufferedWriter.write(CLOSE_TAG_SMSES);
      bufferedWriter.close();
    }
  }
}
