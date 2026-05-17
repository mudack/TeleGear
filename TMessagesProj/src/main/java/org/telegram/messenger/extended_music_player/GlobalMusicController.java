package org.telegram.messenger.extended_music_player;

import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

public interface GlobalMusicController {
    void addListener(RecentPlaylistListener recentPlaylistListener);
    void removeListener(RecentPlaylistListener recentPlaylistListener);
    void createPlaylist(String name);
    void getAllPlaylists(); //should emit ArrayList<Playlist>
    void addMusicToPlaylist(Playlist playlistId, MusicData music);
    void getMusicsByPlaylistId(int playlistId); //should emit ArrayList<MessageLink>


    void addNewAccIdsMapping(int localUserId, long mtprotoUserId);
    void getLocalUserIdByMtprotoId(long mtprotoId);
    void removeAccIdsMappingByLocalId(int localId);
}
