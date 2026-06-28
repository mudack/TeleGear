package org.telegram.SQLite.extended_music_player.dao;

import static org.telegram.SQLite.extended_music_player.dao.PlaylistDao.DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME;
import static org.telegram.SQLite.extended_music_player.dao.PlaylistDao.DB_PLAYLISTS_COLUMN_NAME_UID;
import static org.telegram.SQLite.extended_music_player.dao.PlaylistDao.DB_PLAYLISTS_TABLE_NAME;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.extended_music_player.entity.Playlist;

import java.util.ArrayList;

public class RecentPlaylistDao {

    private final org.telegram.SQLite.SQLiteDatabase database;

    public RecentPlaylistDao(org.telegram.SQLite.SQLiteDatabase database) throws SQLiteException {
        this.database = database;

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_RECENT_PLAYLIST_TABLE_NAME + " (" +
                DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + " INTEGER NOT NULL, " +
                DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT + " INTEGER NOT NULL, " +
                " PRIMARY KEY (" + DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID + ")" +
                ")").stepThis().dispose();
    }

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

    public ArrayList<Playlist> getRecentPlaylists(int limit) throws SQLiteException {
        return getRecentPlaylistsByOffset(0, limit);
    }

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

    public void clearRecentPlaylists() throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "DELETE FROM " + DB_RECENT_PLAYLIST_TABLE_NAME
        );

        state.requery();
        state.step();
        state.dispose();
    }


    //RECENT_TRACKS TABLE
    public static final String DB_RECENT_PLAYLIST_TABLE_NAME = "user_recent_playlists";
    public static final String DB_RECENT_PLAYLIST_COLUMN_NAME_PLAYLIST_UID = "playlist_uid";
    public static final String DB_RECENT_PLAYLIST_COLUMN_NAME_UPDATED_AT = "updated_at";
}
