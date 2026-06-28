package org.telegram.SQLite.extended_music_player;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;
import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;
import org.telegram.messenger.extended_music_player.entity.Playlist;

import java.io.File;
import java.util.ArrayList;

/**
 * GlobalMusicStorage is a low-level data access layer responsible for direct interaction
 * with the local SQLite database used for storing music tracks and playlists.
 *
 * <p>This class encapsulates all SQL operations such as creating tables,
 * inserting data, and executing queries.</p>
 *
 * <h3>⚠️ Threading Model</h3>
 * <ul>
 *     <li>This class DOES NOT manage threading.</li>
 *     <li>All methods are executed in the calling thread.</li>
 *     <li>It is the responsibility of the caller (e.g., Controller) to ensure
 *     that database operations are performed on a background thread.</li>
 * </ul>
 *
 * <h3>⚠️ Error Handling</h3>
 * <ul>
 *     <li>All methods may throw runtime exceptions related to database operations.</li>
 *     <li>Every method call SHOULD be wrapped in try-catch blocks by the caller.</li>
 *     <li>This design allows centralized error handling at a higher level
 *     (e.g., in GlobalMusicController).</li>
 * </ul>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Database initialization (tables creation).</li>
 *     <li>CRUD operations for tracks, playlists, and playlist-track relations.</li>
 *     <li>Returning raw data models from database queries.</li>
 * </ul>
 *
 * <h3>Non-Responsibilities</h3>
 * <ul>
 *     <li>Does NOT handle threading (no DispatchQueue or async logic).</li>
 *     <li>Does NOT contain business logic.</li>
 *     <li>Does NOT interact with UI.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>
 * try {
 *     int trackId = storage.insertTrack(musicData);
 * } catch (Exception e) {
 *     // handle error
 * }
 * </pre>
 *
 * <p>Typically used via a higher-level controller (e.g., GlobalMusicController),
 * which manages threading, error handling, and communication with UI.</p>
 */
public class GlobalMusicDatabaseImpl implements GlobalMusicDatabase {

    private org.telegram.SQLite.SQLiteDatabase database;

    public GlobalMusicDatabaseImpl(File dbFile) throws SQLiteException {
        initDatabase(dbFile);
    }

    private void initDatabase(File dbFile) throws SQLiteException {

        database = new org.telegram.SQLite.SQLiteDatabase(dbFile.getPath());
        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_TRACKS_TABLE_NAME + " (" +
                DB_TRACKS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_DIALOG_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_MESSAGE_ID + " INTEGER NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE + " TEXT, " +
                DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME + " TEXT NOT NULL, " +
                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER + " TEXT, " +
                DB_TRACKS_COLUMN_NAME_DURATION_SEC + " REAL NOT NULL, " +
                " UNIQUE(" + DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " + DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID + ", " + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ")" +
                ")").stepThis().dispose();

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PLAYLISTS_TABLE_NAME + " (" +
                DB_PLAYLISTS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + " TEXT NOT NULL, " +
                " UNIQUE(" + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + ")" +
                ")").stepThis().dispose();

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PT_TABLE_NAME + " (" +
                DB_PT_COLUMN_NAME_PLAYLIST_UID + " INTEGER NOT NULL, " +
                DB_PT_COLUMN_NAME_TRACK_UID + " INTEGER NOT NULL, " +
                " PRIMARY KEY (" + DB_PT_COLUMN_NAME_PLAYLIST_UID + ", " + DB_PT_COLUMN_NAME_TRACK_UID + ")" +
                ")").stepThis().dispose();

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_RECENT_PLAYLIST_TABLE_NAME + " (" +
                DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + " INTEGER NOT NULL, " +
                DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT + " INTEGER NOT NULL, " +
                " PRIMARY KEY (" + DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + ")" +
                ")").stepThis().dispose();

    }

    @Override
    public Playlist createPlaylist(String name) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "INSERT OR IGNORE INTO " + DB_PLAYLISTS_TABLE_NAME + " (" +
                        DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                        ") VALUES (?)"
        );

        state.requery();
        state.bindString(1, name);
        state.step();
        state.dispose();

        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_PLAYLISTS_COLUMN_NAME_UID + ", " +
                        DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + "=?",
                name
        );

        Playlist playlist = null;

        if (cursor.next()) {
            playlist = new Playlist(
                    cursor.intValue(0),
                    cursor.stringValue(1)
            );
        }

        cursor.dispose();

        return playlist;
    }

    @Override
    public ArrayList<Playlist> getAllPlaylists() throws SQLiteException {
        ArrayList<Playlist> results = new ArrayList<>();
        SQLiteCursor cursor = database.queryFinalized("SELECT * FROM " + DB_PLAYLISTS_TABLE_NAME);
        while (cursor.next()) {
            Playlist playlist = new Playlist(
                    cursor.intValue(0),
                    cursor.stringValue(1)
            );
            results.add(playlist);
        }
        cursor.dispose();

        return results;
    }

    @Override
    public Playlist getPlaylistById(int id) throws SQLiteException {
        Playlist playlist = null;
        SQLiteCursor cursor = database.queryFinalized("SELECT * FROM " + DB_PLAYLISTS_TABLE_NAME + " WHERE " + DB_PLAYLISTS_COLUMN_NAME_UID + "=?", id);
        while (cursor.next()) {
            playlist = new Playlist(
                    cursor.intValue(0),
                    cursor.stringValue(1)
            );
        }
        cursor.dispose();
        return playlist;
    }

    @Override
    public Integer addMusicToLibrary(MusicData music) throws SQLiteException { //should be use in addTrackToPlaylist
        SQLitePreparedStatement state = database.executeFast(
                "INSERT OR IGNORE INTO " + DB_TRACKS_TABLE_NAME + " (" +
                        DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " +
                        DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID + ", " +
                        DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
                        DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ", " +

                        DB_TRACKS_COLUMN_NAME_MUSIC_TITLE + ", " +
                        DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME + ", " +
                        DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER + ", " +
                        DB_TRACKS_COLUMN_NAME_DURATION_SEC +
                        ") VALUES (?, ?, ?, ?,   ?, ?, ?, ?)"
        );

        state.requery();

        MusicMetaData metaData = music.getMusicMetaData();
        MessageLink link = music.getMessageLink();

        long globalAccountId = link.getGlobalAccountId();
        int localAccountId = link.getLocalAccountId();
        long dialogId = link.getDialogId();
        int msgId = link.getMessageId();

        state.bindLong(1, globalAccountId);
        state.bindInteger(2, localAccountId);
        state.bindLong(3, dialogId);
        state.bindInteger(4, msgId);

        state.bindString(5, metaData.getTitle());
        state.bindString(6, metaData.getFileName());
        state.bindString(7, metaData.getPerformer());
        state.bindDouble(8, metaData.getDurationSec());

        state.step();
        state.dispose();

        Integer musicIdByLink = database.executeInt("SELECT " + DB_TRACKS_COLUMN_NAME_UID + " FROM " + DB_TRACKS_TABLE_NAME + " WHERE " +
                        DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + "=? AND " +
                        DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID + "=? AND " +
                        DB_TRACKS_COLUMN_NAME_DIALOG_ID + "=? AND " +
                        DB_TRACKS_COLUMN_NAME_MESSAGE_ID + "=?",
                globalAccountId,
                localAccountId,
                dialogId,
                msgId
        );
        return musicIdByLink;
    }


    @Override
    public MusicData getMusicFromLibraryById(int id) throws SQLiteException {
        MusicData music = null;
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT ?, ?, ?, ?,   ?, ?, ?, ?  FROM " + DB_TRACKS_TABLE_NAME + " WHERE " + DB_TRACKS_COLUMN_NAME_UID + "=?",

                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID,
                DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID,
                DB_TRACKS_COLUMN_NAME_DIALOG_ID,
                DB_TRACKS_COLUMN_NAME_MESSAGE_ID,

                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,
                DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,
                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,
                DB_TRACKS_COLUMN_NAME_DURATION_SEC,

                id
        );

        while (cursor.next()) {
        /* documentation
            0 - DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID
            1 - DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID
            2 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
            3 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID

            4 - DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,
            5 - DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,
            6 - DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,
            7 - DB_TRACKS_COLUMN_NAME_DURATION_SEC
         */

            long globalAccId = cursor.longValue(0);
            int localAccId = cursor.intValue(1);
            long dialogId = cursor.longValue(2);
            int messageId = cursor.intValue(3);

            String title = cursor.stringValue(4);
            String fileName = cursor.stringValue(5);
            String performer = cursor.stringValue(6);
            double durationInSec = cursor.doubleValue(7);

            MessageLink link = new MessageLink(globalAccId, localAccId, dialogId, messageId);
            MusicMetaData musicMetaData = new MusicMetaData(title, fileName, performer, durationInSec);
            music = new MusicData(link, musicMetaData);
        }

        cursor.dispose();

        return music;
    }

    @Override
    public ArrayList<MusicData> getAllMusicFromLibrary() throws SQLiteException {
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT ?, ?, ?, ?,   ?, ?, ?, ? FROM " + DB_TRACKS_TABLE_NAME,
                DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID,
                DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID,
                DB_TRACKS_COLUMN_NAME_DIALOG_ID,
                DB_TRACKS_COLUMN_NAME_MESSAGE_ID,

                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,
                DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,
                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,
                DB_TRACKS_COLUMN_NAME_DURATION_SEC

        );

        ArrayList<MusicData> result = new ArrayList<>();
        while (cursor.next()) {
        /* documentation
            0 - DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID
            1 - DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID
            2 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
            3 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID

            4 - DB_TRACKS_COLUMN_NAME_MUSIC_TITLE,
            5 - DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME,
            6 - DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER,
            7 - DB_TRACKS_COLUMN_NAME_DURATION_SEC
         */

            long globalAccId = cursor.longValue(0);
            int localAccId = cursor.intValue(1);
            long dialogId = cursor.longValue(2);
            int messageId = cursor.intValue(3);

            String title = cursor.stringValue(4);
            String fileName = cursor.stringValue(5);
            String performer = cursor.stringValue(6);
            double durationInSec = cursor.doubleValue(7);

            MessageLink link = new MessageLink(globalAccId, localAccId, dialogId, messageId);
            MusicMetaData musicMetaData = new MusicMetaData(title, fileName, performer, durationInSec);
            result.add(new MusicData(link, musicMetaData));
        }

        cursor.dispose();

        return result;
    }

    @Override
    public void addMusicToPlaylist(int playlistId, MusicData music) throws SQLiteException {
        Integer musicId = addMusicToLibrary(music);
        SQLitePreparedStatement state = database.executeFast(
                "INSERT OR IGNORE INTO " + DB_PT_TABLE_NAME + " (" +
                        DB_PT_COLUMN_NAME_PLAYLIST_UID + ", " +
                        DB_PT_COLUMN_NAME_TRACK_UID +
                        ") VALUES (?, ?)"
        );
        state.requery();

        state.bindInteger(1, playlistId);
        state.bindInteger(2, musicId);

        state.step();
        state.dispose();
    }

    @Override
    public ArrayList<MessageLink> getMusicLinksByPlaylistId(int playlistId) throws SQLiteException {
        ArrayList<MessageLink> result = new ArrayList<>();
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT t." + DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID + ", " +
                        "t." + DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID + ", " +
                        "t." + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
                        "t." + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + " " +
                        "FROM " + DB_TRACKS_TABLE_NAME + " t " +
                        "INNER JOIN " + DB_PT_TABLE_NAME + " pt ON t." + DB_TRACKS_COLUMN_NAME_UID + " = pt." + DB_PT_COLUMN_NAME_TRACK_UID + " " +
                        "WHERE pt." + DB_PT_COLUMN_NAME_PLAYLIST_UID + " = ?",
                playlistId
        );

        while (cursor.next()) {
        /* documentation
            0 - DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID
            1 - DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID
            2 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
            3 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID
         */

            long globalAccId = cursor.longValue(0);
            int localAccId = cursor.intValue(1);
            long dialogId = cursor.longValue(2);
            int messageId = cursor.intValue(3);

            MessageLink link = new MessageLink(globalAccId, localAccId, dialogId, messageId);
            result.add(link);

        }

        cursor.dispose();

        return result;
    }

    @Override
    public void markPlaylistAsRecent(int playlistId, long timestamp) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "INSERT INTO " + DB_RECENT_PLAYLIST_TABLE_NAME + " (" +
                        DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + ", " +
                        DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT +
                        ") VALUES (?, ?)" +
                        " ON CONFLICT(" +
                        DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID +
                        ") DO UPDATE SET " +
                        DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT + " = excluded." +
                        DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT
        );
        state.requery();

        state.bindInteger(1, playlistId);
        state.bindLong(2, timestamp);

        state.step();
        state.dispose();
    }

    @Override
    public ArrayList<Playlist> getRecentPlaylists(int limit) throws SQLiteException {
        return getRecentPlaylistsByOffset(0, limit);
    }

    @Override
    public ArrayList<Playlist> getRecentPlaylistsByOffset(int offset, int limit) throws SQLiteException {

        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " +
                        " r." + DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + ", " +
                        " p." + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                        " FROM " + DB_RECENT_PLAYLIST_TABLE_NAME + " r " +
                        " JOIN " + DB_PLAYLISTS_TABLE_NAME + " p " +
                        " ON p." + DB_PLAYLISTS_COLUMN_NAME_UID + " = r." + DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + " " +
                        " ORDER BY r." + DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT + " DESC " +
                        " LIMIT ?,?",
                offset,
                limit
        );

        ArrayList<Playlist> result = new ArrayList<>();

        while (cursor.next()) {

            int playlistId = cursor.intValue(0);
            String name = cursor.stringValue(1);

            result.add(new Playlist(playlistId, name));
        }

        cursor.dispose();

        return result;
    }

    @Override
    public void removeRecentPlaylist(int playlistId) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "DELETE FROM " + DB_RECENT_PLAYLIST_TABLE_NAME +
                        " WHERE " + DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + " = ?"
        );

        state.requery();

        state.bindInteger(1, playlistId);

        state.step();
        state.dispose();
    }

    @Override
    public void clearRecentPlaylists() throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "DELETE FROM " + DB_RECENT_PLAYLIST_TABLE_NAME
        );

        state.requery();
        state.step();
        state.dispose();
    }

    public static final String GLOBAL_MUSIC_DB_FILE_NAME = "global_music.db";

    //TRACKS TABLE
    private static final String DB_TRACKS_TABLE_NAME = "tracks";
    private static final String DB_TRACKS_COLUMN_NAME_UID = "uid";
    private static final String DB_TRACKS_COLUMN_NAME_LOCAL_ACC_ID = "local_acc_id";
    private static final String DB_TRACKS_COLUMN_NAME_GLOBAL_ACC_ID = "global_acc_id";
    private static final String DB_TRACKS_COLUMN_NAME_DIALOG_ID = "dialog_id";
    private static final String DB_TRACKS_COLUMN_NAME_MESSAGE_ID = "message_id";
    private static final String DB_TRACKS_COLUMN_NAME_MUSIC_TITLE = "title";
    private static final String DB_TRACKS_COLUMN_NAME_MUSIC_FILE_NAME = "file_name";
    private static final String DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER = "performer";
    private static final String DB_TRACKS_COLUMN_NAME_DURATION_SEC = "duration_in_seconds";


    //PLAYLIST TABLE
    private static final String DB_PLAYLISTS_TABLE_NAME = "playlists";
    private static final String DB_PLAYLISTS_COLUMN_NAME_UID = "uid";
    private static final String DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME = "playlist_name";


    //PLAYLIST_TRACKS TABLE
    private static final String DB_PT_TABLE_NAME = "playlist_tracks"; //PT is short playlist_tracks
    private static final String DB_PT_COLUMN_NAME_PLAYLIST_UID = "playlist_uid";
    private static final String DB_PT_COLUMN_NAME_TRACK_UID = "track_uid";

    //RECENT_PLAYLIST TABLE
    private static final String DB_RECENT_PLAYLIST_TABLE_NAME = "user_recent_playlists";
    private static final String DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID = "playlist_uid";
    private static final String DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT = "updated_at";
}
