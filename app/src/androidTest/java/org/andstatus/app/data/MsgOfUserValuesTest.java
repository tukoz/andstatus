package org.andstatus.app.data;

import android.content.ContentValues;

import org.andstatus.app.context.Travis;
import org.andstatus.app.database.MsgOfUserTable;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Travis
public class MsgOfUserValuesTest {

    @Test
    public void testCreationFromContentValues() {
        ContentValues contentValues = new ContentValues();
        long userId = 0;
        long msgId = 2;
        MsgOfUserValues userValues = MsgOfUserValues.valueOf(userId, contentValues);
        assertFalse(userValues.isValid());

        userId = 1;
        userValues = MsgOfUserValues.valueOf(userId, contentValues);
        assertFalse(userValues.isValid());
        userValues.setMsgId(msgId);
        assertTrue(userValues.isValid());
        assertTrue(userValues.isEmpty());

        contentValues.put(MsgOfUserTable.SUBSCRIBED, true);
        userValues = MsgOfUserValues.valueOf(userId, contentValues);
        assertFalse(userValues.isValid());
        userValues.setMsgId(msgId);
        assertTrue(userValues.isValid());
        assertFalse(userValues.isEmpty());
    }
}