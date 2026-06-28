import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepo;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepoImpl;
import org.telegram.SQLite.extended_music_player.dao.UtilDao;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;
import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;
import org.telegram.messenger.extended_music_player.entity.music.adapters.message_object.MusicMessageObjectAdapterInterface;

import java.io.File;
import java.util.ArrayList;

public class GlobalMusicDatabaseTest {

    private GlobalMusicDatabaseRepo db;
    private File testDbFile;

    @Before
    public void setup() throws Exception {
        File filesDir = ApplicationLoader.getFilesDirFixed();
        testDbFile = new File(filesDir, "test_2_music.db");

        if (testDbFile.exists()) {
            testDbFile.delete();
        }

        db = new GlobalMusicDatabaseRepoImpl(testDbFile);
    }

    @After
    public void teardown() {
        if (testDbFile.exists()) {
            testDbFile.delete();
        }
    }

    @Test
    public void testCreatePlaylist() throws Exception {
        Playlist p = db.createPlaylist("Rock");

        assertNotNull(p);
        assertEquals("Rock", p.getName());
    }

    @Test
    public void testCreatePlaylist_Unique() throws Exception {
        db.createPlaylist("Rock");
        db.createPlaylist("Rock");

        ArrayList<Playlist> list = db.getAllPlaylists();

        assertEquals(1, list.size());
    }

    @Test
    public void testGetAllPlaylist() throws Exception {
        db.createPlaylist("Jazz");
        db.createPlaylist("Rock");
        db.createPlaylist("Metal");

        assertEquals(3, db.getAllPlaylists().size());
    }

    @Test
    public void testGetPlaylistById() throws Exception {
        Playlist p = db.createPlaylist("Jazz");

        Playlist fromDb = db.getPlaylistById(p.getId());

        assertNotNull(fromDb);
        assertEquals("Jazz", fromDb.getName());
    }


    @Test
    public void testAddMusicToPlaylist() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");

        db.addNewUserIdsMapping(1, TEST_GLOBAL_ACC_ID_1);
        MusicData music = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 12L, 34);

        db.addMusicToPlaylist(playlist.getId(), music);

        ArrayList<MessageLink> result =
                db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(1, result.size());

        MessageLink link = result.get(0);

        assertEquals(music.getMessageLink().getGlobalAccountId(), link.getGlobalAccountId());
        assertEquals(music.getMessageLink().getLocalAccountId(), link.getLocalAccountId());
        assertEquals(music.getMessageLink().getDialogId(), link.getDialogId());
        assertEquals(music.getMessageLink().getMessageId(), link.getMessageId());
    }

    @Test
    public void testAddMusicToPlaylist_exceptionLocalIdIsNullObj() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");

        MusicData music = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 12L, 34);

        db.addMusicToPlaylist(playlist.getId(), music);

        ArrayList<MessageLink> result =
                db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(1, result.size());

        MessageLink link = result.get(0);

        assertEquals(music.getMessageLink().getGlobalAccountId(), link.getGlobalAccountId());
        assertEquals(UtilDao.INT_NULL_OBJECT, link.getLocalAccountId());
        assertEquals(music.getMessageLink().getDialogId(), link.getDialogId());
        assertEquals(music.getMessageLink().getMessageId(), link.getMessageId());
    }

    @Test
    public void testAddMusicToPlaylist_NoDuplicates() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");

        MusicData music = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 123L, 3);

        db.addMusicToPlaylist(playlist.getId(), music);
        db.addMusicToPlaylist(playlist.getId(), music);

        ArrayList<MessageLink> result =
                db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(1, result.size());
    }

    @Test
    public void testGetAllMusicFromLibrary() throws Exception {
        MusicData music1 = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 1231L, 31);
        MusicData music2 = createTestMusic(TEST_GLOBAL_ACC_ID_1, 2, 1232L, 32);
        MusicData music3 = createTestMusic(TEST_GLOBAL_ACC_ID_1, 3, 1233L, 33);
        MusicData music4 = createTestMusic(TEST_GLOBAL_ACC_ID_1, 4, 1234L, 34);

        db.addMusicToLibrary(music1);
        db.addMusicToLibrary(music2);
        db.addMusicToLibrary(music3);
        db.addMusicToLibrary(music4);

        ArrayList<MusicData> allMusic = db.getAllMusicFromLibrary();

        assertEquals(4, allMusic.size());
    }

    @Test
    public void testAddMusicToLibrary_NoDuplicates() throws Exception {
        MusicData music = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 123L, 3);

        db.addMusicToLibrary(music);
        db.addMusicToLibrary(music);

        ArrayList<MusicData> allMusic = db.getAllMusicFromLibrary();

        assertEquals(1, allMusic.size());
    }


    private MusicData createTestMusic(long globalAccId, int localAccId, long dId, int msgId) {
        MusicMessageObjectAdapterInterface messageSource = new MusicMessageObjectAdapterInterface() {
            @Override
            public boolean isMusic() {
                return true;
            }

            @Override
            public MessageLink getMessageLink() {
                return new MessageLink(
                        globalAccId, localAccId, dId, msgId
                );
            }

            @Override
            public MusicMetaData getMusicMetadata() {
                return new MusicMetaData("The music", "the_music_file_name", "The performer", 16.0);
            }

        };
        return new MusicData(messageSource);
    }

    @Test
    public void testMarkAndGetRecentPlaylists() throws SQLiteException {
        Playlist p1 = db.createPlaylist("P1");
        Playlist p2 = db.createPlaylist("P2");
        Playlist p3 = db.createPlaylist("P3");

        db.markPlaylistAsRecent(p1.getId(), 1000);
        db.markPlaylistAsRecent(p2.getId(), 2000);
        db.markPlaylistAsRecent(p3.getId(), 3000);

        ArrayList<Playlist> result = db.getRecentPlaylists(3);

        assertEquals(3, result.size());
        assertEquals(p3.getId(), result.get(0).getId());
        assertEquals(p2.getId(), result.get(1).getId());
        assertEquals(p1.getId(), result.get(2).getId());
    }

    @Test
    public void testRecentPlaylistsUpsert() throws SQLiteException {
        Playlist p = db.createPlaylist("P1");

        db.markPlaylistAsRecent(p.getId(), 1000);
        db.markPlaylistAsRecent(p.getId(), 5000);

        ArrayList<Playlist> result = db.getRecentPlaylists(3);

        assertEquals(1, result.size());
        assertEquals(p.getId(), result.get(0).getId());
    }

    @Test
    public void testRemoveRecentPlaylist() throws SQLiteException {
        Playlist p1 = db.createPlaylist("P1");
        Playlist p2 = db.createPlaylist("P2");

        db.markPlaylistAsRecent(p1.getId(), 1000);
        db.markPlaylistAsRecent(p2.getId(), 2000);

        db.removeRecentPlaylist(p1.getId());

        ArrayList<Playlist> result = db.getRecentPlaylists(3);

        assertEquals(1, result.size());
        assertEquals(p2.getId(), result.get(0).getId());
    }

    @Test
    public void testClearRecentPlaylists() throws SQLiteException {
        Playlist p1 = db.createPlaylist("P1");
        Playlist p2 = db.createPlaylist("P2");
        Playlist p3 = db.createPlaylist("P3");

        db.markPlaylistAsRecent(p1.getId(), 1000);
        db.markPlaylistAsRecent(p2.getId(), 2000);
        db.markPlaylistAsRecent(p3.getId(), 3000);

        db.clearRecentPlaylists();

        ArrayList<Playlist> result = db.getRecentPlaylists(3);

        assertEquals(0, result.size());
    }

    @Test
    public void testRecentPlaylistUpdatesExistingEntry() throws SQLiteException {
        Playlist p = db.createPlaylist("P1");

        db.markPlaylistAsRecent(p.getId(), 1000);

        ArrayList<Playlist> result1 = db.getRecentPlaylists(3);
        assertEquals(1, result1.size());
        assertEquals(p.getId(), result1.get(0).getId());

        db.markPlaylistAsRecent(p.getId(), 5000);

        ArrayList<Playlist> result2 = db.getRecentPlaylists(3);

        assertEquals(1, result2.size());
        assertEquals(p.getId(), result2.get(0).getId());
    }

    @Test
    public void testLocalAndMtprotoUserIdsInMappingTable() throws SQLiteException {
        db.addNewUserIdsMapping(1, 1000);
        db.addNewUserIdsMapping(2, 2000);
        db.addNewUserIdsMapping(3, 3000);

        assertEquals(1, db.getLocalUserIdByMtprotoId(1000));
        assertEquals(2, db.getLocalUserIdByMtprotoId(2000));
        assertEquals(3, db.getLocalUserIdByMtprotoId(3000));
    }

    @Test
    public void testLocalAndMtprotoUserIdsInMappingTable_case_userLogoutAndLoginInAnotherAcc() throws SQLiteException {
        db.addNewUserIdsMapping(1, 1000);
        db.addNewUserIdsMapping(2, 2000);
        db.addNewUserIdsMapping(3, 3000);

        db.removeUserIdsMappingByLocalId(2);

        assertEquals(1, db.getLocalUserIdByMtprotoId(1000));
        assertEquals(3, db.getLocalUserIdByMtprotoId(3000));

        db.addNewUserIdsMapping(2, 2222);

        assertEquals(2, db.getLocalUserIdByMtprotoId(2222));
    }

    private final long TEST_GLOBAL_ACC_ID_1 = 516289273;
}