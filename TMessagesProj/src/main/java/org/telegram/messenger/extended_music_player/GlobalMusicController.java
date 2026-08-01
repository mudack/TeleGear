package org.telegram.messenger.extended_music_player;

import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

import java.util.ArrayList;

public interface GlobalMusicController {
    interface CreatePlaylistAndAddMusicCallback {
        void onSuccess(Playlist playlist);
        void onPlaylistAlreadyExists();
        void onError(Exception e);
    }

    void addListener(RecentPlaylistListener recentPlaylistListener);
    void removeListener(RecentPlaylistListener recentPlaylistListener);
    void createPlaylist(String name);
    void createPlaylistAndAddMusic(String name, MusicData music, CreatePlaylistAndAddMusicCallback callback);
    void getAllPlaylists(); //should emit ArrayList<Playlist>
    void renamePlaylist(int playlistId, String name);
    void deletePlaylist(int playlistId);
    void updatePlaylistOrder(ArrayList<Integer> playlistIds);
    // TODO: Playlist screen needs playlist metadata for subtitles, especially track count.
    // Add a controller/database API that emits id, name, and track count together instead of
    // duplicating playlist state or calculating per-row counts inside UI code.
    void addMusicToPlaylist(Playlist playlistId, MusicData music);
    // Emits playlistId and ArrayList<ResolvedMusicData> through musicReceiveMusicFromPlaylist.
    void getMusicsByPlaylistId(int playlistId);


    void addAccountIdMapping(int localAccountId, long mtprotoAccountId);
    void getLocalAccountIdByMtprotoAccountId(long mtprotoAccountId);
    void removeAccountIdMappingByLocalId(int localAccountId);
}
