package org.telegram.ui.extended_music_screens.playlists;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.extended_music_player.GlobalMusicControllerImpl;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.messenger.extended_music_player.entity.music.MusicData;
import org.telegram.messenger.extended_music_player.entity.music.MusicMetaData;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class DetailedPlaylistActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private MusicAdapter listAdapter;
    private EmptyTextProgressView emptyView;
    private FragmentContextView fragmentContextView;
    private FrameLayout fragmentContextViewWrapper;

    private final ArrayList<MusicData> musics = new ArrayList<>();

    private int playlistId;
    private String playlistName;
    private boolean loading = true;

    private GlobalMusicControllerImpl globalMusicController;

    public DetailedPlaylistActivity(Bundle args) {
        super(args);
    }

    public static Bundle createArgs(Playlist playlist) {
        Bundle args = new Bundle();
        args.putInt(ARG_PLAYLIST_ID, playlist.getId());
        args.putString(ARG_PLAYLIST_NAME, playlist.getName());
        return args;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        if (arguments == null) {
            return false;
        }

        playlistId = arguments.getInt(ARG_PLAYLIST_ID, 0);
        playlistName = arguments.getString(ARG_PLAYLIST_NAME);
        if (playlistId <= 0) {
            return false;
        }

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.musicReceiveMusicFromPlaylist);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.musicDatabaseError);

        globalMusicController = GlobalMusicControllerImpl.getInstance();
        globalMusicController.getMusicsByPlaylistId(playlistId);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.musicReceiveMusicFromPlaylist);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.musicDatabaseError);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(TextUtils.isEmpty(playlistName) ? LocaleController.getString(R.string.Music) : playlistName);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        emptyView = new EmptyTextProgressView(context, null, resourceProvider);
        emptyView.setText(LocaleController.getString(R.string.NoAudioFiles));
        emptyView.setShowAtCenter(true);
        if (loading) {
            emptyView.showProgress(false);
        } else {
            emptyView.showTextView();
        }
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context, resourceProvider);
        listView.setClipToPadding(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new MusicAdapter(context));
        listView.setEmptyView(emptyView);
        listView.setOnItemClickListener((view, position) -> playMusicAt(position));
        actionBar.setAdaptiveBackground(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        fragmentContextViewWrapper = new FrameLayout(context);
        fragmentContextViewWrapper.setVisibility(View.GONE);
        fragmentContextView = new FragmentContextView(context, this, false, resourceProvider) {
            @Override
            public void setVisibility(int visibility) {
                if (fragmentContextViewWrapper != null) {
                    fragmentContextViewWrapper.setVisibility(visibility);
                    checkListViewPadding();
                }
            }
        };
        fragmentContextView.setSupportsCalls(false);
        fragmentContextView.isInsideBubble = true;
        fragmentContextViewWrapper.addView(fragmentContextView);
        frameLayout.addView(fragmentContextViewWrapper, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT));

        checkListViewPadding();
        updateEmptyView();
        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.musicReceiveMusicFromPlaylist) {
            loading = false;
            musics.clear();
            if (args.length > 0 && args[0] instanceof ArrayList<?>) {
                for (Object item : (ArrayList<?>) args[0]) {
                    if (item instanceof MusicData) {
                        musics.add((MusicData) item);
                    }
                }
            }
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            updateEmptyView();
        } else if (id == NotificationCenter.musicDatabaseError) {
            loading = false;
            updateEmptyView();
            // TODO: musicDatabaseError is global for all GlobalMusicController operations.
            // Add playlist/request id to the notification before this screen handles multiple parallel requests.
            if (getParentActivity() != null && args.length > 0) {
                Toast.makeText(getParentActivity(), LocaleController.formatString(R.string.playlist_error_message, String.valueOf(args[0])), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateEmptyView() {
        if (emptyView == null) {
            return;
        }
        if (loading) {
            emptyView.showProgress();
        } else {
            emptyView.setText(LocaleController.getString(R.string.NoAudioFiles));
            emptyView.showTextView();
        }
    }

    private void checkListViewPadding() {
        int contextViewHeight = getFragmentContextViewHeight();
        if (listView != null) {
            listView.setPadding(0, contextViewHeight, 0, 0);
        }
        if (emptyView != null) {
            emptyView.setPadding(0, contextViewHeight, 0, 0);
        }
    }

    private int getFragmentContextViewHeight() {
        return fragmentContextViewWrapper != null && fragmentContextViewWrapper.getVisibility() == View.VISIBLE ? dp(36) : 0;
    }

    private MusicData getMusicAt(int position) {
        if (position < 0 || position >= musics.size()) {
            return null;
        }
        return musics.get(position);
    }

    private void playMusicAt(int position) {
        MusicData selectedMusic = getMusicAt(position);
        if (selectedMusic == null) {
            return;
        }

        ArrayList<MessageObject> playlist = new ArrayList<>();
        MessageObject selectedMessageObject = null;
        for (int i = 0; i < musics.size(); i++) {
            MessageObject messageObject = getMessageObject(musics.get(i));
            if (messageObject == null || !messageObject.isMusic()) {
                continue;
            }
            playlist.add(messageObject);
            if (i == position) {
                selectedMessageObject = messageObject;
            }
        }

        if (selectedMessageObject == null || playlist.isEmpty()) {
            if (getParentActivity() != null) {
                Toast.makeText(getParentActivity(), LocaleController.getString(R.string.ErrorOccurred), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        boolean started = MediaController.getInstance().setPlaylist(
                playlist,
                selectedMessageObject,
                0,
                false,
                null,
                true
        );
        if (!started && getParentActivity() != null) {
            Toast.makeText(getParentActivity(), LocaleController.getString(R.string.ErrorOccurred), Toast.LENGTH_SHORT).show();
        }
    }

    private MessageObject getMessageObject(MusicData music) {
        try {
            return music.getMessageLink().getMessageObject();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    @Override
    public int getThemedColor(int key) {
        return Theme.getColor(key, resourceProvider);
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
            if (listView != null) {
                int count = listView.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = listView.getChildAt(i);
                    if (child instanceof MusicCell) {
                        ((MusicCell) child).updateColors();
                    }
                }
            }
            if (fragmentView != null) {
                fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
            }
            if (fragmentContextView != null) {
                fragmentContextView.updateColors();
            }
            if (actionBar != null) {
                actionBar.updateColors();
            }
        };

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{MusicCell.class}, null, null, cellDelegate, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{MusicCell.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{MusicCell.class}, new String[]{"subtitleTextView", "durationTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText3));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{MusicCell.class}, new String[]{"imageView"}, null, null, cellDelegate, Theme.key_featuredStickers_addButton));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{MusicCell.class}, null, null, cellDelegate, Theme.key_divider));

        return themeDescriptions;
    }

    private class MusicAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        private MusicAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return musics.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new MusicCell(context, resourceProvider));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MusicData music = getMusicAt(position);
            if (music != null) {
                ((MusicCell) holder.itemView).setMusic(music, position != getItemCount() - 1);
            }
        }
    }

    @SuppressLint("ViewConstructor")
    private static class MusicCell extends FrameLayout {

        private final TextView titleTextView;
        private final TextView subtitleTextView;
        private final TextView durationTextView;
        private final FrameLayout iconBackground;
        private final ImageView imageView;
        private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Theme.ResourcesProvider resourcesProvider;
        private boolean needDivider;

        private MusicCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setWillNotDraw(false);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));

            iconBackground = new FrameLayout(context);
            addView(iconBackground, LayoutHelper.createFrame(44, 44, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 14, 0, 14, 0));

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(R.drawable.files_music);
            iconBackground.addView(imageView, LayoutHelper.createFrame(26, 26, Gravity.CENTER));

            titleTextView = new TextView(context);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleTextView.setTypeface(AndroidUtilities.bold());
            titleTextView.setSingleLine(true);
            titleTextView.setEllipsize(TextUtils.TruncateAt.END);
            titleTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 24, Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), LocaleController.isRTL ? 68 : 72, 9, LocaleController.isRTL ? 72 : 68, 0));

            subtitleTextView = new TextView(context);
            subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            subtitleTextView.setSingleLine(true);
            subtitleTextView.setEllipsize(TextUtils.TruncateAt.END);
            subtitleTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(subtitleTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 22, Gravity.TOP | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), LocaleController.isRTL ? 68 : 72, 33, LocaleController.isRTL ? 72 : 68, 0));

            durationTextView = new TextView(context);
            durationTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            durationTextView.setSingleLine(true);
            durationTextView.setGravity((LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
            addView(durationTextView, LayoutHelper.createFrame(54, 22, Gravity.TOP | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT), 12, 33, 12, 0));

            updateColors();
        }

        private void setMusic(MusicData music, boolean divider) {
            MusicMetaData metaData = music.getMusicMetaData();
            String title = getTitle(metaData);
            String performer = getPerformer(metaData);

            titleTextView.setText(title);
            subtitleTextView.setText(performer);
            durationTextView.setText(getDuration(metaData));
            setContentDescription(LocaleController.formatString("AccDescrMusicInfo", R.string.AccDescrMusicInfo, performer, title));
            needDivider = divider;
            invalidate();
        }

        private static String getTitle(MusicMetaData metaData) {
            String title = metaData.getTitle();
            if (!isNullObject(title, MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getTitle())) {
                return title.replace('\n', ' ');
            }
            String fileName = metaData.getFileName();
            if (!isNullObject(fileName, MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getFileName())) {
                return fileName.replace('\n', ' ');
            }
            return LocaleController.getString(R.string.AudioUnknownTitle);
        }

        private static String getPerformer(MusicMetaData metaData) {
            String performer = metaData.getPerformer();
            if (!isNullObject(performer, MusicMetaData.MUSIC_META_DATA_NULL_OBJECT.getPerformer())) {
                return performer.replace('\n', ' ');
            }
            return LocaleController.getString(R.string.AudioUnknownArtist);
        }

        private static String getDuration(MusicMetaData metaData) {
            int duration = (int) Math.ceil(Math.max(0, metaData.getDurationSec()));
            return duration > 0 ? AndroidUtilities.formatShortDuration(duration) : "-:--";
        }

        private static boolean isNullObject(String value, String nullObject) {
            return TextUtils.isEmpty(value) || TextUtils.equals(value, nullObject);
        }

        private void updateColors() {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
            if (iconBackground != null) {
                Drawable background = Theme.createRoundRectDrawable(dp(14), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider), 34));
                iconBackground.setBackground(background);
            }
            if (imageView != null) {
                imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider), PorterDuff.Mode.SRC_IN));
            }
            if (titleTextView != null) {
                titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            }
            if (subtitleTextView != null) {
                subtitleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
            }
            if (durationTextView != null) {
                durationTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
            }
            dividerPaint.setColor(Theme.getColor(Theme.key_divider, resourcesProvider));
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                int start = dp(72);
                if (LocaleController.isRTL) {
                    canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - start, getMeasuredHeight() - 1, dividerPaint);
                } else {
                    canvas.drawLine(start, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, dividerPaint);
                }
            }
        }
    }
}
