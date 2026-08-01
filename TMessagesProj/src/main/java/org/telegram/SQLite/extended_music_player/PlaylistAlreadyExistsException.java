package org.telegram.SQLite.extended_music_player;

import org.telegram.SQLite.SQLiteException;

public class PlaylistAlreadyExistsException extends SQLiteException {

    public PlaylistAlreadyExistsException(String name) {
        super("Playlist already exists: " + name);
    }
}
