package org.telegram.messenger.extended_music_player;

import org.telegram.messenger.extended_music_player.entity.Playlist;

import java.util.ArrayList;

public interface RecentPlaylistListener {
    void onStateChanged(RecentPlaylistState state, ArrayList<Playlist> data);
}