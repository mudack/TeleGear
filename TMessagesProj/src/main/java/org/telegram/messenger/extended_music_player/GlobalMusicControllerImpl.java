package org.telegram.messenger.extended_music_player;

import static org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepoImpl.GLOBAL_MUSIC_DB_FILE_NAME;

import androidx.annotation.UiThread;

import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepo;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepoImpl;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GlobalMusicControllerImpl implements GlobalMusicController {

    private static volatile GlobalMusicControllerImpl Instance;
    private final DispatchQueue storageQueue = new DispatchQueue(DISPATCH_STORAGE_QUEUE_NAME);
    private GlobalMusicDatabaseRepo databaseRepo;

    private final Deque<Playlist> recentPlaylists = new ArrayDeque<>(MAX_AMOUNT_OF_RECENT_PLAYLIST);
    private volatile RecentPlaylistState recentPlaylistsState = RecentPlaylistState.LOADING;
    private final List<RecentPlaylistListener> recentPlaylistListeners = new CopyOnWriteArrayList<>();

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
                databaseRepo = new GlobalMusicDatabaseRepoImpl(dbFile);

                ArrayList<Playlist> savedRecentPlaylist = databaseRepo.getRecentPlaylists(MAX_AMOUNT_OF_RECENT_PLAYLIST);

                AndroidUtilities.runOnUIThread(() -> {
                    recentPlaylists.clear();
                    recentPlaylists.addAll(savedRecentPlaylist);
                    recentPlaylistsState = RecentPlaylistState.READY;
                    for (RecentPlaylistListener l : recentPlaylistListeners) {
                        l.onStateChanged(recentPlaylistsState, getRecentPlaylist());
                    }
                });
            } catch (SQLiteException e) {

                AndroidUtilities.runOnUIThread(() -> {
                    recentPlaylistsState = RecentPlaylistState.ERROR;
                    for (RecentPlaylistListener l : recentPlaylistListeners) {
                        l.onStateChanged(recentPlaylistsState, getRecentPlaylist());
                    }
                });
                FileLog.e(e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void addListener(RecentPlaylistListener recentPlaylistListener) {
        AndroidUtilities.runOnUIThread(() -> {
            recentPlaylistListeners.add(recentPlaylistListener);
            recentPlaylistListener.onStateChanged(recentPlaylistsState, getRecentPlaylist());
        });
    }

    @Override
    public void removeListener(RecentPlaylistListener recentPlaylistListener) {
        AndroidUtilities.runOnUIThread(() -> {
            recentPlaylistListeners.remove(recentPlaylistListener);
        });
    }

    @Override
    public void createPlaylist(String name) {
        storageQueue.postRunnable(() -> {
            try {
                Playlist insertedPlaylist = databaseRepo.createPlaylist(name);

                AndroidUtilities.runOnUIThread(() -> {
//                    if (recentPlaylists.size() < MAX_AMOUNT_OF_RECENT_PLAYLIST) {
                    addPlaylistToRecent(insertedPlaylist);
//                    }
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
                ArrayList<Playlist> playlists = databaseRepo.getAllPlaylists();
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
                Playlist playlist = databaseRepo.getPlaylistById(playlistId);
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
                databaseRepo.addMusicToPlaylist(playlist.getId(), music);
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
                ArrayList<MessageLink> musicLinksInPlaylist = databaseRepo.getMusicLinksByPlaylistId(playlistId);
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

    @UiThread
    private void addPlaylistToRecent(Playlist playlist) {
        AndroidUtilities.runOnUIThread(() -> {
            if (recentPlaylists.remove(playlist)) {
                recentPlaylists.addFirst(playlist);
            } else {
                if (recentPlaylists.size() >= MAX_AMOUNT_OF_RECENT_PLAYLIST) {
                    recentPlaylists.removeLast();
                }
                recentPlaylists.addFirst(playlist);
            }
            int playlistId = playlist.getId();
            long timestamp = System.currentTimeMillis();
            storageQueue.postRunnable(() -> {
                try {
                    databaseRepo.markPlaylistAsRecent(playlistId, timestamp);
                } catch (SQLiteException e) {
                    FileLog.e(e);
                    e.printStackTrace();
                    AndroidUtilities.runOnUIThread(() -> {
                        NotificationCenter.getGlobalInstance().postNotificationName(
                                NotificationCenter.musicDatabaseError,
                                "Can't mark playlist as a recent one with id: " + playlistId + " due " + e.getMessage()
                        );
                    });
                }
            });
        });
    }

    @UiThread
    private ArrayList<Playlist> getRecentPlaylist() {
        return new ArrayList<Playlist>(recentPlaylists); //GC can be optimized
    }

    @Override
    public void addNewAccIdsMapping(int localUserId, long mtprotoUserId) {
        storageQueue.postRunnable(() -> {
            try {
                databaseRepo.addNewAccIdsMapping(localUserId, mtprotoUserId);
//                AndroidUtilities.runOnUIThread(() -> {
//                    NotificationCenter.getGlobalInstance().postNotificationName(
//                    );
//                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't do addNewAccIdsMapping with: localUserId = " + localUserId + ", mtprotoUserId = "+ mtprotoUserId +" due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void getLocalUserIdByMtprotoId(long mtprotoId) {
        storageQueue.postRunnable(() -> {
            try {
                long mtprotoUserId = databaseRepo.getAccIdsMappingByMtprotoId(mtprotoId);
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.mtprotoAccIdFromMappingTable,
                            mtprotoUserId
                    );
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't do getLocalUserIdByMtprotoId with: mtprotoUserId = "+ mtprotoId +" due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void removeAccIdsMappingByLocalId(int localId) {
        storageQueue.postRunnable(() -> {
            try {
                databaseRepo.removeAccIdsMappingByLocalId(localId);
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't do removeAccIdsMappingByLocalId with: localId = "+ localId +" due " + e.getMessage()
                    );
                });
            }
        });
    }

    private static final String DISPATCH_STORAGE_QUEUE_NAME = "global_music_queue";
    private static final int MAX_AMOUNT_OF_RECENT_PLAYLIST = 3;
}