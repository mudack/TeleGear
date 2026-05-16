package org.telegram.SQLite.extended_music_player.dao;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteException;
import org.telegram.messenger.extended_music_player.entity.MessageLink;

public class UtilDao {
    public static long getLongOrNullObject(SQLiteCursor cursor, int columnIndex) throws SQLiteException {
        return (cursor.isNull(columnIndex)) ? LONG_NULL_OBJECT : cursor.longValue(columnIndex);
    }

    public static int getIntOrNullObject(SQLiteCursor cursor, int columnIndex) throws SQLiteException {
        return (cursor.isNull(columnIndex)) ? INT_NULL_OBJECT : cursor.intValue(columnIndex);
    }

    public static double getDoubleOrNullObject(SQLiteCursor cursor, int columnIndex) throws SQLiteException {
        return (cursor.isNull(columnIndex)) ? DOUBLE_NULL_OBJECT : cursor.doubleValue(columnIndex);
    }

    public static String getStringNullObject(SQLiteCursor cursor, int columnIndex) throws SQLiteException {
        return (cursor.isNull(columnIndex)) ? STRING_NULL_OBJECT : cursor.stringValue(columnIndex);
    }


    public static final int INT_NULL_OBJECT = -1;
    public static final long LONG_NULL_OBJECT = -1;
    public static final double DOUBLE_NULL_OBJECT = -1;
    public static final String STRING_NULL_OBJECT = "";
}
