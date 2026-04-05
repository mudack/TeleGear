package org.telegram.messenger.extended_music_player.entity;

public class Playlist {
    private final int id;
    private final String name;

    public Playlist(int id, String name){
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
