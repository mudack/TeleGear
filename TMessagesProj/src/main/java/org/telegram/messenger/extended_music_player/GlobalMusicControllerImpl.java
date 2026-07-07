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
import java.util.Iterator;
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
                AndroidUtilities.runOnUIThread(() -> postPlaylistsLoaded(playlists));
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

    @Override
    public void renamePlaylist(int playlistId, String name) {
        storageQueue.postRunnable(() -> {
            try {
                Playlist renamedPlaylist = databaseRepo.renamePlaylist(playlistId, name);
                ArrayList<Playlist> playlists = databaseRepo.getAllPlaylists();
                AndroidUtilities.runOnUIThread(() -> {
                    replaceRecentPlaylist(renamedPlaylist);
                    postPlaylistsLoaded(playlists);
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't rename playlist with id: " + playlistId + " due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void deletePlaylist(int playlistId) {
        storageQueue.postRunnable(() -> {
            try {
                databaseRepo.deletePlaylist(playlistId);
                ArrayList<Playlist> playlists = databaseRepo.getAllPlaylists();
                AndroidUtilities.runOnUIThread(() -> {
                    removeRecentPlaylistFromMemory(playlistId);
                    postPlaylistsLoaded(playlists);
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't delete playlist with id: " + playlistId + " due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void updatePlaylistOrder(ArrayList<Integer> playlistIds) {
        storageQueue.postRunnable(() -> {
            try {
                databaseRepo.updatePlaylistOrder(playlistIds);
                ArrayList<Playlist> playlists = databaseRepo.getAllPlaylists();
                AndroidUtilities.runOnUIThread(() -> postPlaylistsLoaded(playlists));
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't update playlist order due " + e.getMessage()
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

    @UiThread
    private void postPlaylistsLoaded(ArrayList<Playlist> playlists) {
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.musicLoadListOfPlaylist,
                playlists
        );
    }

    @UiThread
    private void replaceRecentPlaylist(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        boolean changed = false;
        ArrayList<Playlist> updated = new ArrayList<>();
        for (Playlist recentPlaylist : recentPlaylists) {
            if (recentPlaylist.getId() == playlist.getId()) {
                updated.add(playlist);
                changed = true;
            } else {
                updated.add(recentPlaylist);
            }
        }
        if (changed) {
            recentPlaylists.clear();
            recentPlaylists.addAll(updated);
        }
    }

    @UiThread
    private void removeRecentPlaylistFromMemory(int playlistId) {
        Iterator<Playlist> iterator = recentPlaylists.iterator();
        while (iterator.hasNext()) {
            Playlist playlist = iterator.next();
            if (playlist.getId() == playlistId) {
                iterator.remove();
                break;
            }
        }
    }

    @Override
    public void addAccountIdMapping(int localAccountId, long mtprotoAccountId) {
        storageQueue.postRunnable(() -> {
            try {
                databaseRepo.addAccountIdMapping(localAccountId, mtprotoAccountId);
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
                            "Can't do addAccountIdMapping with: localAccountId = " + localAccountId + ", mtprotoAccountId = "+ mtprotoAccountId +" due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void getLocalAccountIdByMtprotoAccountId(long mtprotoAccountId) {
        storageQueue.postRunnable(() -> {
            try {
                int localAccountId = databaseRepo.getLocalAccountIdByMtprotoAccountId(mtprotoAccountId);
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.localAccountIdFromMappingTable,
                            localAccountId
                    );
                });
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't do getLocalAccountIdByMtprotoAccountId with: mtprotoAccountId = "+ mtprotoAccountId +" due " + e.getMessage()
                    );
                });
            }
        });
    }

    @Override
    public void removeAccountIdMappingByLocalId(int localAccountId) {
        storageQueue.postRunnable(() -> {
            try {
                databaseRepo.removeAccountIdMappingByLocalId(localAccountId);
            } catch (SQLiteException e) {
                FileLog.e(e);
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    NotificationCenter.getGlobalInstance().postNotificationName(
                            NotificationCenter.musicDatabaseError,
                            "Can't do removeAccountIdMappingByLocalId with: localAccountId = "+ localAccountId +" due " + e.getMessage()
                    );
                });
            }
        });
    }

    private static final String DISPATCH_STORAGE_QUEUE_NAME = "global_music_queue";
    private static final int MAX_AMOUNT_OF_RECENT_PLAYLIST = 3;
}
