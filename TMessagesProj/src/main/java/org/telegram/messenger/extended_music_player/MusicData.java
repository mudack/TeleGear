package org.telegram.messenger.extended_music_player;

import androidx.annotation.NonNull;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Objects;

public class MusicData {

    private final MessageLink messageLink;
    private final MusicMetaData musicMetaData;

    public MusicData(@NonNull MessageObject messageObject) {
        if (!messageObject.isMusic())
            throw new IllegalArgumentException("messageObject type is not Music");
        TLRPC.Document document = messageObject.getDocument();
        this.messageLink = new MessageLink(messageObject);
        this.musicMetaData = new MusicMetaData(document.attributes);
    }

    public MusicData(@NonNull MessageLink messageLink, @NonNull MusicMetaData musicMetaData) {
        this.messageLink = messageLink;
        this.musicMetaData = musicMetaData;
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
