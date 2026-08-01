import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepo;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseRepoImpl;
import org.telegram.SQLite.extended_music_player.PlaylistAlreadyExistsException;
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
    public void testCreatePlaylistAndAddMusic() throws Exception {
        MusicData music = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 12L, 34);

        Playlist playlist = db.createPlaylistAndAddMusic("MyPlaylist", music);

        assertNotNull(playlist);
        assertEquals("MyPlaylist", playlist.getName());
        ArrayList<MessageLink> links = db.getMusicLinksByPlaylistId(playlist.getId());
        assertEquals(1, links.size());
        MessageLink link = links.get(0);
        assertEquals(music.getMessageLink().getMtprotoAccountId(), link.getMtprotoAccountId());
        assertEquals(music.getMessageLink().getDialogId(), link.getDialogId());
        assertEquals(music.getMessageLink().getMessageId(), link.getMessageId());
    }

    @Test
    public void testCreatePlaylistAndAddMusic_exceptionDuplicateNameDoesNotAddMusic() throws Exception {
        Playlist playlist = db.createPlaylistAndAddMusic("MyPlaylist", createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 12L, 34));

        try {
            db.createPlaylistAndAddMusic("MyPlaylist", createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 56L, 78));
            fail("Expected PlaylistAlreadyExistsException");
        } catch (PlaylistAlreadyExistsException expected) {
            assertEquals(1, db.getAllPlaylists().size());
            ArrayList<MessageLink> links = db.getMusicLinksByPlaylistId(playlist.getId());
            assertEquals(1, links.size());
            assertEquals(34, links.get(0).getMessageId());
        }
    }

    @Test
    public void testGetAllPlaylist() throws Exception {
        Playlist jazz = db.createPlaylist("Jazz");
        Playlist rock = db.createPlaylist("Rock");
        Playlist metal = db.createPlaylist("Metal");

        ArrayList<Playlist> result = db.getAllPlaylists();

        assertEquals(3, result.size());
        assertEquals(jazz.getId(), result.get(0).getId());
        assertEquals(rock.getId(), result.get(1).getId());
        assertEquals(metal.getId(), result.get(2).getId());
    }

    @Test
    public void testGetPlaylistById() throws Exception {
        Playlist p = db.createPlaylist("Jazz");

        Playlist fromDb = db.getPlaylistById(p.getId());

        assertNotNull(fromDb);
        assertEquals("Jazz", fromDb.getName());
    }

    @Test
    public void testRenamePlaylist() throws Exception {
        Playlist playlist = db.createPlaylist("Old name");

        Playlist renamed = db.renamePlaylist(playlist.getId(), "New name");
        Playlist fromDb = db.getPlaylistById(playlist.getId());

        assertEquals(playlist.getId(), renamed.getId());
        assertEquals("New name", renamed.getName());
        assertNotNull(fromDb);
        assertEquals("New name", fromDb.getName());
    }

    @Test
    public void testRenamePlaylist_sameNameNoOp() throws Exception {
        Playlist playlist = db.createPlaylist("Same name");

        Playlist renamed = db.renamePlaylist(playlist.getId(), "Same name");

        assertEquals(playlist.getId(), renamed.getId());
        assertEquals("Same name", renamed.getName());
        assertEquals("Same name", db.getPlaylistById(playlist.getId()).getName());
    }

    @Test
    public void testRenamePlaylist_exceptionDuplicateName() throws Exception {
        Playlist playlist = db.createPlaylist("First");
        db.createPlaylist("Second");

        try {
            db.renamePlaylist(playlist.getId(), "Second");
            fail("Expected SQLiteException");
        } catch (SQLiteException ignored) {

        }

        assertEquals("First", db.getPlaylistById(playlist.getId()).getName());
    }

    @Test
    public void testRenamePlaylist_exceptionEmptyName() throws Exception {
        Playlist playlist = db.createPlaylist("Playlist");

        try {
            db.renamePlaylist(playlist.getId(), "  ");
            fail("Expected SQLiteException");
        } catch (SQLiteException ignored) {

        }

        assertEquals("Playlist", db.getPlaylistById(playlist.getId()).getName());
    }

    @Test
    public void testDeletePlaylist() throws Exception {
        Playlist playlist = db.createPlaylist("Playlist");

        db.deletePlaylist(playlist.getId());

        assertEquals(0, db.getAllPlaylists().size());
    }

    @Test
    public void testDeletePlaylist_removesPlaylistRelations() throws Exception {
        Playlist playlist = db.createPlaylist("Playlist");
        db.markPlaylistAsRecent(playlist.getId(), 1000);
        db.addMusicToPlaylist(playlist.getId(), createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 12L, 34));

        db.deletePlaylist(playlist.getId());

        assertEquals(0, db.getRecentPlaylists(3).size());
        assertEquals(0, db.getMusicLinksByPlaylistId(playlist.getId()).size());
    }

    @Test
    public void testDeletePlaylist_exceptionMissingPlaylist() throws Exception {
        try {
            db.deletePlaylist(777);
            fail("Expected SQLiteException");
        } catch (SQLiteException ignored) {

        }
    }

    @Test
    public void testUpdatePlaylistOrder() throws Exception {
        Playlist jazz = db.createPlaylist("Jazz");
        Playlist rock = db.createPlaylist("Rock");
        Playlist metal = db.createPlaylist("Metal");

        ArrayList<Integer> order = new ArrayList<>();
        order.add(metal.getId());
        order.add(jazz.getId());
        order.add(rock.getId());
        db.updatePlaylistOrder(order);

        ArrayList<Playlist> result = db.getAllPlaylists();

        assertEquals(metal.getId(), result.get(0).getId());
        assertEquals(jazz.getId(), result.get(1).getId());
        assertEquals(rock.getId(), result.get(2).getId());
    }

    @Test
    public void testUpdatePlaylistOrder_exceptionDuplicatePlaylistId() throws Exception {
        Playlist jazz = db.createPlaylist("Jazz");
        Playlist rock = db.createPlaylist("Rock");

        ArrayList<Integer> order = new ArrayList<>();
        order.add(jazz.getId());
        order.add(jazz.getId());

        try {
            db.updatePlaylistOrder(order);
            fail("Expected SQLiteException");
        } catch (SQLiteException ignored) {

        }

        ArrayList<Playlist> result = db.getAllPlaylists();
        assertEquals(jazz.getId(), result.get(0).getId());
        assertEquals(rock.getId(), result.get(1).getId());
    }

    @Test
    public void testUpdatePlaylistOrder_exceptionMissingPlaylistId() throws Exception {
        Playlist jazz = db.createPlaylist("Jazz");
        Playlist rock = db.createPlaylist("Rock");

        ArrayList<Integer> order = new ArrayList<>();
        order.add(jazz.getId());
        order.add(777);

        try {
            db.updatePlaylistOrder(order);
            fail("Expected SQLiteException");
        } catch (SQLiteException ignored) {

        }

        ArrayList<Playlist> result = db.getAllPlaylists();
        assertEquals(jazz.getId(), result.get(0).getId());
        assertEquals(rock.getId(), result.get(1).getId());
    }

    @Test
    public void testAddMusicToPlaylist() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");

        db.addAccountIdMapping(1, TEST_GLOBAL_ACC_ID_1);
        MusicData music = createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 12L, 34);

        db.addMusicToPlaylist(playlist.getId(), music);

        ArrayList<MessageLink> result =
                db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(1, result.size());

        MessageLink link = result.get(0);

        assertEquals(music.getMessageLink().getMtprotoAccountId(), link.getMtprotoAccountId());
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

        assertEquals(music.getMessageLink().getMtprotoAccountId(), link.getMtprotoAccountId());
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
    public void testGetMusicLinksByPlaylistId_sortedByTitleAsc() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");

        db.addMusicToPlaylist(playlist.getId(), createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 100L, 1, "Charlie"));
        db.addMusicToPlaylist(playlist.getId(), createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 101L, 2, "alpha"));
        db.addMusicToPlaylist(playlist.getId(), createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 102L, 3, "Bravo"));

        ArrayList<MessageLink> result = db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(3, result.size());
        assertEquals(101L, result.get(0).getDialogId());
        assertEquals(102L, result.get(1).getDialogId());
        assertEquals(100L, result.get(2).getDialogId());
    }

    @Test
    public void testGetMusicDataByPlaylistId_keepsStoredMetadata() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");
        db.addAccountIdMapping(1, TEST_GLOBAL_ACC_ID_1);
        db.addMusicToPlaylist(playlist.getId(), createTestMusic(TEST_GLOBAL_ACC_ID_1, 1, 100L, 1, "Stored title"));

        ArrayList<MusicData> result = db.getMusicDataByPlaylistId(playlist.getId());

        assertEquals(1, result.size());
        assertEquals("Stored title", result.get(0).getMusicMetaData().getTitle());
        assertEquals("the_music_file_name", result.get(0).getMusicMetaData().getFileName());
        assertEquals("The performer", result.get(0).getMusicMetaData().getPerformer());
        assertEquals(16.0, result.get(0).getMusicMetaData().getDurationSec(), 0.0);
        assertEquals(1, result.get(0).getMessageLink().getLocalAccountId());
        assertEquals(100L, result.get(0).getMessageLink().getDialogId());
        assertEquals(1, result.get(0).getMessageLink().getMessageId());
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
        return createTestMusic(globalAccId, localAccId, dId, msgId, "The music");
    }

    private MusicData createTestMusic(long globalAccId, int localAccId, long dId, int msgId, String title) {
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
                return new MusicMetaData(title, "the_music_file_name", "The performer", 16.0);
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
        db.addAccountIdMapping(1, 1000);
        db.addAccountIdMapping(2, 2000);
        db.addAccountIdMapping(3, 3000);

        assertEquals(1, db.getLocalAccountIdByMtprotoAccountId(1000));
        assertEquals(2, db.getLocalAccountIdByMtprotoAccountId(2000));
        assertEquals(3, db.getLocalAccountIdByMtprotoAccountId(3000));
    }

    @Test
    public void testLocalAndMtprotoUserIdsInMappingTable_case_userLogoutAndLoginInAnotherAcc() throws SQLiteException {
        db.addAccountIdMapping(1, 1000);
        db.addAccountIdMapping(2, 2000);
        db.addAccountIdMapping(3, 3000);

        db.removeAccountIdMappingByLocalId(2);

        assertEquals(1, db.getLocalAccountIdByMtprotoAccountId(1000));
        assertEquals(3, db.getLocalAccountIdByMtprotoAccountId(3000));

        db.addAccountIdMapping(2, 2222);

        assertEquals(2, db.getLocalAccountIdByMtprotoAccountId(2222));
    }

    private final long TEST_GLOBAL_ACC_ID_1 = 516289273;
}
