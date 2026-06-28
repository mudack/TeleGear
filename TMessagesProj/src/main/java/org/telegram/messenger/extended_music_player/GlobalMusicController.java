package org.telegram.messenger.extended_music_player;

import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

import java.util.ArrayList;

public interface GlobalMusicController {
    void addListener(RecentPlaylistListener recentPlaylistListener);
    void removeListener(RecentPlaylistListener recentPlaylistListener);
    void createPlaylist(String name);
    void getAllPlaylists(); //should emit ArrayList<Playlist>
    // TODO: Playlist screen needs playlist metadata for subtitles, especially track count.
    // Add a controller/database API that emits id, name, and track count together instead of
    // duplicating playlist state or calculating per-row counts inside UI code.
    void addMusicToPlaylist(Playlist playlistId, MusicData music);
    void getMusicsByPlaylistId(int playlistId); //should emit ArrayList<MessageLink>
}
