package org.telegram.SQLite.extended_music_player;

import org.telegram.SQLite.SQLiteException;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

import java.util.ArrayList;

public interface GlobalMusicDatabaseRepo {
    Playlist createPlaylist(String name) throws SQLiteException;
    ArrayList<Playlist> getAllPlaylists() throws SQLiteException;
    Playlist getPlaylistById(int id) throws SQLiteException;
    Playlist renamePlaylist(int id, String name) throws SQLiteException;
    void deletePlaylist(int id) throws SQLiteException;
    void updatePlaylistOrder(ArrayList<Integer> playlistIds) throws SQLiteException;
    Integer addMusicToLibrary(MusicData music) throws SQLiteException;
    MusicData getMusicFromLibraryById(int id) throws SQLiteException;
    ArrayList<MusicData> getAllMusicFromLibrary() throws SQLiteException;
    void addMusicToPlaylist(int playlistId, MusicData music) throws SQLiteException;
    ArrayList<MusicData> getMusicDataByPlaylistId(int playlistId) throws SQLiteException;
    ArrayList<MessageLink> getMusicLinksByPlaylistId(int playlistId) throws SQLiteException;

    void markPlaylistAsRecent(int playlistId, long timestamp) throws SQLiteException;
    ArrayList<Playlist> getRecentPlaylists(int limit) throws SQLiteException;
    ArrayList<Playlist> getRecentPlaylistsByOffset(int offset, int limit) throws SQLiteException;
    void removeRecentPlaylist(int playlistId) throws SQLiteException;
    void clearRecentPlaylists() throws SQLiteException;

    void addAccountIdMapping(int localAccountId, long mtprotoAccountId) throws SQLiteException;
    int getLocalAccountIdByMtprotoAccountId(long mtprotoAccountId) throws SQLiteException;
    void removeAccountIdMappingByLocalId(int localAccountId) throws SQLiteException;
}
