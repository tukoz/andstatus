/* 
 * Copyright (c) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.andstatus.app.context.MyContext;
import org.andstatus.app.util.MyLog;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ThreadLocalRandom;

public final class DbUtils {
    private static final int MS_BETWEEN_RETRIES = 500;
    private static final String TAG = DbUtils.class.getSimpleName();
    
    private DbUtils() {
    }

    /**
     * @return rowId
     */
    public static long addRowWithRetry(MyContext myContext, String tableName, ContentValues values, int nRetries) {
        String method = "addRowWithRetry";
        long rowId = -1;
        SQLiteDatabase db = myContext.getDatabase();
        if (db == null) {
            MyLog.v(TAG, method + "; Database is null");
            return 0;
        }
        for (int pass = 0; pass < nRetries; pass++) {
            try {
                rowId = db.insert(tableName, null, values);
                if (rowId != -1) {
                    break;
                }
                MyLog.v(method, "Error inserting row, table=" + tableName + "; pass=" + pass);
            } catch (NullPointerException e) {
                MyLog.i(method, "NullPointerException, table=" + tableName + "; pass=" + pass, e);
                break;
            } catch (SQLiteException e) {
                MyLog.i(method, "Database is locked, table=" + tableName + "; pass=" + pass, e);
                rowId = -1;
            }
            waitBetweenRetries(method);
        }
        if (rowId == -1) {
            MyLog.e(method, "Failed to insert row into " + tableName + "; values=" + values.toString(), null);
        }
        return rowId;
    }

    /**
     * @return Number of rows updated
     */
    public static int updateRowWithRetry(MyContext myContext, String tableName, long rowId, ContentValues values, int nRetries) {
        String method = "updateRowWithRetry";
        int rowsUpdated = 0;
        SQLiteDatabase db = myContext.getDatabase();
        if (db == null) {
            MyLog.v(TAG, method + "; Database is null");
            return 0;
        }
        for (int pass=0; pass<nRetries; pass++) {
            try {
                rowsUpdated = db.update(tableName, values, BaseColumns._ID + "=" + Long.toString(rowId), null);
                break;
            } catch (SQLiteException e) {
                MyLog.i(method, " Database is locked, pass=" + pass, e);
            }
            waitBetweenRetries(method);
        }
        if (rowsUpdated != 1) {
            MyLog.e(method, " Failed to update rowId=" + rowId + " updated " + rowsUpdated + " rows", null);
        }
        return rowsUpdated;
    }

    /** @return true if current thread was interrupted */
    public static boolean waitBetweenRetries(String method) {
        return waitMs(method, MS_BETWEEN_RETRIES);
    }

    /** @return true if current thread was interrupted */
    public static boolean waitMs(Object tag, long delayMs) {
        if (delayMs > 1) {
            long delay = delayMs;
            // http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
            if (Build.VERSION.SDK_INT >= 21) {
                delay = (delay/2) + ThreadLocalRandom.current().nextLong(0, delay);
            }
            Long startTime = System.currentTimeMillis();
            Object lock = new Object();
            synchronized (lock) {
                while (System.currentTimeMillis() < startTime + delay && System.currentTimeMillis() >= startTime) {
                    try {
                        lock.wait(delay);
                    } catch (InterruptedException e) {
                        MyLog.v(tag, "Interrupted waiting " + delay + "ms");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        return Thread.currentThread().isInterrupted();
    }

    // Couldn't use "Closeable" as a Type due to incompatibility with API <= 10
    public static void closeSilently(Object closeable) {
        closeSilently(closeable, "");
    }
    
    public static void closeSilently(Object closeable, String message) {
        if (closeable != null) {
            try {
                closeLegacy(closeable, message);
            } catch ( IOException e) {
                MyLog.ignored(closeable, e);
            }
        }
    }
    
    private static void closeLegacy(Object closeable, String message) throws IOException {
        if (Closeable.class.isAssignableFrom(closeable.getClass()) ) {
            ((Closeable) closeable).close();
        } else if (Cursor.class.isAssignableFrom(closeable.getClass())) {
            if (!((Cursor) closeable).isClosed()) {
                ((Cursor) closeable).close();
            }
        } else if (FileChannel.class.isAssignableFrom(closeable.getClass())) {
            ((FileChannel) closeable).close();
        } else if (InputStream.class.isAssignableFrom(closeable.getClass())) {
            ((InputStream) closeable).close();
        } else if (Reader.class.isAssignableFrom(closeable.getClass())) {
            ((Reader) closeable).close();
        } else if (SQLiteStatement.class.isAssignableFrom(closeable.getClass())) {
            ((SQLiteStatement) closeable).close();
        } else if (OutputStream.class.isAssignableFrom(closeable.getClass())) {
            ((OutputStream) closeable).close();
        } else if (OutputStreamWriter.class.isAssignableFrom(closeable.getClass())) {
            ((OutputStreamWriter) closeable).close();
        } else if (Writer.class.isAssignableFrom(closeable.getClass())) {
            ((Writer) closeable).close();
        } else {
            String detailMessage = "Couldn't close silently an object of the class: "
                    + closeable.getClass().getCanonicalName() 
                    + (TextUtils.isEmpty(message) ? "" : "; " + message) ;
            MyLog.w(TAG, MyLog.getStackTrace(new IllegalArgumentException(detailMessage)));
        }
    }

    @NonNull
    public static String getString(Cursor cursor, String columnName) {
        return cursor == null ? "" : getString(cursor, cursor.getColumnIndex(columnName));
    }

    @NonNull
    public static String getString(Cursor cursor, int columnIndex) {
        String value = "";
        if (cursor != null && columnIndex >= 0) {
            String value2 = cursor.getString(columnIndex);
            if (!TextUtils.isEmpty(value2)) {
                value = value2;
            }
        }
        return value;
    }

    public static boolean getBoolean(Cursor cursor, String columnName) {
        return getInt(cursor, columnName) == 1;
    }

    public static long getLong(Cursor cursor, String columnName) {
        if (cursor == null) {
            return 0;
        }
        long value = 0;
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex >= 0) {
            try {
                value = cursor.getLong(columnIndex);
            } catch (Exception e){
                MyLog.d(TAG, "getLong column " + columnName, e);
            }
        }
        return value;
    }

    public static int getInt(Cursor cursor, String columnName) {
        if (cursor == null) {
            return 0;
        }
        int value = 0;
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex >= 0) {
            try {
                value = cursor.getInt(columnIndex);
            } catch (Exception e){
                MyLog.d(TAG, "getInt column " + columnName, e);
            }
        }
        return value;
    }

    public static void execSQL(SQLiteDatabase db, String sql) {
        MyLog.v("execSQL", sql);
        db.execSQL(sql);
    }

    public static String sqlZeroToNull(long value) {
        return value == 0 ? null : Long.toString(value);
    }

    public static String sqlEmptyToNull(String value) {
        return TextUtils.isEmpty(value) ? null : "'" + value + "'";
    }
}
