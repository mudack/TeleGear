import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabase;
import org.telegram.SQLite.extended_music_player.GlobalMusicDatabaseImpl;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.extended_music_player.entity.MessageLink;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;
import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;
import org.telegram.messenger.extended_music_player.entity.music.adapters.message_object.MusicMessageObjectAdapterInterface;

import java.io.File;
import java.util.ArrayList;

public class GlobalMusicDatabaseTest {

    private GlobalMusicDatabase db;
    private File testDbFile;

    @Before
    public void setup() throws Exception {
        File filesDir = ApplicationLoader.getFilesDirFixed();
        testDbFile = new File(filesDir, "test_music.db");

        if (testDbFile.exists()) {
            testDbFile.delete();
        }

        db =  new GlobalMusicDatabaseImpl(testDbFile);
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

        MusicData music = createTestMusic(1, 12L, 34);

        db.addMusicToPlaylist(playlist.getId(), music);

        ArrayList<MessageLink> result =
                db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(1, result.size());

        MessageLink link = result.get(0);

        assertEquals(music.getMessageLink().getAccountId(), link.getAccountId());
        assertEquals(music.getMessageLink().getDialogId(), link.getDialogId());
        assertEquals(music.getMessageLink().getMessageId(), link.getMessageId());
    }

    @Test
    public void testAddMusicToPlaylist_NoDuplicates() throws Exception {
        Playlist playlist = db.createPlaylist("MyPlaylist");

        MusicData music = createTestMusic(1, 123L, 3);

        db.addMusicToPlaylist(playlist.getId(), music);
        db.addMusicToPlaylist(playlist.getId(), music);

        ArrayList<MessageLink> result =
                db.getMusicLinksByPlaylistId(playlist.getId());

        assertEquals(1, result.size());
    }

    @Test
    public void testGetAllMusicFromLibrary() throws Exception {
        MusicData music1 = createTestMusic(1, 1231L, 31);
        MusicData music2 = createTestMusic(2, 1232L, 32);
        MusicData music3 = createTestMusic(3, 1233L, 33);
        MusicData music4 = createTestMusic(4, 1234L, 34);

        db.addMusicToLibrary(music1);
        db.addMusicToLibrary(music2);
        db.addMusicToLibrary(music3);
        db.addMusicToLibrary(music4);

        ArrayList<MusicData> allMusic = db.getAllMusicFromLibrary();

        assertEquals(4, allMusic.size());
    }

    @Test
    public void testAddMusicToLibrary_NoDuplicates() throws Exception {
        MusicData music = createTestMusic(1, 123L, 3);

        db.addMusicToLibrary(music);
        db.addMusicToLibrary(music);

        ArrayList<MusicData> allMusic = db.getAllMusicFromLibrary();

        assertEquals(1, allMusic.size());
    }


    private MusicData createTestMusic(int accId, long dId, int msgId) {

        MusicMessageObjectAdapterInterface messageSource = new MusicMessageObjectAdapterInterface() {
            @Override
            public boolean isMusic() {
                return true;
            }

            @Override
            public int getCurrentAccountId() {
                return accId;
            }

            @Override
            public long getDialogId() {
                return dId;
            }

            @Override
            public int getMessageId() {
                return msgId;
            }

            @Override
            public MusicMetaData getMusicMetadata() {
                return new MusicMetaData("The music", "the_music_file_name", "The performer", 16.0);
            }

        };
        return new MusicData(messageSource);
    }
}