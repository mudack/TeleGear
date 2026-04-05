package org.telegram.messenger.extended_music_player;

import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

import java.util.ArrayList;

public interface GlobalMusicController {
    void createPlaylist(String name);
    void getAllPlaylists(); //should emit ArrayList<Playlist>
    void addMusicToPlaylist(Playlist playlistId, MusicData music);
    void getMusicsByPlaylistId(int playlistId); //should emit ArrayList<MessageLink>
    ArrayList<Playlist> getRecentPlaylist(); //should emit ArrayList<MessageLink>
}
