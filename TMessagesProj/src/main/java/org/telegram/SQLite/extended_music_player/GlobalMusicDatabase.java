package org.telegram.SQLite.extended_music_player;

import org.telegram.SQLite.SQLiteException;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;
import org.telegram.messenger.extended_music_player.entity.Playlist;

import java.util.ArrayList;

public interface GlobalMusicDatabase {
    Playlist createPlaylist(String name) throws SQLiteException;
    ArrayList<Playlist> getAllPlaylists() throws SQLiteException;
    Playlist getPlaylistById(int id) throws SQLiteException;
    Integer addMusicToLibrary(MusicData music) throws SQLiteException;
    MusicData getMusicFromLibraryById(int id) throws SQLiteException;
    ArrayList<MusicData> getAllMusicFromLibrary() throws SQLiteException;
    void addMusicToPlaylist(int playlistId, MusicData music) throws SQLiteException;
    ArrayList<MessageLink> getMusicLinksByPlaylistId(int playlistId) throws SQLiteException;
}