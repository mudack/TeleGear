package org.telegram.messenger.extended_music_player;

public interface ResultCallback<T> {
    void onResult(T result);
    void onError(Exception e);
}