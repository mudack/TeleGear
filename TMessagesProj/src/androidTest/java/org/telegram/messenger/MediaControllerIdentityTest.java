package org.telegram.messenger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.telegram.tgnet.TLRPC;

public class MediaControllerIdentityTest {

    @Test
    public void sameChannelPostFromDifferentAccounts_isNotSameMessage() {
        MessageObject account0Message = createMessage(0, 777L, 100);
        MessageObject account1Message = createMessage(1, 777L, 100);

        assertFalse(MediaController.isSameMessageIdentity(account0Message, account1Message));
    }

    @Test
    public void sameChannelPostFromSameAccount_isSameMessage() {
        MessageObject first = createMessage(0, 777L, 100);
        MessageObject second = createMessage(0, 777L, 100);

        assertTrue(MediaController.isSameMessageIdentity(first, second));
    }

    @Test
    public void differentPostFromSameAccount_isNotSameMessage() {
        MessageObject first = createMessage(0, 777L, 100);
        MessageObject second = createMessage(0, 777L, 101);

        assertFalse(MediaController.isSameMessageIdentity(first, second));
    }

    private MessageObject createMessage(int account, long channelId, int messageId) {
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = messageId;
        message.date = 1;
        message.message = "";

        TLRPC.TL_peerChannel peer = new TLRPC.TL_peerChannel();
        peer.channel_id = channelId;
        message.peer_id = peer;

        return new MessageObject(account, message, false, false);
    }
}
