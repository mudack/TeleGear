package org.telegram.messenger.extended_music_player;

import androidx.annotation.NonNull;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.TLRPC;

public class MessageLink {
    public final int accountId;
    public final long dialogId; //i would prefer to call it as a chatId but whole telegram project call it "dialogId" so i better leave it like that
    public final int messageId;

    public MessageLink(MessageObject messageObject) {
        this.accountId = messageObject.currentAccount;
        this.dialogId = messageObject.getDialogId();
        this.messageId = messageObject.getId();
    }

    public int getAccountId() {
        return accountId;
    }

    public long getDialogId() {
        return dialogId;
    }

    public int getMessageId() {
        return messageId;
    }

    public MessageObject getMessageObject(){
        int accNum = this.accountId;
        MessagesStorage messagesStorage = MessagesStorage.getInstance(accNum); //account id is a part of path to the music

        TLRPC.Message tlrpcMessage = messagesStorage.getMessage(this);
        return new MessageObject(accNum, tlrpcMessage, false, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageLink that = (MessageLink) o;
        return accountId == that.accountId &&
                dialogId == that.dialogId &&
                messageId == that.messageId;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(accountId, dialogId, messageId);
    }

    @NonNull
    @Override
    public String toString() {
        return "MessageLink{" +
                "accountId=" + accountId +
                ", chatId=" + dialogId +
                ", messageId=" + messageId +
                '}';
    }
}