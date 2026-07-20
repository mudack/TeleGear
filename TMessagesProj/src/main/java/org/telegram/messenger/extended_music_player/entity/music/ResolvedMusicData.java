package org.telegram.messenger.extended_music_player.entity.music;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.MessageObject;

/**
 * A playlist track with its persisted metadata and an optional live Telegram message.
 *
 * <p>The database can retain a playlist entry after its source message is no longer
 * available in the currently logged-in accounts. In that case {@link #getMessageObject()}
 * is {@code null}, while {@link #getMusicData()} remains usable for a fallback row.</p>
 */
public class ResolvedMusicData {

    @NonNull
    private final MusicData musicData;
    @Nullable
    private final MessageObject messageObject;

    public ResolvedMusicData(@NonNull MusicData musicData, @Nullable MessageObject messageObject) {
        this.musicData = musicData;
        this.messageObject = messageObject;
    }

    @NonNull
    public MusicData getMusicData() {
        return musicData;
    }

    @Nullable
    public MessageObject getMessageObject() {
        return messageObject;
    }
}
