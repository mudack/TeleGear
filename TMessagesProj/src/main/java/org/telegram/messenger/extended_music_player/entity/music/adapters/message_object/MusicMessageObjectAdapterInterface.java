package org.telegram.messenger.extended_music_player.entity.music.adapters.message_object;

import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;

public interface MusicMessageObjectAdapterInterface {
    boolean isMusic();
    int getCurrentAccountId();
    long getDialogId();
    int getMessageId();
    MusicMetaData getMusicMetadata();
}
