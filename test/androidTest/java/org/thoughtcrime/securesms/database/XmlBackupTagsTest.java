package org.thoughtcrime.securesms.database;


import junit.framework.TestCase;

import android.provider.Telephony.BaseMmsColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.TextBasedSmsColumns;

/**
 * Document XML backup attributes corresponding to Android database column names.
 * Breaks if the names change in a future Android version.
 */
public class XmlBackupTagsTest extends TestCase {

    public void testCommonAttributes() {
        assertEquals(TextBasedSmsColumns.ADDRESS,   "address");   // text
        assertEquals(TextBasedSmsColumns.DATE,      "date");      // int
        assertEquals(TextBasedSmsColumns.DATE_SENT, "date_sent"); // int
        assertEquals(TextBasedSmsColumns.LOCKED,    "locked");    // int (boolean)
        assertEquals(TextBasedSmsColumns.READ,      "read");      // int (boolean)
        final String CONTACT_NAME    = "contact_name";            // text, SMS Backup & Restore
        final String READABLE_DATE   = "readable_date";           // text, SMS Backup & Restore
        final String THREAD_ADDRESS  = "thread_address";          // text, Signal
    }

    public void testSmsAttributes() {
        assertEquals(TextBasedSmsColumns.BODY,           "body");           // text
        assertEquals(TextBasedSmsColumns.PROTOCOL,       "protocol");       // int
        assertEquals(TextBasedSmsColumns.SERVICE_CENTER, "service_center"); // text
        assertEquals(TextBasedSmsColumns.STATUS,         "status");         // int
        assertEquals(TextBasedSmsColumns.SUBJECT,        "subject");        // text
        assertEquals(TextBasedSmsColumns.TYPE,           "type");           // int
        final String SC_TOA = "sc_toa";                                     // text, SMS Backup & Restore
        final String TOA    = "toa";                                        // text, SMS Backup & Restore
    }

    public void testMmsAttributes() {
        assertEquals(BaseMmsColumns.CONTENT_CLASS,         "ct_cls");      // int
        assertEquals(BaseMmsColumns.CONTENT_LOCATION,      "ct_l");        // text
        assertEquals(BaseMmsColumns.CONTENT_TYPE,          "ct_t");        // text
        assertEquals(BaseMmsColumns.CREATOR,               "creator");     // text, e.g., app name
        assertEquals(BaseMmsColumns.DELIVERY_REPORT,       "d_rpt");       // int
        assertEquals(BaseMmsColumns.DELIVERY_TIME,         "d_tm");        // int
        assertEquals(BaseMmsColumns.EXPIRY,                "exp");         // int
        assertEquals(BaseMmsColumns.MESSAGE_BOX,            "msg_box");    // int, e.g., MESSAGE_BOX_INBOX
        assertEquals(BaseMmsColumns.MESSAGE_CLASS,         "m_cls");       // text
        assertEquals(BaseMmsColumns.MESSAGE_ID,            "m_id");        // text
        assertEquals(BaseMmsColumns.MESSAGE_SIZE,          "m_size");      // int
        assertEquals(BaseMmsColumns.MESSAGE_TYPE,          "m_type");      // int
        assertEquals(BaseMmsColumns.MMS_VERSION,           "v");           // int
        assertEquals(BaseMmsColumns.PRIORITY,              "pri");         // int
        assertEquals(BaseMmsColumns.READ_REPORT,           "rr");          // int (boolean)
        assertEquals(BaseMmsColumns.READ_STATUS,           "read_status"); // int
        assertEquals(BaseMmsColumns.REPORT_ALLOWED,        "rpt_a");       // int (boolean)
        assertEquals(BaseMmsColumns.RESPONSE_STATUS,       "resp_st");     // int
        assertEquals(BaseMmsColumns.RESPONSE_TEXT,         "resp_txt");    // text
        assertEquals(BaseMmsColumns.RETRIEVE_STATUS,       "retr_st");     // int
        assertEquals(BaseMmsColumns.RETRIEVE_TEXT,         "retr_txt");    // text
        assertEquals(BaseMmsColumns.RETRIEVE_TEXT_CHARSET, "retr_txt_cs"); // int
        assertEquals(BaseMmsColumns.SEEN,                  "seen");        // int (boolean)
        assertEquals(BaseMmsColumns.STATUS,                "st");          // int
        assertEquals(BaseMmsColumns.SUBJECT,               "sub");         // text
        assertEquals(BaseMmsColumns.SUBJECT_CHARSET,       "sub_cs");      // subject
        assertEquals(BaseMmsColumns.SUBSCRIPTION_ID,       "sub_id");      // int (long), < 0 if unknown
        assertEquals(BaseMmsColumns.TEXT_ONLY,             "text_only");   // int (boolean)
        assertEquals(BaseMmsColumns.TRANSACTION_ID,        "tr_id");       // id
    }

    public void testPartAttributes() {
        assertEquals(Mms.Part.CHARSET,             "chset"); // charset, text
        assertEquals(Mms.Part.CONTENT_DISPOSITION, "cd");    // content disposition, text
        assertEquals(Mms.Part.CONTENT_ID,          "cid");   // content id, int
        assertEquals(Mms.Part.CONTENT_LOCATION,    "cl");    // content location, int
        assertEquals(Mms.Part.CONTENT_TYPE,        "ct");    // content type, text (MIME ?)
        assertEquals(Mms.Part.CT_START,            "ctt_s"); // start of content type, int
        assertEquals(Mms.Part.CT_TYPE,             "ctt_t"); // type of content type, text
        assertEquals(Mms.Part.FILENAME,            "fn");    // filename, text
        assertEquals(Mms.Part.NAME,                "name");  // name, text
        assertEquals(Mms.Part.SEQ,                 "seq");   // int
        assertEquals(Mms.Part.TEXT,                "text");  // text
        final String DISPLAY_NAME = "_display_name";         // text,   SMS Backup & Restore
        final String SIZE         = "_size";                 // int,    SMS Backup & Restore
        final String DATA         = "data";                  // base64, SMS Backup & Restore
    }
}
