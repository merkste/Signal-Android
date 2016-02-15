package org.thoughtcrime.securesms.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class XmlBackupItem {
  private static final String PROTOCOL       = "protocol";
  private static final String ADDRESS        = "address";
  private static final String THREAD_ADDRESS = "thread_address";
  private static final String DATE           = "date";
  private static final String TYPE           = "type";
  private static final String SUBJECT        = "subject";
  private static final String BODY           = "body";
  private static final String SERVICE_CENTER = "service_center";
  private static final String READ           = "read";
  private static final String STATUS         = "status";

  private static final String OPEN_TAG_SMS = " <sms ";
  private static final String CLOSE_EMPTY_TAG = "/>";
  private static final String OPEN_ATTRIBUTE = "=\"";
  private static final String CLOSE_ATTRIBUTE = "\" ";
  private static final Pattern PATTERN = Pattern.compile("[^\u0020-\uD7FF]");
//  private static final String TOA = "toa";
//  private static final String SC_TOA = "sc_toa";
//  private static final String LOCKED = "locked";


  private int protocol;
  private String address;
  private String threadAddress;
  private long date;
  private int type;
  private String subject;
  private String body;
  private String serviceCenter;
  private int read;
  private int status;

  public XmlBackupItem(@NonNull MessageRecord record, @Nullable String threadAddress) {
    this(record.isMms() ? 1 : 0,
            record.getIndividualRecipient().getNumber(),
            threadAddress,
            record.getDateReceived(),
            MmsSmsColumns.Types.translateToSystemBaseType(record.getType()),
            null,
            record.getDisplayBody().toString(),
            null,
            1,
            record.isDelivered() ? SmsDatabase.Status.STATUS_COMPLETE : record.getDeliveryStatus());
  }

  public XmlBackupItem(@NonNull XmlPullParser parser) {
    for (int i = 0, count = parser.getAttributeCount(); i < count; i++) {
      switch (parser.getAttributeName(i)) {
        case PROTOCOL:
          protocol = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case ADDRESS:
          address = parser.getAttributeValue(i);
          break;
        case THREAD_ADDRESS:
          threadAddress = parser.getAttributeValue(i);
          break;
        case DATE:
          date = Long.parseLong(parser.getAttributeValue(i));
          break;
        case TYPE:
          type = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case SUBJECT:
          subject = parser.getAttributeValue(i);
          break;
        case BODY:
          body = parser.getAttributeValue(i);
          break;
        case SERVICE_CENTER:
          serviceCenter = parser.getAttributeValue(i);
          break;
        case READ:
          read = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case STATUS:
          status = Integer.parseInt(parser.getAttributeValue(i));
          break;
        default:
          // skip unkown attributes
      }
    }
  }

  private XmlBackupItem(int protocol, String address, String threadAddress, long date, int type, String subject,
                        String body, String serviceCenter, int read, int status) {
    this.protocol = protocol;
    this.address = address;
    this.threadAddress = threadAddress;
    this.date = date;
    this.type = type;
    this.subject = subject;
    this.body = body;
    this.serviceCenter = serviceCenter;
    this.read = read;
    this.status = status;
  }

  public int getProtocol() {
    return protocol;
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

  public int getType() {
    return type;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getServiceCenter() {
    return serviceCenter;
  }

  public int getRead() {
    return read;
  }

  public int getStatus() {
    return status;
  }

  @NonNull
  public java.io.Writer storeOn(@NonNull java.io.Writer writer) throws IOException {
    writer.write(OPEN_TAG_SMS);
    storeAttribute(writer, PROTOCOL, protocol);
    storeAttribute(writer, ADDRESS, escapeXML(address));
    if (threadAddress != null && !threadAddress.equals(address)) {
      storeAttribute(writer, THREAD_ADDRESS, escapeXML(threadAddress));
    }
    storeAttribute(writer, DATE, date);
    storeAttribute(writer, TYPE, type);
    storeAttribute(writer, SUBJECT, escapeXML(subject));
    storeAttribute(writer, BODY, escapeXML(body));
    // storeAttribute(writer, TOA, null);
    // storeAttribute(writer, SC_TOA, null);
    storeAttribute(writer, SERVICE_CENTER, serviceCenter);
    storeAttribute(writer, READ, read);
    storeAttribute(writer, STATUS, status);
    // storeAttribute(stringBuilder, LOCKED, 0);
    writer.write(CLOSE_EMPTY_TAG);

    return writer;
  }

  @NonNull
  protected <T> java.io.Writer storeAttribute(@NonNull java.io.Writer writer, @NonNull String name, T value) throws IOException {
    writer.write(name);
    writer.write(OPEN_ATTRIBUTE);
    writer.write(String.valueOf(value));
    writer.write(CLOSE_ATTRIBUTE);

    return writer;
  }

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

    public Sms(@NonNull SmsMessageRecord record, @Nullable String threadAddress) {
      super(record, threadAddress);
    }

    public Sms(@NonNull XmlPullParser parser) {
      super(parser);
    }

  }

  public static class Mms extends XmlBackupItem {

    public Mms(@NonNull MessageRecord record, @Nullable String threadAddress) {
      super(record, threadAddress);
    }
  }
}
