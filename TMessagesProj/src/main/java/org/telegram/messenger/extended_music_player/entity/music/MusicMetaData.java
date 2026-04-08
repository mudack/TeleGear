package org.telegram.messenger.extended_music_player.entity.music;

import androidx.annotation.NonNull;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Objects;

public class MusicMetaData {
    private final String title;
    private final String fileName;
    private final String performer;
    private final double durationSec;

    public MusicMetaData(String title, String fileName, String performer, double duration) {
        this.title = title;
        this.fileName = fileName;
        this.performer = performer;
        this.durationSec = duration;
    }

    public MusicMetaData(ArrayList<TLRPC.DocumentAttribute> attributes) {
        MusicMetaData metadata = fromDocumentAttrList(attributes);

        this.title = metadata.getTitle();
        this.fileName = metadata.getFileName();
        this.performer = metadata.getPerformer();
        this.durationSec = metadata.getDurationSec();
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPerformer() {
        return performer;
    }

    public double getDurationSec() {
        return durationSec;
    }


    public static MusicMetaData fromDocumentAttrList(ArrayList<TLRPC.DocumentAttribute> attributes) {
        //fields from TL_documentAttributeAudio
        String title = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getTitle();//todo replace with stringRes
        String fileName = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getFileName();//todo replace with stringRes
        String performer = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getPerformer();
        double duration = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getDurationSec();

        //fields from TLRPC.DocumentAttribute
        String docAttrTitle = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getTitle();
        String docAttrFileName = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getFileName();
        String docAttrPerformer = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getPerformer();
        double docAttrDuration = MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getDurationSec();

        for (TLRPC.DocumentAttribute attr : attributes) {
            if (attr.title != null && !attr.title.isEmpty()) docAttrTitle = attr.title;
            if (attr.file_name != null && !attr.file_name.isEmpty())
                docAttrFileName = attr.file_name;
            if (attr.performer != null && !attr.performer.isEmpty())
                docAttrPerformer = attr.performer;
            if (attr.duration != 0) docAttrDuration = attr.duration;

            if (attr instanceof TLRPC.TL_documentAttributeAudio) {
                TLRPC.TL_documentAttributeAudio audio = (TLRPC.TL_documentAttributeAudio) attr;
                if (audio.title != null && !audio.title.isEmpty()) title = audio.title;
                else if (audio.file_name != null && !audio.file_name.isEmpty()) {
                    title = audio.file_name;
                }
                if (audio.file_name != null && !audio.file_name.isEmpty())
                    fileName = audio.file_name;
                if (audio.performer != null && !audio.performer.isEmpty())
                    performer = audio.performer;
                if (audio.duration != 0) duration = audio.duration;

            }
        }

        //checking below was implemented assure that data like fileName is not miss cause it can be!!!

        if (fileName.equals(MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getFileName())) {
            fileName = docAttrFileName;
        }
        if (title.equals(MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getTitle())) {
            title = docAttrTitle;
            if (title.equals(MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getTitle()) && !fileName.equals(MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getFileName())) {
                title = fileName;
            }
        }
        if (performer.equals(MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getPerformer())) {
            performer = docAttrPerformer;
        }
        if (duration == MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getDurationSec()) {
            duration = docAttrDuration;
        }

        return new MusicMetaData(title, fileName, performer, duration);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicMetaData that = (MusicMetaData) o;
        return Double.compare(that.durationSec, durationSec) == 0 &&
                Objects.equals(title, that.title) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(performer, that.performer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, fileName, performer, durationSec);
    }

    @NonNull
    @Override
    public String toString() {
        return "MusicMetaData{" +
                "title='" + title + '\'' +
                ", fileName='" + fileName + '\'' +
                ", performer='" + performer + '\'' +
                ", duration=" + durationSec +
                '}';
    }

    public static final MusicMetaData MUSIC_META_DATA_NULL_OBJECT = new MusicMetaData(
            "AudioUnknownTitle",
            "AudioUnknownFileName",
            "AudioUnknownArtist",
            0
    );
}