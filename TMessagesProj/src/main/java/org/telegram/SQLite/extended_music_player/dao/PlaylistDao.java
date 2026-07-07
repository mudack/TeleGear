package org.telegram.SQLite.extended_music_player.dao;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.extended_music_player.entity.Playlist;

import java.util.ArrayList;
import java.util.HashSet;

public class PlaylistDao {

    private final org.telegram.SQLite.SQLiteDatabase database;

    public PlaylistDao(org.telegram.SQLite.SQLiteDatabase database) throws SQLiteException {
        this.database = database;

        database.executeFast("CREATE TABLE IF NOT EXISTS " + DB_PLAYLISTS_TABLE_NAME + " (" +
                DB_PLAYLISTS_COLUMN_NAME_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + " TEXT NOT NULL, " +
                DB_PLAYLISTS_COLUMN_NAME_ORDER_INDEX + " INTEGER NOT NULL, " +
                " UNIQUE(" + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + ")" +
                ")").stepThis().dispose();

    }


    public Playlist createPlaylist(String name) throws SQLiteException {
        String playlistName = validatePlaylistName(name);
        int orderIndex = getNextOrderIndex();
        SQLitePreparedStatement state = database.executeFast(
                "INSERT OR IGNORE INTO " + DB_PLAYLISTS_TABLE_NAME + " (" +
                        DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + ", " +
                        DB_PLAYLISTS_COLUMN_NAME_ORDER_INDEX +
                        ") VALUES (?, ?)"
        );

        state.requery();
        state.bindString(1, playlistName);
        state.bindInteger(2, orderIndex);
        state.step();
        state.dispose();

        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_PLAYLISTS_COLUMN_NAME_UID + ", " +
                        DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + "=?",
                playlistName
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
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_PLAYLISTS_COLUMN_NAME_UID + ", " +
                        DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME +
                        " ORDER BY " + DB_PLAYLISTS_COLUMN_NAME_ORDER_INDEX + " ASC, " +
                        DB_PLAYLISTS_COLUMN_NAME_UID + " ASC"
        );
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
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_PLAYLISTS_COLUMN_NAME_UID + ", " +
                        DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_UID + "=?",
                id
        );
        if (cursor.next()) {
            playlist = new Playlist(
                    cursor.intValue(0),
                    cursor.stringValue(1)
            );
        }
        cursor.dispose();
        return playlist;
    }

    public Playlist renamePlaylist(int id, String name) throws SQLiteException {
        String playlistName = validatePlaylistName(name);
        Playlist currentPlaylist = requirePlaylistExists(id);
        if (playlistName.equals(currentPlaylist.getName())) {
            return currentPlaylist;
        }
        if (playlistNameExists(playlistName, id)) {
            throw new SQLiteException("Playlist with name already exists: " + playlistName);
        }

        SQLitePreparedStatement state = database.executeFast(
                "UPDATE " + DB_PLAYLISTS_TABLE_NAME +
                        " SET " + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + " = ?" +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_UID + " = ?"
        );

        state.requery();
        state.bindString(1, playlistName);
        state.bindInteger(2, id);
        state.step();
        state.dispose();

        return new Playlist(id, playlistName);
    }

    public void deletePlaylist(int id) throws SQLiteException {
        requirePlaylistExists(id);
        SQLitePreparedStatement state = database.executeFast(
                "DELETE FROM " + DB_PLAYLISTS_TABLE_NAME +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_UID + " = ?"
        );

        state.requery();
        state.bindInteger(1, id);
        state.step();
        state.dispose();
    }

    public void validatePlaylistOrder(ArrayList<Integer> playlistIds) throws SQLiteException {
        if (playlistIds == null) {
            throw new SQLiteException("Playlist order can't be null");
        }

        ArrayList<Integer> savedPlaylistIds = getAllPlaylistIds();
        if (playlistIds.size() != savedPlaylistIds.size()) {
            throw new SQLiteException("Playlist order should contain every playlist id");
        }

        HashSet<Integer> savedIds = new HashSet<>(savedPlaylistIds);
        HashSet<Integer> requestedIds = new HashSet<>();
        for (int i = 0; i < playlistIds.size(); i++) {
            Integer playlistId = playlistIds.get(i);
            if (playlistId == null || !savedIds.contains(playlistId) || !requestedIds.add(playlistId)) {
                throw new SQLiteException("Invalid playlist id in order");
            }
        }
    }

    public void updatePlaylistOrder(ArrayList<Integer> playlistIds) throws SQLiteException {
        SQLitePreparedStatement state = database.executeFast(
                "UPDATE " + DB_PLAYLISTS_TABLE_NAME +
                        " SET " + DB_PLAYLISTS_COLUMN_NAME_ORDER_INDEX + " = ?" +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_UID + " = ?"
        );

        for (int i = 0; i < playlistIds.size(); i++) {
            state.requery();
            state.bindInteger(1, i);
            state.bindInteger(2, playlistIds.get(i));
            state.step();
        }
        state.dispose();
    }

    public Playlist requirePlaylistExists(int id) throws SQLiteException {
        Playlist playlist = getPlaylistById(id);
        if (playlist == null) {
            throw new SQLiteException("Playlist doesn't exist: " + id);
        }
        return playlist;
    }

    private String validatePlaylistName(String name) throws SQLiteException {
        String playlistName = name == null ? "" : name.trim();
        if (playlistName.isEmpty()) {
            throw new SQLiteException("Playlist name can't be empty");
        }
        return playlistName;
    }

    private boolean playlistNameExists(String name, int exceptId) throws SQLiteException {
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_PLAYLISTS_COLUMN_NAME_UID +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME +
                        " WHERE " + DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME + " = ?" +
                        " AND " + DB_PLAYLISTS_COLUMN_NAME_UID + " != ?",
                name,
                exceptId
        );
        boolean exists = cursor.next();
        cursor.dispose();
        return exists;
    }

    private int getNextOrderIndex() throws SQLiteException {
        Integer result = database.executeInt(
                "SELECT COALESCE(MAX(" + DB_PLAYLISTS_COLUMN_NAME_ORDER_INDEX + "), -1) + 1" +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME
        );
        return result == null ? 0 : result;
    }

    private ArrayList<Integer> getAllPlaylistIds() throws SQLiteException {
        ArrayList<Integer> result = new ArrayList<>();
        SQLiteCursor cursor = database.queryFinalized(
                "SELECT " + DB_PLAYLISTS_COLUMN_NAME_UID +
                        " FROM " + DB_PLAYLISTS_TABLE_NAME
        );
        while (cursor.next()) {
            result.add(cursor.intValue(0));
        }
        cursor.dispose();
        return result;
    }

    //PLAYLIST TABLE
    public static final String DB_PLAYLISTS_TABLE_NAME = "playlists";
    public static final String DB_PLAYLISTS_COLUMN_NAME_UID = "uid";
    public static final String DB_PLAYLISTS_COLUMN_NAME_PLAYLIST_NAME = "playlist_name";
    public static final String DB_PLAYLISTS_COLUMN_NAME_ORDER_INDEX = "order_index";
}
