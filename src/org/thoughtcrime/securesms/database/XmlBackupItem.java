package org.thoughtcrime.securesms.database;

import android.provider.Telephony;
import android.provider.Telephony.BaseMmsColumns;
import android.provider.Telephony.TextBasedSmsColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.thoughtcrime.securesms.database.model.MediaMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.NotificationMmsMessageRecord;
import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.BufferedWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class XmlBackupItem {
  // common signal attributes
//  public static final String READABLE_DATE   = "readable_date";  // SMS Backup & Restore, optional
//  public static final String CONTACT_NAME    = "contact_name";   // SMS Backup & Restore, optional
  public static final String THREAD_ADDRESS  = "thread_address"; // Signal

  // XML tags
  public static final String CLOSE_TAG       = ">";
  public static final String CLOSE_EMPTY_TAG = "/>";
  public static final String OPEN_ATTRIBUTE  = "=\"";
  public static final String CLOSE_ATTRIBUTE = "\" ";

  // XML escaping
  public static final Pattern PATTERN        = Pattern.compile("[^\u0020-\uD7FF]");

  // common attributes
  protected String address;
  protected String threadAddress;
  protected long date;
  protected long dateSent;
  protected int type;
  protected String subject;
  protected String body;
  protected int read;
  protected int status;

  public XmlBackupItem(@NonNull MessageRecord record, @Nullable String threadAddress) {
    this(record.getIndividualRecipient().getNumber(),
          threadAddress,
          record.getDateReceived(),
          record.getDateSent(),
          MmsSmsColumns.Types.translateToSystemBaseType(record.getType()),
          null,
          record.getDisplayBody().toString(),
          1,
          record.isDelivered() ? SmsDatabase.Status.STATUS_COMPLETE : record.getDeliveryStatus());
  }

  public XmlBackupItem(@NonNull XmlPullParser parser) {
    for (int i = 0, count = parser.getAttributeCount(); i < count; i++) {
      readAttribute(parser, i);
    }
  }

  protected void readAttribute(@NonNull XmlPullParser parser, int i) {
    switch (parser.getAttributeName(i)) {
      case TextBasedSmsColumns.ADDRESS:
        address = parser.getAttributeValue(i);
        break;
      case THREAD_ADDRESS:
        threadAddress = parser.getAttributeValue(i);
        break;
      case TextBasedSmsColumns.DATE:
        date = Long.parseLong(parser.getAttributeValue(i));
        break;
      case TextBasedSmsColumns.DATE_SENT:
        dateSent = Long.parseLong(parser.getAttributeValue(i));
        break;
      case TextBasedSmsColumns.READ:
        read = Integer.parseInt(parser.getAttributeValue(i));
        break;
      default:
        // ignore unkown attributes
    }
  }

  private XmlBackupItem(String address, String threadAddress, long date, long dateSent, int type, String subject,
                        String body, int read, int status) {
    this.address = address;
    this.threadAddress = threadAddress;
    this.date = date;
    this.dateSent = dateSent;
    this.type = type;
    this.subject = subject;
    this.body = body;
    this.read = read;
    this.status = status;
  }

  public String getAddress() {
    return address;
  }

  public String getThreadAddress() {
    return threadAddress;
  }

  public long getDate() {
    return date;
  }

  public long getDateSent() {
    return dateSent;
  }

  public int getType() {
    return type;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() { return body; }

  public int getRead() {
    return read;
  }

  public int getStatus() {
    return status;
  }

  @NonNull
  public abstract BufferedWriter storeOn(@NonNull BufferedWriter writer) throws IOException;

  @NonNull
  protected <T> BufferedWriter storeAttribute(@NonNull BufferedWriter writer, @NonNull String name, T value) throws IOException {
    writer.write(name);
    writer.write(OPEN_ATTRIBUTE);
    writer.write(String.valueOf(value));
    writer.write(CLOSE_ATTRIBUTE);

    return writer;
  }

  @Nullable
  protected String escapeXML(String s) {
    if (TextUtils.isEmpty(s)) return s;

    Matcher matcher = PATTERN.matcher(s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;"));
    StringBuffer st = new StringBuffer();

    while (matcher.find()) {
      String escaped = "";
      for (char ch : matcher.group(0).toCharArray()) {
        escaped += ("&#" + ((int) ch) + ";");
      }
      matcher.appendReplacement(st, escaped);
    }
    matcher.appendTail(st);
    return st.toString();
  }

  public static class Sms extends XmlBackupItem {
    // SMS attribute names
    public static final String TOA = "toa";       // SMS Backup & Restore, optional
    public static final String SC_TOA = "sc_toa"; // SMS Backup & Restore, optional

    // XML tags
    public static final String OPEN_TAG_SMS    = " <sms ";

    // SMS attributes
    protected int protocol;
    protected String serviceCenter;

    public Sms(@NonNull SmsMessageRecord record, @Nullable String threadAddress) {
      super(record, threadAddress);
    }

    public Sms(@NonNull XmlPullParser parser) {
      super(parser);
    }

    @Override
    protected void readAttribute(@NonNull XmlPullParser parser, int i) {
      switch (parser.getAttributeName(i)) {
        case TextBasedSmsColumns.STATUS:
          status = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case TextBasedSmsColumns.PROTOCOL:
          protocol = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case TextBasedSmsColumns.TYPE:
          type = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case TextBasedSmsColumns.SUBJECT:
          subject = parser.getAttributeValue(i);
          break;
        case TextBasedSmsColumns.BODY:
          body = parser.getAttributeValue(i);
          break;
        case TextBasedSmsColumns.SERVICE_CENTER:
          serviceCenter = parser.getAttributeValue(i);
          break;
        default:
          super.readAttribute(parser, i);
      }
    }

    public int getProtocol() {
      return protocol;
    }

    public String getServiceCenter() {
      return serviceCenter;
    }

    @NonNull
    public BufferedWriter storeOn(@NonNull BufferedWriter writer) throws IOException {
      writer.write(OPEN_TAG_SMS);

      storeAttribute(writer, TextBasedSmsColumns.PROTOCOL, protocol);
      storeAttribute(writer, TextBasedSmsColumns.ADDRESS, escapeXML(address));
      if (threadAddress != null && !threadAddress.equals(address)) {
        // aka group address
        storeAttribute(writer, THREAD_ADDRESS, escapeXML(threadAddress));
      }
      storeAttribute(writer, TextBasedSmsColumns.DATE, date);
      storeAttribute(writer, TextBasedSmsColumns.DATE_SENT, dateSent);
      storeAttribute(writer, TextBasedSmsColumns.TYPE, type);
      storeAttribute(writer, TextBasedSmsColumns.SUBJECT, escapeXML(subject));
      storeAttribute(writer, TextBasedSmsColumns.BODY, escapeXML(body));
      storeAttribute(writer, TextBasedSmsColumns.SERVICE_CENTER, serviceCenter);
      storeAttribute(writer, TextBasedSmsColumns.READ, read);
      storeAttribute(writer, TextBasedSmsColumns.STATUS, status);

      writer.write(CLOSE_EMPTY_TAG);

      return writer;
    }
  }



  public static class Mms extends XmlBackupItem {

    // Part attribute names
    public static final String DISPLAY_NAME = "_display_name"; // SMS Backup & Restore, optional
    public static final String SIZE         = "_size";         // SMS Backup & Restore, optional
    public static final String DATA         = "data";          // SMS Backup & Restore, optional

    // XML tags
    public static final String OPEN_TAG_MMS    = " <mms ";
    public static final String OPEN_TAG_PART   = "  <part ";
    public static final String CLOSE_TAG_MMS   = " </mms> ";

    // character sets
    public static final int UTF_8 = 106;

    // MMS attributes
    private byte[] contentLocation;
    private long expiry          = -1;  // TODO: check meaning of expiry long value
    private long messageSize     = 0;
    private int partCount        = 1;
    private int subscriptionId   = -1;
    private byte[] transactionId = null;


    public Mms(@NonNull MediaMmsMessageRecord record, @Nullable String threadAddress) {
      super(record, threadAddress);
      this.partCount = record.getPartCount();
//      this.slideDeck = record.getSlideDeck();
    }

    public Mms(@NonNull NotificationMmsMessageRecord record, @Nullable String threadAddress) {
      super(record, threadAddress);
      this.contentLocation = record.getContentLocation();
      this.messageSize     = record.getMessageSize();
      this.expiry          = record.getExpiration();
//      this.status          = record.getStatus(); TODO: check status type of MMS
      this.subscriptionId = record.getSubscriptionId();
      this.transactionId   = record.getTransactionId();
    }

    public Mms(@NonNull XmlPullParser parser) throws IOException, XmlPullParserException {
      super(parser);
      while (parser.next() != XmlPullParser.END_TAG) {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
          continue;
        }
        if (!parser.getName().equalsIgnoreCase("part")) {
          continue;
        }
        if (body == null) {
          // read first text part only
          readTextOnlyPart(parser);
        }
      }
    }

    @Override
    protected void readAttribute(@NonNull XmlPullParser parser, int i) {
      switch (parser.getAttributeName(i)) {
        case BaseMmsColumns.SUBJECT:
          subject = parser.getAttributeValue(i);
          break;
        case BaseMmsColumns.STATUS:
          status = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case BaseMmsColumns.CONTENT_LOCATION:
          contentLocation = null; // TODO: convert to byte array
          break;
        case BaseMmsColumns.EXPIRY:
          expiry = Long.parseLong(parser.getAttributeValue(i));
          break;
        case BaseMmsColumns.MESSAGE_SIZE:
          messageSize = Long.parseLong(parser.getAttributeValue(i));
          break;
        case BaseMmsColumns.SUBSCRIPTION_ID:
          subscriptionId = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case BaseMmsColumns.TRANSACTION_ID:
          transactionId = null;  // TODO: convert to byte array
          break;
        default:
          super.readAttribute(parser, i);
      }
    }

    protected void readTextOnlyPart(@NonNull XmlPullParser parser) {
      String mime = null;
      String text = null;
      for (int i=0, count=parser.getAttributeCount(); i<count; i++) {
        switch (parser.getAttributeName(i)) {
          case Telephony.Mms.Part.CONTENT_TYPE:
            mime = parser.getAttributeValue(i);
            break;
          case Telephony.Mms.Part.TEXT:
            text = parser.getAttributeValue(i);
            break;
          default:
            // ignore other attributes
        }
        if (mime != null && text != null) {
          if (mime.equals("text/plain")) {
            // TODO deal with encoding: CHSET
            body = text;
          }
          break;
        }
      }
    }

    public String getContentLocation() {
      return contentLocation == null ? null : new String(contentLocation);
    }

    public long getMessageSize() {
      return messageSize;
    }
    public long getExpiry() {
      return expiry;
    }

    public int getPartCount() {
      return partCount;
    }

    public int getSubscriptionId() {
      return subscriptionId;
    }

    public String getTransactionId() {
      return transactionId == null ? null : new String(transactionId);
    }

    @NonNull
    public BufferedWriter storeOn(@NonNull BufferedWriter writer) throws IOException {
      writer.write(OPEN_TAG_MMS);

      storeAttribute(writer, BaseMmsColumns.TEXT_ONLY, 1);              // optional
      storeAttribute(writer, TextBasedSmsColumns.ADDRESS, escapeXML(address));
      if (threadAddress != null && !threadAddress.equals(address)) {
        // aka group address
        storeAttribute(writer, THREAD_ADDRESS, escapeXML(threadAddress));
      }
      storeAttribute(writer, TextBasedSmsColumns.DATE, date);
      storeAttribute(writer, TextBasedSmsColumns.DATE_SENT, dateSent);
      storeAttribute(writer, BaseMmsColumns.MESSAGE_BOX, type);
      storeAttribute(writer, BaseMmsColumns.SUBJECT, escapeXML(subject));
      storeAttribute(writer, TextBasedSmsColumns.READ, read);
      storeAttribute(writer, BaseMmsColumns.STATUS, status);

//      storeAttribute(writer, BaseMmsColumns.CONTENT_CLASS, null);
//      storeAttribute(writer, BaseMmsColumns.SUBJECT_CHARSET, 106);
//      storeAttribute(writer, BaseMmsColumns.RETRIEVE_STATUS, "128");
      storeAttribute(writer, BaseMmsColumns.CONTENT_LOCATION, contentLocation);
      storeAttribute(writer, BaseMmsColumns.TRANSACTION_ID, transactionId);
//      storeAttribute(writer, BaseMmsColumns.MESSAGE_CLASS, "personal");
//      storeAttribute(writer, BaseMmsColumns.DELIVERY_TIME, null);
//      storeAttribute(writer, BaseMmsColumns.READ_STATUS, null);
//      storeAttribute(writer, BaseMmsColumns.CONTENT_TYPE, "application/vnd.wap.multipart.related");
//      storeAttribute(writer, BaseMmsColumns.SUBSCRIPTION_ID, 0);
//      storeAttribute(writer, BaseMmsColumns.RETRIEVE_TEXT_CHARSET, null);
//      storeAttribute(writer, BaseMmsColumns.DELIVERY_REPORT, 128);
//      storeAttribute(writer, BaseMmsColumns.MESSAGE_ID, "NOKASDF0000900002");
//      storeAttribute(writer, BaseMmsColumns.SEEN, 1);
//      storeAttribute(writer, BaseMmsColumns.MESSAGE_TYPE, 132);
//      storeAttribute(writer, BaseMmsColumns.MMS_VERSION, 17);
      storeAttribute(writer, BaseMmsColumns.EXPIRY, expiry);
//      storeAttribute(writer, BaseMmsColumns.PRIORITY, 129);
//      storeAttribute(writer, BaseMmsColumns.READ_REPORT, 129);
//      storeAttribute(writer, BaseMmsColumns.RESPONSE_TEXT, null);
//      storeAttribute(writer, BaseMmsColumns.REPORT_ALLOWED, null);
//      storeAttribute(writer, BaseMmsColumns.LOCKED, 0);
//      storeAttribute(writer, BaseMmsColumns.RETRIEVE_TEXT, null);
//      storeAttribute(writer, BaseMmsColumns.RESPONSE_STATUS, null);
      storeAttribute(writer, BaseMmsColumns.MESSAGE_SIZE, messageSize);
//      storeAttribute(writer, BaseMmsColumns.CREATOR, null);
//      storeAttribute(writer, READABLE_DATE, "08.03.3588 12:54:30");
//      storeAttribute(writer, CONTACT_NAME, "Hook");

      writer.write(CLOSE_TAG);
      writer.newLine();

      // store message text only
      writer.write(OPEN_TAG_PART);

      storeAttribute(writer, Telephony.Mms.Part.SEQ, 0);
      storeAttribute(writer, Telephony.Mms.Part.CONTENT_TYPE, "text/plain");
      storeAttribute(writer, Telephony.Mms.Part.NAME, null);
      storeAttribute(writer, Telephony.Mms.Part.CHARSET, UTF_8);
      storeAttribute(writer, Telephony.Mms.Part.CONTENT_DISPOSITION, null);
      storeAttribute(writer, Telephony.Mms.Part.FILENAME, null);
      storeAttribute(writer, Telephony.Mms.Part.CONTENT_ID, "&lt;text_0&gt;"); // should be int?
      storeAttribute(writer, Telephony.Mms.Part.CONTENT_LOCATION, "text_0.txt");      // should be int?
      storeAttribute(writer, Telephony.Mms.Part.CT_START, null);
      storeAttribute(writer, Telephony.Mms.Part.CT_TYPE, null);
      storeAttribute(writer, Telephony.Mms.Part.TEXT, escapeXML(body));
//      storeAttribute(writer, DISPLAY_NAME, null);
//      storeAttribute(writer, SIZE, null);

      writer.write(CLOSE_EMPTY_TAG);
      writer.newLine();

      writer.write(CLOSE_TAG_MMS);

      return writer;
    }
  }
}
