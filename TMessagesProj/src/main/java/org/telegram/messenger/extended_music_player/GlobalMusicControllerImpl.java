package org.telegram.messenger.extended_music_player;

import static org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseImpl.GLOBAL_MUSIC_DB_FILE_NAME;

import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabase;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseImpl;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.stream.Collectors;


public class GlobalMusicControllerImpl implements GlobalMusicController {

    private static volatile GlobalMusicControllerImpl Instance;
    private final DispatchQueue storageQueue = new DispatchQueue(DISPATCH_STORAGE_QUEUE_NAME);
    private GlobalMusicDatabase database;

    private final Deque<Playlist> recentPlaylists = new ArrayDeque<>(MAX_AMOUNT_OF_RECENT_PLAYLIST);

    public static GlobalMusicControllerImpl getInstance() {
        GlobalMusicControllerImpl localInstance = Instance;
        if (localInstance == null) {
            synchronized (GlobalMusicControllerImpl.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new GlobalMusicControllerImpl();
                }
            }
        }
        return localInstance;
    }

    private GlobalMusicControllerImpl() {
        storageQueue.postRunnable(() -> {
            try {
                File filesDir = ApplicationLoader.getFilesDirFixed();
                File dbFile = new File(filesDir, GLOBAL_MUSIC_DB_FILE_NAME);
                database = new GlobalMusicDatabaseImpl(dbFile);
            } catch (SQLiteException e) {
                FileLog.e(e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void createPlaylist(String name) {
        storageQueue.postRunnable(() -> {
            try {
                Playlist insertedPlaylist = database.createPlaylist(name);

                AndroidUtilities.runOnUIThread(() -> {
                    if (recentPlaylists.size() < MAX_AMOUNT_OF_RECENT_PLAYLIST) {
                        addPlaylistToRecent(insertedPlaylist);
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicPlaylistCreated,
                            name
                    );
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't create playlist with name: " + name + " due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void getAllPlaylists() {
        storageQueue.postRunnable(() -> {
            try {
                ArrayList<Playlist> playlists = database.getAllPlaylists();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicLoadListOfPlaylist,
                            playlists
                    );
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't get all playlists due " + e.getMessage()
                    );
                });
            }
        });
    }

    private void getPlaylistById(int playlistId, ResultCallback<Playlist> resultCallback) {
        storageQueue.postRunnable(() -> {
            try {
                Playlist playlist = database.getPlaylistById(playlistId);
                AndroidUtilities.runOnUIThread(() -> {
                    resultCallback.onResult(playlist);
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    resultCallback.onError(e);
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't get playlist entity by id: " + playlistId + " due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void addMusicToPlaylist(Playlist playlist, MusicData music) {
        storageQueue.postRunnable(() -> {
            try {
                database.addMusicToPlaylist(playlist.getId(), music);
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicAddedToPlaylist,
                            playlist.getName(),
                            music.getMusicMetaData().getTitle()
                    );
                    addPlaylistToRecent(playlist);
                });

            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't add music with title " + music.getMusicMetaData().getTitle() + " to playlist with id: " + playlist + " due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void getMusicsByPlaylistId(int playlistId) {
        storageQueue.postRunnable(() -> {
            try {
                ArrayList<MessageLink> musicLinksInPlaylist = database.getMusicLinksByPlaylistId(playlistId);
                ArrayList<MusicData> musics = musicLinksInPlaylist.stream().map(MusicData::new).collect(Collectors.toCollection(ArrayList::new));
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicReceiveMusicFromPlaylist,
                            musics
                    );
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't receive musics from playlist with id: " + playlistId + " due " + e.getMessage()
                    );
                });
            }
        });
    }

    private void addPlaylistToRecent(Playlist playlist) {
        if (recentPlaylists.remove(playlist)) {
            recentPlaylists.addFirst(playlist);
        } else {
            if (recentPlaylists.size() >= MAX_AMOUNT_OF_RECENT_PLAYLIST) {
                recentPlaylists.removeLast();
            }
            recentPlaylists.addFirst(playlist);
        }
    }

    @Override
    public ArrayList<Playlist> getRecentPlaylist() {
        return new ArrayList<Playlist>(recentPlaylists);
    }


    private static final String DISPATCH_STORAGE_QUEUE_NAME = "global_music_queue";
    private static final int MAX_AMOUNT_OF_RECENT_PLAYLIST = 3;
}