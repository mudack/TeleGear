package org.telegram.SQLite.extended_music_player.dao;

import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;

public class PlaylistTrackDao {
    private final org.telegram.SQLite.SQLiteDatabase database;

    public PlaylistTrackDao(org.telegram.SQLite.SQLiteDatabase database) throws SQLiteException {
        this.database = database;

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PT_TABLE_NAME + " (" +
                DB_PT_COLUMN_NAME_PLAYLIST_UID + " INTEGER NOT NULL, " +
                DB_PT_COLUMN_NAME_TRACK_UID + " INTEGER NOT NULL, " +
                " PRIMARY KEY (" + DB_PT_COLUMN_NAME_PLAYLIST_UID + ", " + DB_PT_COLUMN_NAME_TRACK_UID + ")" +
                ")").stepThis().dispose();
    }


    public void addMusicToPlaylistById(int playlistId, Integer musicId) throws SQLiteException {
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

    public void removeMusicLinksByPlaylistId(int playlistId) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "DELETE FROM " + DB_PT_TABLE_NAME +
                        " WHERE " + DB_PT_COLUMN_NAME_PLAYLIST_UID + " = ?"
        );

        state.requery();
        state.bindInteger(1, playlistId);
        state.step();
        state.dispose();
    }

    //PLAYLIST_TRACKS TABLE
    static final String DB_PT_TABLE_NAME = "playlist_tracks"; //PT is short playlist_tracks
    static final String DB_PT_COLUMN_NAME_PLAYLIST_UID = "playlist_uid";
    static final String DB_PT_COLUMN_NAME_TRACK_UID = "track_uid";
}
