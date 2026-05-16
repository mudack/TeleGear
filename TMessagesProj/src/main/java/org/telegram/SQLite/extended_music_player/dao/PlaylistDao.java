package org.telegram.SQLite.extended_music_player.dao;

import static org.telegram.SQLite.extended_music_player.dao.RecentPlaylistDao.DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID;
import static org.telegram.SQLite.extended_music_player.dao.RecentPlaylistDao.DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT;
import static org.telegram.SQLite.extended_music_player.dao.RecentPlaylistDao.DB_RECENT_PLAYLIST_TABLE_NAME;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.extended_music_player.entity.Playlist;

import java.util.ArrayList;

public class PlaylistDao {

    private final org.telegram.SQLite.SQLiteDatabase database;

    public PlaylistDao(org.telegram.SQLite.SQLiteDatabase database) throws SQLiteException {
        this.database = database;

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PLAYLISTS_TABLE_NAME + " (" +
                DB_PLAYLISTS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + " TEXT NOT NULL, " +
                " UNIQUE(" + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + ")" +
                ")").stepThis().dispose();

    }


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

    //PLAYLIST TABLE
    public static final String DB_PLAYLISTS_TABLE_NAME = "playlists";
    public static final String DB_PLAYLISTS_COLUMN_NAME_UID = "uid";
    public static final String DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME = "playlist_name";
}
