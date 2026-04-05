package org.telegram.messenger.extended_music_player.entity.music.adapters.message_object;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;

public class MessageObjectAdapter implements MusicMessageObjectAdapterInterface {
    private final MessageObject messageObject;

    public MessageObjectAdapter(MessageObject msg) {
        this.messageObject = msg;
    }

    @Override
    public boolean isMusic() {
        return messageObject.isMusic();
    }

    @Override
    public int getCurrentAccountId() {
        return messageObject.currentAccount;
    }

    @Override
    public long getDialogId() {
        return messageObject.getDialogId();
    }

    @Override
    public int getMessageId() {
        return messageObject.getId();
    }

    @Override
    public MusicMetaData getMusicMetadata() {
        return new MusicMetaData(messageObject.getDocument().attributes);
    }

}