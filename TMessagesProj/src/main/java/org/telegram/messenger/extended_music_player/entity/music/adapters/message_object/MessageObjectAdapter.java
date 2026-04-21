package org.telegram.messenger.extended_music_player.entity.music.adapters.message_object;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
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
    public MessageLink getMessageLink() {
        return new MessageLink(messageObject);
    }

    @Override
    public MusicMetaData getMusicMetadata() {
        return new MusicMetaData(messageObject.getDocument().attributes);
    }

}