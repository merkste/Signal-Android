package org.thoughtcrime.securesms.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.BufferedWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class XmlBackupItem {
  // common attributes names
  public static final String ADDRESS         = "address";
  public static final String THREAD_ADDRESS  = "thread_address";
  public static final String DATE            = "date";
  public static final String DATE_SENT       = "date_sent";
  public static final String READ            = "read";
  public static final String LOCKED          = "locked";
  public static final String READABLE_DATE   = "readable_date"; // SMS Backup & Restore
  public static final String CONTACT_NAME    = "contact_name";  // SMS Backup & Restore

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
      case ADDRESS:
        address = parser.getAttributeValue(i);
        break;
      case THREAD_ADDRESS:
        threadAddress = parser.getAttributeValue(i);
        break;
      case DATE:
        date = Long.parseLong(parser.getAttributeValue(i));
        break;
      case DATE_SENT:
        dateSent = Long.parseLong(parser.getAttributeValue(i));
        break;
      case READ:
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
    public static final String STATUS          = "status";
    public static final String PROTOCOL        = "protocol";
    public static final String TYPE            = "type";
    public static final String SUBJECT         = "subject";
    public static final String BODY            = "body";
    public static final String SERVICE_CENTER  = "service_center";
//  public static final String TOA = "toa";
//  public static final String SC_TOA = "sc_toa";

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
        case STATUS:
          status = Integer.parseInt(parser.getAttributeValue(i));
          break;
        case PROTOCOL:
          protocol = Integer.parseInt(parser.getAttributeValue(i));
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

      storeAttribute(writer, PROTOCOL, protocol);
      storeAttribute(writer, ADDRESS, escapeXML(address));
      if (threadAddress != null && !threadAddress.equals(address)) {
        // aka group address
        storeAttribute(writer, THREAD_ADDRESS, escapeXML(threadAddress));
      }
      storeAttribute(writer, DATE, date);
      storeAttribute(writer, DATE_SENT, dateSent);
      storeAttribute(writer, TYPE, type);
      storeAttribute(writer, SUBJECT, escapeXML(subject));
      storeAttribute(writer, BODY, escapeXML(body));
      storeAttribute(writer, SERVICE_CENTER, serviceCenter);
      storeAttribute(writer, READ, read);
      storeAttribute(writer, STATUS, status);
//      storeAttribute(writer, TOA, null);       // unused
//      storeAttribute(writer, SC_TOA, null);    // unused
//      storeAttribute(stringBuilder, LOCKED, 0); // unused
//      storeAttribute(writer, READABLE_DATE, "08.03.3588 12:54:30"); // optional
//      storeAttribute(writer, CONTACT_NAME, "Hook"); // optional

      writer.write(CLOSE_EMPTY_TAG);

      return writer;
    }
  }



  public static class Mms extends XmlBackupItem {

    // MMS attribute names
    /** @see android.provider.Telephony.BaseMmsColumns */
    public static final String TEXT_ONLY   = "text_only";   // does message have only a text part?
    public static final String SUB         = "sub";         // subject, text
    public static final String RETR_ST     = "retr_st";     // retrieve status, int
    public static final String CT_CLS      = "ct_cls";      // content class, int
    public static final String SUB_CS      = "sub_cs";      // character set of subject
    public static final String CT_L        = "ct_l";        // content location, text
    public static final String TR_ID       = "tr_id";       // transaction id
    public static final String ST          = "st";          // status, int
    public static final String MSG_BOX     = "msg_box";     // message box, e.g. MESSAGE_BOX_INBOX, int
    public static final String M_CLS       = "m_cls";       // message class, text
    public static final String D_TM        = "d_tm";        // delivery time, int
    public static final String READ_STATUS = "read_status"; // read status, int
    public static final String CT_T        = "ct_t";        // content type, text
    public static final String RETR_TXT_CS = "retr_txt_cs"; // character set of retrieve text
    public static final String D_RPT       = "d_rpt";       // delivery report, int
    public static final String M_ID        = "m_id";        // message id, text
    public static final String SEEN        = "seen";        // message been seen by the user? The "seen" flag determines whether we need to show a new message notification., int (boolean)
    public static final String M_TYPE      = "m_type";      // message type defined by MMS spec, int
    public static final String V           = "v";           // version of the MMS specification, int
    public static final String EXP         = "exp";         // expiry time, int
    public static final String PRI         = "pri";         // priority
    public static final String RR          = "rr";          // read report, int (boolean)
    public static final String RESP_TXT    = "resp_txt";    // response test, text
    public static final String RPT_A       = "rpt_a";       // read report allowed, int (boolean)
    public static final String SUB_ID      = "sub_id";      // subscription id, int (long), < 0 if unknown
    public static final String RETR_TXT    = "retr_txt";    // retrive text, text
    public static final String RESP_ST     = "resp_st";     // response status, int
    public static final String M_SIZE      = "m_size";      // size of the message, int
    public static final String CREATOR     = "creator";     // identity of sender (e.g. app name), text

    // Part attribute names
    /** @see android.provider.Telephony.Mms.Part */
    public static final String SEQ          = "seq";           // order, int
    public static final String CT           = "ct";            // content type, text (MIME ?)
    public static final String NAME         = "name";          // name, text
    public static final String CHSET        = "chset";         // charset, text
    public static final String CD           = "cd";            // content disposition, text
    public static final String FN           = "fn";            // filename, text
    public static final String CID          = "cid";           // content id, int
    public static final String CL           = "cl";            // content location, int
    public static final String CTT_S        = "ctt_s";         // start of content type, int
    public static final String CTT_T        = "ctt_t";         // type of content type, text
    public static final String TEXT         = "text";          // text
    public static final String DISPLAY_NAME = "_display_name"; // SMS Backup & Restore
    public static final String SIZE         = "_size";         // SMS Backup & Restore
    public static final String DATA         = "data";          // SMS Backup & Restore

    // XML tags
    public static final String OPEN_TAG_MMS    = " <mms ";
    public static final String OPEN_TAG_PART   = "  <part ";
    public static final String CLOSE_TAG_MMS   = " </mms> ";

    // character sets
    public static final int UTF_8 = 106;


    public Mms(@NonNull MessageRecord record, @Nullable String threadAddress) {
      super(record, threadAddress);
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
        case SUB:
          subject = parser.getAttributeValue(i);
          break;
        case ST:
          status = Integer.parseInt(parser.getAttributeValue(i));
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
          case CT:
            mime = parser.getAttributeValue(i);
            break;
          case TEXT:
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

    @NonNull
    public BufferedWriter storeOn(@NonNull BufferedWriter writer) throws IOException {
      writer.write(OPEN_TAG_MMS);

      storeAttribute(writer, TEXT_ONLY, 1);              // optional
      storeAttribute(writer, ADDRESS, escapeXML(address));
      if (threadAddress != null && !threadAddress.equals(address)) {
        // aka group address
        storeAttribute(writer, THREAD_ADDRESS, escapeXML(threadAddress));
      }
      storeAttribute(writer, DATE, date);
      storeAttribute(writer, DATE_SENT, dateSent);
      storeAttribute(writer, MSG_BOX, type);
      storeAttribute(writer, SUB, escapeXML(subject));
      storeAttribute(writer, READ, read);
      storeAttribute(writer, ST, status);

//      storeAttribute(writer, CT_CLS, null);
//      storeAttribute(writer, SUB_CS, 106);      // charset of subject
//      storeAttribute(writer, RETR_ST, "128");
//      storeAttribute(writer, CT_L, null);
//      storeAttribute(writer, TR_ID, "NOHGKJFXDGAO27ng4E");
//      storeAttribute(writer, M_CLS, "personal");
//      storeAttribute(writer, D_TM, null);           // delivery time, int
//      storeAttribute(writer, READ_STATUS, null);
//      storeAttribute(writer, CT_T, "application/vnd.wap.multipart.related");
//      storeAttribute(writer, SUB_ID, 0);          // subscription id, int (long), <0 if unknown
//      storeAttribute(writer, RETR_TXT_CS, null);
//      storeAttribute(writer, D_RPT, 128);
//      storeAttribute(writer, M_ID, "NOKASDF0000900002");
//      storeAttribute(writer, DATE_SENT, 0);     // sent date, int
//      storeAttribute(writer, SEEN, 1);
//      storeAttribute(writer, M_TYPE, 132);
//      storeAttribute(writer, V, 17);
//      storeAttribute(writer, EXP, null);
//      storeAttribute(writer, PRI, 129);
//      storeAttribute(writer, RR, 129);
//      storeAttribute(writer, RESP_TXT, null);
//      storeAttribute(writer, RPT_A, null);
//      storeAttribute(writer, LOCKED, 0);
//      storeAttribute(writer, RETR_TXT, null);
//      storeAttribute(writer, RESP_ST, null);
//      storeAttribute(writer, M_SIZE, null);
//      storeAttribute(writer, READABLE_DATE, "08.03.3588 12:54:30"); // optional
//      storeAttribute(writer, CONTACT_NAME, "Hook");
//      storeAttribute(writer, CREATOR, null);  // optional ?

      writer.write(CLOSE_TAG);
      writer.newLine();

      // store message text only
      writer.write(OPEN_TAG_PART);
      /** @see android.provider.Telephony.Mms.Part */
      storeAttribute(writer, SEQ, 0);
      storeAttribute(writer, CT, "text/plain");
      storeAttribute(writer, NAME, null);
      storeAttribute(writer, CHSET, UTF_8);
      storeAttribute(writer, CD, null);
      storeAttribute(writer, FN, null);
      storeAttribute(writer, CID, "&lt;text_0&gt;"); // should be int?
      storeAttribute(writer, CL, "text_0.txt");      // should be int?
      storeAttribute(writer, CTT_S, null);
      storeAttribute(writer, CTT_T, null);
      storeAttribute(writer, TEXT, escapeXML(body));
      storeAttribute(writer, DISPLAY_NAME, null);
      storeAttribute(writer, SIZE, null);


      writer.write(CLOSE_EMPTY_TAG);
      writer.newLine();

      writer.write(CLOSE_TAG_MMS);

      return writer;
    }
  }
}
