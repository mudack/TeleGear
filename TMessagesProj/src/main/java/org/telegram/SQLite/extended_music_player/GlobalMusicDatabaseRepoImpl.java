package org.telegram.SQLite.extended_music_player;

import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.extended_music_player.dao.PlaylistDao;
import org.telegram.SQLite.extended_music_player.dao.PlaylistTrackDao;
import org.telegram.SQLite.extended_music_player.dao.RecentPlaylistDao;
import org.telegram.SQLite.extended_music_player.dao.TracksDao;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;

import java.io.File;
import java.util.ArrayList;

/**
 * GlobalMusicStorage is a low-level data access layer responsible for direct interaction
 * with the local SQLite database used for storing music tracks and playlists.
 *
 * <p>This class encapsulates all SQL operations such as creating tables,
 * inserting data, and executing queries.</p>
 *
 * <h3>⚠️ Threading Model</h3>
 * <ul>
 *     <li>This class DOES NOT manage threading.</li>
 *     <li>All methods are executed in the calling thread.</li>
 *     <li>It is the responsibility of the caller (e.g., Controller) to ensure
 *     that database operations are performed on a background thread.</li>
 * </ul>
 *
 * <h3>⚠️ Error Handling</h3>
 * <ul>
 *     <li>All methods may throw runtime exceptions related to database operations.</li>
 *     <li>Every method call SHOULD be wrapped in try-catch blocks by the caller.</li>
 *     <li>This design allows centralized error handling at a higher level
 *     (e.g., in GlobalMusicController).</li>
 * </ul>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Database initialization (tables creation).</li>
 *     <li>CRUD operations for tracks, playlists, and playlist-track relations.</li>
 *     <li>Returning raw data models from database queries.</li>
 * </ul>
 *
 * <h3>Non-Responsibilities</h3>
 * <ul>
 *     <li>Does NOT handle threading (no DispatchQueue or async logic).</li>
 *     <li>Does NOT contain business logic.</li>
 *     <li>Does NOT interact with UI.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>
 * try {
 *     int trackId = storage.insertTrack(musicData);
 * } catch (Exception e) {
 *     // handle error
 * }
 * </pre>
 *
 * <p>Typically used via a higher-level controller (e.g., GlobalMusicController),
 * which manages threading, error handling, and communication with UI.</p>
 */
public class GlobalMusicDatabaseRepoImpl implements GlobalMusicDatabaseRepo {

    private final org.telegram.SQLite.SQLiteDatabase database;

    private final PlaylistDao playlistDao;
    private final PlaylistTrackDao playlistTrackDao;
    private final TracksDao tracksDao;
    private final RecentPlaylistDao recentPlaylistDao;

    public GlobalMusicDatabaseRepoImpl(File dbFile) throws SQLiteException {
        database = new org.telegram.SQLite.SQLiteDatabase(dbFile.getPath());

        playlistDao = new PlaylistDao(database);
        playlistTrackDao = new PlaylistTrackDao(database);
        tracksDao = new TracksDao(database);
        recentPlaylistDao = new RecentPlaylistDao(database);
    }

    @Override
    public Playlist createPlaylist(String name) throws SQLiteException {
        return playlistDao.createPlaylist(name);
    }

    @Override
    public ArrayList<Playlist> getAllPlaylists() throws SQLiteException {
        return playlistDao.getAllPlaylists();
    }

    @Override
    public Playlist getPlaylistById(int id) throws SQLiteException {
        return playlistDao.getPlaylistById(id);
    }

    @Override
    public Integer addMusicToLibrary(MusicData music) throws SQLiteException {
        return tracksDao.addMusicToLibrary(music);
    }

    @Override
    public MusicData getMusicFromLibraryById(int id) throws SQLiteException {
        return tracksDao.getMusicFromLibraryById(id);
    }

    @Override
    public ArrayList<MusicData> getAllMusicFromLibrary() throws SQLiteException {
        return tracksDao.getAllMusicFromLibrary();
    }

    @Override
    public void addMusicToPlaylist(int playlistId, MusicData music) throws SQLiteException {
        Integer musicId = addMusicToLibrary(music);
        playlistTrackDao.addMusicToPlaylistById(playlistId, musicId);
    }

    @Override
    public ArrayList<MessageLink> getMusicLinksByPlaylistId(int playlistId) throws SQLiteException {
        return tracksDao.getMusicLinksByPlaylistId(playlistId);
    }

    @Override
    public void markPlaylistAsRecent(int playlistId, long timestamp) throws SQLiteException {
        recentPlaylistDao.markPlaylistAsRecent(playlistId, timestamp);
    }

    @Override
    public ArrayList<Playlist> getRecentPlaylists(int limit) throws SQLiteException {
        return recentPlaylistDao.getRecentPlaylists(limit);
    }

    @Override
    public ArrayList<Playlist> getRecentPlaylistsByOffset(int offset, int limit) throws SQLiteException {
        return recentPlaylistDao.getRecentPlaylistsByOffset(offset, limit);
    }

    @Override
    public void removeRecentPlaylist(int playlistId) throws SQLiteException {
        recentPlaylistDao.removeRecentPlaylist(playlistId);
    }

    @Override
    public void clearRecentPlaylists() throws SQLiteException {
        recentPlaylistDao.clearRecentPlaylists();
    }

    @Override
    public void addNewUserIdsMapping(int localUserId, long mtprotoUserId) throws SQLiteException {
        tracksDao.addNewEntry(localUserId, mtprotoUserId);
    }

    @Override
    public int getLocalUserIdByMtprotoId(long mtprotoId) throws SQLiteException {
        return tracksDao.getLocalUserId(mtprotoId);
    }

    @Override
    public void removeUserIdsMappingByLocalId(int localId) throws SQLiteException {
        tracksDao.removeMappingEntryByLocalId(localId);
    }

    public static final String GLOBAL_MUSIC_DB_FILE_NAME = "global_music.db";
}