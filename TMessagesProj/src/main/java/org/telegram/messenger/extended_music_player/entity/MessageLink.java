package org.telegram.messenger.extended_music_player.entity;

import androidx.annotation.NonNull;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.extended_music_player.entity.music.adapters.message_object.MessageObjectAdapter;
import org.telegram.messenger.extended_music_player.entity.music.adapters.message_object.MusicMessageObjectAdapterInterface;
import org.telegram.tgnet.TLRPC;

public class MessageLink {
    private final long globalAccountId;
    private final int localAccountId;
    private final long dialogId; //i would prefer to call it as a chatId but whole telegram project call it "dialogId" so i better leave it like that
    private final int messageId;

    public MessageLink(MusicMessageObjectAdapterInterface messageSource) {
        MessageLink msgLink = messageSource.getMessageLink();
        this.localAccountId = msgLink.getLocalAccountId();
        this.dialogId = msgLink.getDialogId();
        this.messageId = msgLink.getMessageId();
        this.globalAccountId = msgLink.getGlobalAccountId();
    }

    public MessageLink(MessageObject messageObject) {
        this.localAccountId = messageObject.currentAccount;
        this.dialogId = messageObject.getDialogId();
        this.messageId = messageObject.getId();
        this.globalAccountId = UserConfig.getInstance(localAccountId).getClientUserId();
    }

    public MessageLink(long globalAccountId, int localAccountId, long dialogId, int messageId) {
        this.globalAccountId = globalAccountId;
        this.localAccountId = localAccountId;
        this.dialogId = dialogId;
        this.messageId = messageId;
    }

    public long getGlobalAccountId() {
        return globalAccountId;
    }

    public int getLocalAccountId() {
        return localAccountId;
    }

    public long getDialogId() {
        return dialogId;
    }

    public int getMessageId() {
        return messageId;
    }

    public MessageObject getMessageObject(){
        int accNum = this.localAccountId;
        MessagesStorage messagesStorage = MessagesStorage.getInstance(accNum); //account id is a part of path to the music

        //todo wrap the tlrpcMessage in try catch
        TLRPC.Message tlrpcMessage = messagesStorage.getMessage(this);

        //todo wrap the MessageObject constructor in try catch and impl fallback in case there is no way to get access to message and impl show message and make color of this message/music red
        MessageObject result = new MessageObject(accNum, tlrpcMessage, false, false);

        return result;
    }

    public MusicMessageObjectAdapterInterface getMessageSource(){
        MessageObject messageObject = getMessageObject();
        MusicMessageObjectAdapterInterface messageSource = new MessageObjectAdapter(messageObject);
        return messageSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageLink that = (MessageLink) o;
        return localAccountId == that.localAccountId &&
                globalAccountId == that.globalAccountId &&
                dialogId == that.dialogId &&
                messageId == that.messageId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(globalAccountId, localAccountId, dialogId, messageId);
    }

    @NonNull
    @Override
    public String toString() {
        return "MessageLink{" +
                "globalAccountId=" + globalAccountId +
                ", localAccountId=" + localAccountId +
                ", chatId=" + dialogId +
                ", messageId=" + messageId +
                '}';
    }

}