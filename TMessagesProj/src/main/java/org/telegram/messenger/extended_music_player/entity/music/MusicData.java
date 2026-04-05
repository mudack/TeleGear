package org.telegram.messenger.extended_music_player.entity.music;

import androidx.annotation.NonNull;

import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.music.adapters.message_object.MusicMessageObjectAdapterInterface;

import java.util.Objects;

public class MusicData {

    private final MessageLink messageLink;
    private final MusicMetaData musicMetaData;

    public MusicData(@NonNull MusicMessageObjectAdapterInterface messageSource) {
        this(messageSource, new MessageLink(messageSource));
    }

    public MusicData(@NonNull MessageLink messageLink) {
        this(messageLink.getMessageSource(), messageLink);
    }

    public MusicData(@NonNull MessageLink messageLink, @NonNull MusicMetaData musicMetaData){
        this.messageLink = messageLink;
        this.musicMetaData = musicMetaData;
    }

    private MusicData(MusicMessageObjectAdapterInterface messageObject, MessageLink messageLink) {
        if (!messageObject.isMusic())
            throw new IllegalArgumentException("messageObject type is not Music");

        this.messageLink = messageLink;

        this.musicMetaData = messageObject.getMusicMetadata();
    }


    public MusicMetaData getMusicMetaData() {
        return musicMetaData;
    }

    public MessageLink getMessageLink() {
        return messageLink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicData that = (MusicData) o;
        return Objects.equals(messageLink, that.messageLink) &&
                Objects.equals(musicMetaData, that.musicMetaData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageLink, musicMetaData);
    }

    @NonNull
    @Override
    public String toString() {
        return "MusicData{" +
                "messageLink=" + messageLink +
                ", musicMetaData=" + musicMetaData +
                '}';
    }
}
