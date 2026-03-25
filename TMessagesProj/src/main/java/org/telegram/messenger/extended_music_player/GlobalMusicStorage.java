package org.telegram.messenger.extended_music_player;

import androidx.annotation.NonNull;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.util.ArrayList;

public class GlobalMusicStorage {

    private org.telegram.SQLite.SQLiteDatabase database;
    private final DispatchQueue storageQueue = new DispatchQueue(DISPATCH_STORAGE_QUEUE_NAME);

    public GlobalMusicStorage() {
        storageQueue.postRunnable(this::initDatabase);
    }

    private void initDatabase() {
        File filesDir = ApplicationLoader.getFilesDirFixed();
        File dbFile = new File(filesDir, GLOBAL_MUSIC_DB_FILE_NAME);

        try {
            database = new org.telegram.SQLite.SQLiteDatabase(dbFile.getPath());
            database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_TRACKS_TABLE_NAME + " (" +
                    DB_TRACKS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DB_TRACKS_COLUMN_NAME_ACC_ID + " INTEGER, " +
                    DB_TRACKS_COLUMN_NAME_DIALOG_ID + " INTEGER, " +
                    DB_TRACKS_COLUMN_NAME_MESSAGE_ID + " INTEGER, " +
                    DB_TRACKS_COLUMN_NAME_MUSIC_TITLE + " TEXT, " +
                    DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER + " TEXT, " +
                    DB_TRACKS_COLUMN_NAME_DURATION_SEC + " REAL, " +
                    " UNIQUE(" + DB_TRACKS_COLUMN_NAME_ACC_ID + ", " + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ")" +
                    ")").stepThis().dispose();

            database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PLAYLISTS_TABLE_NAME + " (" +
                    DB_PLAYLISTS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + " TEXT, " +
                    " UNIQUE(" + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + ")" +
                    ")").stepThis().dispose();

            database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PT_TABLE_NAME + " (" +
                    DB_PT_COLUMN_NAME_PLAYLIST_UID + " INTEGER, " +
                    DB_PT_COLUMN_NAME_TRACK_UID + " INTEGER, " +
                    " PRIMARY KEY (" + DB_PT_COLUMN_NAME_PLAYLIST_UID + ", " + DB_PT_COLUMN_NAME_TRACK_UID + ")" +
                    ")").stepThis().dispose();

        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void createPlaylist(String name) {
        storageQueue.postRunnable(() -> {
            try {
                SQLitePreparedStatement state = database.executeFast(
                        "INSERT INTO " + DB_PLAYLISTS_TABLE_NAME + " (" +
                                DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                                ") VALUES (?)"
                );

                state.requery();
                state.bindString(1, name);
                state.step();
                state.dispose();

            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void getAllPlaylists(@NonNull ResultCallback<ArrayList<Playlist>> callback) {
        storageQueue.postRunnable(() -> {
            ArrayList<Playlist> results = new ArrayList<>();
            try {
                SQLiteCursor cursor = database.queryFinalized("SELECT * FROM " + DB_PLAYLISTS_TABLE_NAME);
                while (cursor.next()) {
                    Playlist playlist = new Playlist(
                            cursor.intValue(0),
                            cursor.stringValue(1)
                    );
                    results.add(playlist);
                }
                cursor.dispose();

                AndroidUtilities.runOnUIThread(() -> callback.onResult(results));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> callback.onError(e));
            }
        });
    }

    public void getPlaylistById(int id, @NonNull ResultCallback<Playlist> callback) {
        storageQueue.postRunnable(() -> {
            try {
                Playlist playlist = null;
                SQLiteCursor cursor = database.queryFinalized("SELECT * FROM " + DB_PLAYLISTS_TABLE_NAME + " WHERE " + DB_PLAYLISTS_COLUMN_NAME_UID + "=" + id);
                while (cursor.next()) {
                    playlist = new Playlist(
                            cursor.intValue(0),
                            cursor.stringValue(1)
                    );
                }
                cursor.dispose();
                Playlist finalPlaylist = playlist;
                AndroidUtilities.runOnUIThread(() -> callback.onResult(finalPlaylist));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> callback.onError(e));
            }
        });
    }

    private void addTrackToLibrary(MusicData musicData, ResultCallback<Integer> callback) { //should be use in addTrackToPlaylist
        storageQueue.postRunnable(() -> {
            try {
                SQLitePreparedStatement state = database.executeFast(
                        "INSERT OR IGNORE INTO " + DB_TRACKS_TABLE_NAME + " (" +
                                DB_TRACKS_COLUMN_NAME_ACC_ID + ", " +
                                DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
                                DB_TRACKS_COLUMN_NAME_MESSAGE_ID + ", " +
                                DB_TRACKS_COLUMN_NAME_MUSIC_TITLE + ", " +
                                DB_TRACKS_COLUMN_NAME_MUSIC_PERFORMER + ", " +
                                DB_TRACKS_COLUMN_NAME_DURATION_SEC +
                                ") VALUES (?, ?, ?, ?, ?, ?)"
                );

                state.requery();

                MusicMetaData metaData = musicData.getMusicMetaData();
                org.telegram.messenger.extended_music_player.MessageLink link = musicData.getMessageLink();

                int accId = link.getAccountId();
                long dialogId = link.getDialogId();
                int msgId = link.getMessageId();

                state.bindInteger(1, accId);
                state.bindLong(2, dialogId);
                state.bindInteger(3, msgId);
                state.bindString(4, metaData.getTitle());
                state.bindString(5, metaData.getPerformer());
                state.bindDouble(6, metaData.getDurationSec());

                state.step();
                state.dispose();

                Integer trackIdByLink = database.executeInt("SELECT " + DB_TRACKS_COLUMN_NAME_UID + " FROM " + DB_TRACKS_TABLE_NAME + " WHERE " +
                        DB_TRACKS_COLUMN_NAME_ACC_ID + "=" + accId + " AND " +
                        DB_TRACKS_COLUMN_NAME_DIALOG_ID + "=" + dialogId + " AND " +
                        DB_TRACKS_COLUMN_NAME_MESSAGE_ID + "=" + msgId
                );
                if(callback!=null) {
                    callback.onResult(trackIdByLink);
                }
            } catch (Exception e) {
                FileLog.e(e);
                if (callback!=null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void addTrackToPlaylist(int playlistId, MusicData track) {
        addTrackToLibrary(track, new ResultCallback<Integer>() {
            @Override
            public void onResult(Integer trackId) {
                try {
                    SQLitePreparedStatement state = database.executeFast(
                            "INSERT OR IGNORE INTO " + DB_PT_TABLE_NAME + " (" +
                                    DB_PT_COLUMN_NAME_PLAYLIST_UID + ", " +
                                    DB_PT_COLUMN_NAME_TRACK_UID +
                                    ") VALUES (?, ?)"
                    );
                    state.requery();

                    state.bindInteger(1, playlistId);
                    state.bindInteger(2, trackId);

                    state.step();
                    state.dispose();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            @Override
            public void onError(Exception e) {
                FileLog.e(e);
            }
        });
    }

    public void getTracksLinkByPlaylistId(int playlistId, @NonNull ResultCallback<ArrayList<MessageLink>> callback) {
        storageQueue.postRunnable(() -> {
            ArrayList<MessageLink> result = new ArrayList<>();

            try {
                SQLiteCursor cursor = database.queryFinalized(
                        "SELECT t." + DB_TRACKS_COLUMN_NAME_ACC_ID + ", " +
                                "t." + DB_TRACKS_COLUMN_NAME_DIALOG_ID + ", " +
                                "t." + DB_TRACKS_COLUMN_NAME_MESSAGE_ID + " " +
                                "FROM " + DB_TRACKS_TABLE_NAME + " t " +
                                "INNER JOIN " + DB_PT_TABLE_NAME + " pt ON t." + DB_TRACKS_COLUMN_NAME_UID + " = pt." + DB_PT_COLUMN_NAME_TRACK_UID + " " +
                                "WHERE pt." + DB_PT_COLUMN_NAME_PLAYLIST_UID + " = ?",
                        playlistId
                );

                while (cursor.next()) {
                    /* documentation
                        0 - DB_TRACKS_COLUMN_NAME_ACC_ID
                        1 - DB_TRACKS_COLUMN_NAME_DIALOG_ID
                        2 - DB_TRACKS_COLUMN_NAME_MESSAGE_ID*/
                    int accId = cursor.intValue(0);
                    long dialogId = cursor.longValue(1);
                    int messageId = cursor.intValue(2);

                    try {
                        MessageLink link = new MessageLink(accId, dialogId, messageId);
                        result.add(link);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }

                cursor.dispose();

                AndroidUtilities.runOnUIThread(() -> callback.onResult(result));

            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.runOnUIThread(() -> callback.onError(e));
            }
        });
    }

    private static final String DISPATCH_STORAGE_QUEUE_NAME = "global_music_queue";
    private static final String GLOBAL_MUSIC_DB_FILE_NAME = "global_music.db";

    //TRACKS TABLE
    private static final String DB_TRACKS_TABLE_NAME = "tracks";
    private static final String DB_TRACKS_COLUMN_NAME_UID = "uid";
    private static final String DB_TRACKS_COLUMN_NAME_ACC_ID = "acc_id";
    private static final String DB_TRACKS_COLUMN_NAME_DIALOG_ID = "dialog_id";
    private static final String DB_TRACKS_COLUMN_NAME_MESSAGE_ID = "message_id";
    private static final String DB_TRACKS_COLUMN_NAME_MUSIC_TITLE = "title";
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
}
