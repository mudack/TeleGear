package org.telegram.messenger.extended_music_player;

import java.util.ArrayList;

public interface GlobalMusicDatabase {
    void createPlaylist(String name);
    ArrayList<Playlist> getAllPlayLists();
    Playlist getPlaylistById(int id);

    void addTrackToPlaylist(long playlistId, long trackId);
    ArrayList<MusicData> getTracksByPlaylistId(int id);
}