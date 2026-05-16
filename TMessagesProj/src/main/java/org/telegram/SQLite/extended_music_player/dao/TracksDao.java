package org.telegram.SQLite.extended_music_player.dao;

import static org.telegram.SQLite.extended_music_player.dao.PlaylistTrackDao.DB_PT_COLUMN_NAME_PLAYLIST_UID;
import static org.telegram.SQLite.extended_music_player.dao.PlaylistTrackDao.DB_PT_COLUMN_NAME_TRACK_UID;
import static org.telegram.SQLite.extended_music_player.dao.PlaylistTrackDao.DB_PT_TABLE_NAME;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;
import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;

import java.util.ArrayList;

public class TracksDao {

    private final org.telegram.SQLite.SQLiteDatabase database;

    public TracksDao(org.telegram.SQLite.SQLiteDatabase database) throws SQLiteException {
        this.database = database;

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_TRACKS_TABLE_NAME + " (" +
                DB_TRACKS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_DIALOG_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_MESSAGE_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE + " TEXT, " +
                DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME + " TEXT NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER + " TEXT, " +
                DB_TRACKS_COLUMN_NAME_DURATION_SEC + " REAL NOT NULL, " +
                " UNIQUE(" + DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ")" +
                ")").stepThis().dispose();


        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_ACC_IDS_MAPPING_TABLE_NAME + " (" +
                DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID + " INTEGER PRIMARY KEY, " +
                DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID + " REAL NOT NULL, " +
                " UNIQUE(" + DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID + ")" +
                ")").stepThis().dispose();
    }

    public Integer addMusicToLibrary(MusicData music) throws SQLiteException { //should be use in addTrackToPlaylist
        SQLitePreparedStatement state = database.executeFast(
                "INSERT OR IGNORE INTO " + DB_TRACKS_TABLE_NAME + " (" +
                        DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " +
                        DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
                        DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ", " +

                        DB_TRACKS_COLUMN_NAME_MUSIC_TITLE + ", " +
                        DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME + ", " +
                        DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER + ", " +
                        DB_TRACKS_COLUMN_NAME_DURATION_SEC +
                        ") VALUES (?, ?, ?,   ?, ?, ?, ?)"
        );

        state.requery();

        MusicMetaData metaData = music.getMusicMetaData();
        MessageLink link = music.getMessageLink();

        long globalAccountId = link.getGlobalAccountId();
        long dialogId = link.getDialogId();
        int msgId = link.getMessageId();

        state.bindLong(1, globalAccountId);
        state.bindLong(2, dialogId);
        state.bindInteger(3, msgId);

        state.bindString(4, metaData.getTitle());
        state.bindString(5, metaData.getFileName());
        state.bindString(6, metaData.getPerformer());
        state.bindDouble(7, metaData.getDurationSec());

        state.step();
        state.dispose();

        return database.executeInt("SELECT " + DB_TRACKS_COLUMN_NAME_UID + " FROM " + DB_TRACKS_TABLE_NAME + " WHERE " +
                        DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + "=? AND " +
                        DB_TRACKS_COLUMN_NAME_DIALOG_ID + "=? AND " +
                        DB_TRACKS_COLUMN_NAME_MESSAGE_ID + "=?",
                globalAccountId,
                dialogId,
                msgId
        );
    }


    public MusicData getMusicFromLibraryById(int id) throws SQLiteException {
        MusicData music = null;
        String query = String.format(
                "SELECT t.%s, t.%s, t.%s, t.%s, " +
                        "t.%s, t.%s, t.%s," +
                        " a.%s " +
                        "FROM %s t WHERE %s=%s" +
                        "LEFT JOIN %s a ON t.%s = a.%s",
                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID,        // 0
                DB_TRACKS_COLUMN_NAME_DIALOG_ID,            // 1
                DB_TRACKS_COLUMN_NAME_MESSAGE_ID,           // 2
                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,          // 3

                DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,      // 4
                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,      // 5
                DB_TRACKS_COLUMN_NAME_DURATION_SEC,         // 6

                DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID,    // 7

                DB_TRACKS_TABLE_NAME,
                DB_TRACKS_COLUMN_NAME_UID,
                id,
                DB_ACC_IDS_MAPPING_TABLE_NAME,
                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID,
                DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID
        );
        SQLiteCursor cursor = database.queryFinalized(query);

        while (cursor.next()) {
        /* documentation
            0 - DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID
            1 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
            2 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID

            3 - DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,
            4 - DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,
            5 - DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,
            6 - DB_TRACKS_COLUMN_NAME_DURATION_SEC

            7 - DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID
         */

            long globalUserId = cursor.longValue(0);
            long dialogId = cursor.longValue(1);
            int messageId = cursor.intValue(2);

            String title = cursor.stringValue(3);
            String fileName = cursor.stringValue(4);
            String performer = cursor.stringValue(5);
            double durationInSec = cursor.doubleValue(6);

            int localAccountId = cursor.intValue(7);
            MessageLink link = new MessageLink(globalUserId, localAccountId, dialogId, messageId);
            MusicMetaData musicMetaData = new MusicMetaData(title, fileName, performer, durationInSec);
            music = new MusicData(link, musicMetaData);
        }

        cursor.dispose();

        return music;
    }

    public ArrayList<MusicData> getAllMusicFromLibrary() throws SQLiteException {
        String query = String.format(
                "SELECT t.%s, t.%s, t.%s, t.%s, " +
                        "t.%s, t.%s, t.%s," +
                        " a.%s " +
                        " FROM %s t" +
                        " LEFT JOIN %s a ON t.%s = a.%s",
                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID,        // 0
                DB_TRACKS_COLUMN_NAME_DIALOG_ID,            // 1
                DB_TRACKS_COLUMN_NAME_MESSAGE_ID,           // 2
                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,          // 3

                DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,      // 4
                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,      // 5
                DB_TRACKS_COLUMN_NAME_DURATION_SEC,         // 6

                DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID,    // 7

                DB_TRACKS_TABLE_NAME,
                DB_ACC_IDS_MAPPING_TABLE_NAME,

                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID,
                DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID
        );


        SQLiteCursor cursor = database.queryFinalized(query);

        ArrayList<MusicData> result = new ArrayList<>();
        while (cursor.next()) {
        /* documentation
            0 - DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID
            1 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
            2 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID

            3 - DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,
            4 - DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,
            5 - DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,
            6 - DB_TRACKS_COLUMN_NAME_DURATION_SEC

            7 - DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID
         */

            long globalUserId = cursor.longValue(0);
            long dialogId = cursor.longValue(1);
            int messageId = cursor.intValue(2);

            String title = cursor.stringValue(3);
            String fileName = cursor.stringValue(4);
            String performer = cursor.stringValue(5);
            double durationInSec = cursor.doubleValue(6);

            int localId = UtilDao.getIntOrNullObject(cursor, 7);

            MessageLink link = new MessageLink(globalUserId, localId, dialogId, messageId);
            MusicMetaData musicMetaData = new MusicMetaData(title, fileName, performer, durationInSec);
            result.add(new MusicData(link, musicMetaData));
        }

        cursor.dispose();

        return result;
    }

    public void addNewEntry(int localUserId, long mtprotoUserId) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "INSERT OR REPLACE INTO " + DB_ACC_IDS_MAPPING_TABLE_NAME + " (" +
                        DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID + ", " +
                        DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID +
                        ") VALUES (?, ?)"
        );

        state.requery();
        state.bindInteger(1, localUserId);
        state.bindLong(2, mtprotoUserId);
        state.step();
        state.dispose();
    }

    public int getLocalUserId(long mtprotoUserId) throws SQLiteException {
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID + " FROM " + DB_ACC_IDS_MAPPING_TABLE_NAME +
                        " WHERE " + DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID + " = ?",
                mtprotoUserId
        );

        int localAccId = UtilDao.INT_NULL_OBJECT;
        while (cursor.next()) {
        /* documentation
            0 - DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID
         */
            localAccId = UtilDao.getIntOrNullObject(cursor, 0);
        }
        cursor.dispose();

        return localAccId;
    }

    public void removeMappingEntryByLocalId(int localUserId) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "DELETE FROM " + DB_ACC_IDS_MAPPING_TABLE_NAME +
                        " WHERE " + DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID + " =? "
        );

        state.requery();
        state.bindInteger(1, localUserId);
        state.step();
        state.dispose();
    }


    public ArrayList<MessageLink> getMusicLinksByPlaylistId(int playlistId) throws SQLiteException {
        ArrayList<MessageLink> result = new ArrayList<>();
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT t." + DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " +
                        "t." + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
                        "t." + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ", " +
                        "a." + DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID + " " +
                        "FROM " + DB_TRACKS_TABLE_NAME + " t " +
                        "INNER JOIN " + DB_PT_TABLE_NAME + " pt ON t." + DB_TRACKS_COLUMN_NAME_UID + " = pt." + DB_PT_COLUMN_NAME_TRACK_UID + " " +
                        "LEFT JOIN " + DB_ACC_IDS_MAPPING_TABLE_NAME + " a " +
                        "ON t." + DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + " = a." + DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID + " " +
                        "WHERE pt." + DB_PT_COLUMN_NAME_PLAYLIST_UID + " = ?",
                playlistId
        );
//        SQLiteCursor cursor = database.queryFinalized(
//                "SELECT t." + DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " +
//                        "t." + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
//                        "t." + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + " " +
//                        "FROM " + DB_TRACKS_TABLE_NAME + " t " +
//                        "INNER JOIN " + DB_PT_TABLE_NAME + " pt ON t." + DB_TRACKS_COLUMN_NAME_UID + " = pt." + DB_PT_COLUMN_NAME_TRACK_UID + " " +
//                        "WHERE pt." + DB_PT_COLUMN_NAME_PLAYLIST_UID + " = ?",
//                playlistId
//        );

        while (cursor.next()) {   
        /* documentation
            0 - DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID
            1 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
            2 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID
            3 - DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID
         */

            long globalUserId = cursor.longValue(0);
            long dialogId = cursor.longValue(1);
            int messageId = cursor.intValue(2);
            int localAccId = UtilDao.getIntOrNullObject(cursor, 3);
            MessageLink link = new MessageLink(
                    globalUserId,
                    localAccId,
                    dialogId,
                    messageId
            );
            result.add(link);

        }

        cursor.dispose();

        return result;
    }

    //MTPROTO_LOCAL_IDS_MAPPING TABLE  //need to map local id to global id to be able to get object of message having only global id and be able to update this mapping separately
    private static final String DB_ACC_IDS_MAPPING_TABLE_NAME = "local_and_mtproto_acc_ids_mapping";
    private static final String DB_ACC_IDS_MAPPING_COLUMN_NAME_LOCAL_ID = "local_user_id";
    private static final String DB_ACC_IDS_MAPPING_COLUMN_NAME_MTPROTO_ID = "mtproto_user_id";

    //TRACKS TABLE
    public static final String DB_TRACKS_TABLE_NAME = "tracks";
    public static final String DB_TRACKS_COLUMN_NAME_UID = "uid";
    public static final String DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID = "global_acc_id";
    public static final String DB_TRACKS_COLUMN_NAME_DIALOG_ID = "dialog_id";
    public static final String DB_TRACKS_COLUMN_NAME_MESSAGE_ID = "message_id";
    public static final String DB_TRACKS_COLUMN_NAME_MUSIC_TITLE = "title";
    public static final String DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME = "file_name";
    public static final String DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER = "performer";
    public static final String DB_TRACKS_COLUMN_NAME_DURATION_SEC = "duration_in_seconds";
}
