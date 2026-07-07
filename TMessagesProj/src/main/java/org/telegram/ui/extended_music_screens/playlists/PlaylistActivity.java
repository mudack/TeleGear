package org.telegram.ui.extended_music_screens.playlists;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.MainTabsActivity.ARGS_NAME_HAS_MAIN_TABS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.extended_music_player.GlobalMusicControllerImpl;
import org.telegram.messenger.extended_music_player.entity.Playlist;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EditTextCell;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.FragmentSearchField;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerAnimationScrollHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.HeaderShadowView;
import org.telegram.ui.MainTabsActivity;

import java.util.ArrayList;
import java.util.Locale;

public class PlaylistActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate, MainTabsActivity.TabFragmentDelegate {

    private static final boolean ARGS_NAME_HAS_MAIN_TABS_DEFAULT_VALUE = false;
    public static final int MAX_LENGTH_OF_PLAYLIST_NAME = 100;

    public PlaylistActivity() {
        super();
    }

    public PlaylistActivity(Bundle args) {
        super(args);
    }

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private RecyclerAnimationScrollHelper scrollHelper;
    private PlaylistAdapter listAdapter;
    private EmptyTextProgressView emptyView;
    private FragmentSearchField searchField;
    private FragmentFloatingButton floatingButton;
    private HeaderShadowView headerShadowView;
    private FragmentContextView fragmentContextView;
    private FrameLayout fragmentContextViewWrapper;
    private ItemTouchHelper itemTouchHelper;

    private final ArrayList<Playlist> playlists = new ArrayList<>();
    private final ArrayList<Playlist> filteredPlaylists = new ArrayList<>();

    private boolean hasMainTabs;
    private boolean loading = true;
    private String query = "";
    private int navigationBarHeight;
    private int additionNavigationBarHeight;
    private int additionFloatingButtonOffset;
    private boolean playlistOrderChanged;

    private GlobalMusicControllerImpl globalMusicController;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        if (arguments != null) {
            hasMainTabs = arguments.getBoolean(ARGS_NAME_HAS_MAIN_TABS, ARGS_NAME_HAS_MAIN_TABS_DEFAULT_VALUE);
        } else {
            hasMainTabs = ARGS_NAME_HAS_MAIN_TABS_DEFAULT_VALUE;
        }

        additionNavigationBarHeight = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS) : 0;
        additionFloatingButtonOffset = hasMainTabs ? dp(DialogsActivity.MAIN_TABS_HEIGHT + DialogsActivity.MAIN_TABS_MARGIN) : 0;

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.musicLoadListOfPlaylist);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.musicPlaylistCreated);
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.musicDatabaseError);

        globalMusicController = GlobalMusicControllerImpl.getInstance();
        globalMusicController.getAllPlaylists();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.musicLoadListOfPlaylist);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.musicPlaylistCreated);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.musicDatabaseError);
    }

    @Override
    public View createView(Context context) {
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getString(R.string.MainTabsPlaylist));
        if (!hasMainTabs) {
            actionBar.setBackButtonDrawable(new BackDrawable(false));
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        SizeNotifierFrameLayout contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);
                if (searchField != null) {
                    LayoutParams searchParams = (LayoutParams) searchField.getLayoutParams();
                    searchParams.topMargin = actionBar.getMeasuredHeight() + getFragmentContextViewHeight() + dp(6);
                }
                if (emptyView != null) {
                    LayoutParams emptyParams = (LayoutParams) emptyView.getLayoutParams();
                    emptyParams.topMargin = actionBar.getMeasuredHeight() + getFragmentContextViewHeight() + dp(52);
                }
                if (headerShadowView != null) {
                    LayoutParams shadowParams = (LayoutParams) headerShadowView.getLayoutParams();
                    shadowParams.topMargin = actionBar.getMeasuredHeight() + getFragmentContextViewHeight();
                }
                if (fragmentContextViewWrapper != null) {
                    LayoutParams contextParams = (LayoutParams) fragmentContextViewWrapper.getLayoutParams();
                    contextParams.topMargin = actionBar.getMeasuredHeight();
                }
                checkListViewPadding();
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        fragmentView = contentView;
        fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        emptyView = new EmptyTextProgressView(context, null, resourceProvider);
        emptyView.setText(getString(R.string.playlist_no_local_playlist_text));
        emptyView.setShowAtCenter(true);
        if (loading) {
            emptyView.showProgress(false);
        } else {
            emptyView.showTextView();
        }
        contentView.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context, resourceProvider);
        listView.setClipToPadding(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter = new PlaylistAdapter(context));
        listView.setEmptyView(emptyView);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listView.setOnItemClickListener((view, position) -> {
            Playlist playlist = getPlaylistAt(position);
            if (playlist == null) {
                return;
            }
            presentFragment(new DetailedPlaylistActivity(DetailedPlaylistActivity.createArgs(playlist)));
        });
        listView.setOnItemLongClickListener((view, position) -> {
            Playlist playlist = getPlaylistAt(position);
            if (playlist == null) {
                return false;
            }
            showPlaylistOptionsDialog(playlist);
            return true;
        });
        setupPlaylistReorderHelper();
        scrollHelper = new RecyclerAnimationScrollHelper(listView, layoutManager);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        searchField = new FragmentSearchField(context, resourceProvider);
        searchField.editText.setHint(getString(R.string.Search));
        searchField.editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString();
                updateFilteredPlaylists();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        contentView.addView(searchField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 42, Gravity.TOP | Gravity.FILL_HORIZONTAL, 12, 0, 12, 0));

        floatingButton = new FragmentFloatingButton(context, resourceProvider);
        floatingButton.setImageResource(R.drawable.ic_add_playlist);
        floatingButton.setContentDescription(getString(R.string.playlist_screen_item_create_playlist));
        floatingButton.setOnClickListener(v -> showCreateNewPlaylistDialog());
        contentView.addView(floatingButton, FragmentFloatingButton.createDefaultLayoutParams());

        contentView.addView(actionBar);

        fragmentContextViewWrapper = new FrameLayout(context);
        fragmentContextViewWrapper.setVisibility(View.GONE);
        fragmentContextView = new FragmentContextView(context, this, false, resourceProvider) {
            @Override
            public void setVisibility(int visibility) {
                if (fragmentContextViewWrapper != null) {
                    fragmentContextViewWrapper.setVisibility(visibility);
                    checkListViewPadding();
                    if (fragmentView != null) {
                        fragmentView.requestLayout();
                    }
                }
            }
        };
        fragmentContextView.setSupportsCalls(false);
        fragmentContextView.isInsideBubble = true;
        fragmentContextViewWrapper.addView(fragmentContextView);
        contentView.addView(fragmentContextViewWrapper, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT));

        headerShadowView = new HeaderShadowView(context, parentLayout);
        headerShadowView.setShadowVisible(false, false);
        contentView.addView(headerShadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 5, Gravity.TOP));

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                View topChild = recyclerView.getChildAt(0);
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int firstViewTop = topChild != null ? topChild.getTop() : 0;
                headerShadowView.setShadowVisible(!(firstVisibleItem == 0 && firstViewTop >= listView.getPaddingTop()), true);
                if (dy != 0) {
                    floatingButton.setButtonVisible(dy < 0, true);
                }
            }
        });

        actionBar.setAdaptiveBackground(listView);
        actionBar.setDrawBlurBackground(contentView);
        ViewCompat.setOnApplyWindowInsetsListener(fragmentView, this::onApplyWindowInsets);

        updateFilteredPlaylists();
        checkListViewPadding();
        checkFloatingButtonPosition();
        return fragmentView;
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        actionBar.setUseContainerForTitles();
        actionBar.getTitlesContainer().setTranslationX(dp(4));
        actionBar.setAddToContainer(false);
        return actionBar;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.musicLoadListOfPlaylist) {
            loading = false;
            playlists.clear();
            if (args.length > 0 && args[0] instanceof ArrayList<?>) {
                for (Object item : (ArrayList<?>) args[0]) {
                    if (item instanceof Playlist) {
                        playlists.add((Playlist) item);
                    }
                }
            }
            updateFilteredPlaylists();
        } else if (id == NotificationCenter.musicPlaylistCreated) {
            if (globalMusicController != null) {
                globalMusicController.getAllPlaylists();
            }
        } else if (id == NotificationCenter.musicDatabaseError) {
            loading = false;
            updateEmptyView();
            // TODO: musicDatabaseError is global for every GlobalMusicController operation.
            // Pass an operation/request marker or use dedicated notifications before production,
            // otherwise this screen can show unrelated errors from player/add-to-playlist flows.
            if (getParentActivity() != null && args.length > 0) {
                Toast.makeText(getParentActivity(), LocaleController.formatString(R.string.playlist_error_message, String.valueOf(args[0])), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onParentScrollToTop() {
        if (listView == null || layoutManager == null || scrollHelper == null) {
            return;
        }
        if (layoutManager.findFirstVisibleItemPosition() < 15) {
            listView.smoothScrollToPosition(0);
        } else {
            scrollHelper.setScrollDirection(RecyclerAnimationScrollHelper.SCROLL_DIRECTION_UP);
            scrollHelper.scrollToPosition(0, 0, false, true);
        }
    }

    private WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
        navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        checkListViewPadding();
        checkFloatingButtonPosition();
        return WindowInsetsCompat.CONSUMED;
    }

    private void checkListViewPadding() {
        if (listView == null || actionBar == null) {
            return;
        }
        listView.setPadding(
                0,
                actionBar.getMeasuredHeight() + getFragmentContextViewHeight() + dp(56),
                0,
                navigationBarHeight + additionNavigationBarHeight + dp(10)
        );
        if (emptyView != null) {
            emptyView.setPadding(0, 0, 0, navigationBarHeight + additionNavigationBarHeight);
        }
    }

    private int getFragmentContextViewHeight() {
        return fragmentContextViewWrapper != null && fragmentContextViewWrapper.getVisibility() == View.VISIBLE ? dp(36) : 0;
    }

    private void checkFloatingButtonPosition() {
        if (floatingButton != null) {
            floatingButton.setTranslationY(-navigationBarHeight - additionFloatingButtonOffset);
        }
    }

    private void updateFilteredPlaylists() {
        filteredPlaylists.clear();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.US);
        String normalizedTranslitQuery = LocaleController.getInstance().getTranslitString(normalizedQuery).toLowerCase(Locale.US);
        if (TextUtils.isEmpty(normalizedQuery)) {
            filteredPlaylists.addAll(playlists);
        } else {
            for (int i = 0; i < playlists.size(); i++) {
                Playlist playlist = playlists.get(i);
                String name = playlist.getName();
                if (name == null) {
                    continue;
                }
                String normalizedName = name.toLowerCase(Locale.US);
                String normalizedTranslitName = LocaleController.getInstance().getTranslitString(normalizedName).toLowerCase(Locale.US);
                if (normalizedName.contains(normalizedQuery) ||
                        normalizedTranslitName.contains(normalizedQuery) ||
                        normalizedName.contains(normalizedTranslitQuery)) {
                    filteredPlaylists.add(playlist);
                }
            }
        }
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (emptyView == null) {
            return;
        }
        if (loading) {
            emptyView.showProgress();
            return;
        }
        if (TextUtils.isEmpty(query)) {
            emptyView.setText(getString(R.string.playlist_no_local_playlist_text));
        } else {
            emptyView.setText(LocaleController.formatString(R.string.NoResultFoundFor, query));
        }
        emptyView.showTextView();
    }

    private Playlist getPlaylistAt(int position) {
        if (position < 0 || position >= filteredPlaylists.size()) {
            return null;
        }
        return filteredPlaylists.get(position);
    }

    private void setupPlaylistReorderHelper() {
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (!canReorderPlaylists()) {
                    return 0;
                }
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                if (!canReorderPlaylists()) {
                    return false;
                }
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION ||
                        fromPosition < 0 || toPosition < 0 ||
                        fromPosition >= playlists.size() || toPosition >= playlists.size()) {
                    return false;
                }
                movePlaylist(fromPosition, toPosition);
                playlistOrderChanged = true;
                listAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                if (viewHolder != null) {
                    listView.hideSelector(false);
                }
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    listView.cancelClickRunnables(false);
                    if (viewHolder != null) {
                        viewHolder.itemView.setPressed(true);
                    }
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setPressed(false);
                if (playlistOrderChanged) {
                    playlistOrderChanged = false;
                    savePlaylistOrder();
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(listView);
    }

    private boolean canReorderPlaylists() {
        String normalizedQuery = query == null ? "" : query.trim();
        return TextUtils.isEmpty(normalizedQuery) && playlists.size() > 1;
    }

    private void movePlaylist(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }
        Playlist playlist = playlists.remove(fromPosition);
        playlists.add(toPosition, playlist);

        filteredPlaylists.clear();
        filteredPlaylists.addAll(playlists);
    }

    private void savePlaylistOrder() {
        if (globalMusicController == null) {
            return;
        }
        ArrayList<Integer> playlistIds = new ArrayList<>();
        for (int i = 0; i < playlists.size(); i++) {
            playlistIds.add(playlists.get(i).getId());
        }
        globalMusicController.updatePlaylistOrder(playlistIds);
    }

    private void showCreateNewPlaylistDialog() {
        showPlaylistTitleDialog(
                getString(R.string.playlist_new_playlist),
                getString(R.string.playlist_new_playlist_dialog_create_btn_text),
                null,
                playlistName -> globalMusicController.createPlaylist(playlistName)
        );
    }

    private void showRenamePlaylistDialog(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        String oldPlaylistName = playlist.getName() == null ? "" : playlist.getName();
        showPlaylistTitleDialog(
                getString(R.string.playlist_rename_playlist),
                getString(R.string.Rename),
                oldPlaylistName,
                playlistName -> {
                    if (playlistName.equals(oldPlaylistName.trim())) {
                        return;
                    }
                    globalMusicController.renamePlaylist(playlist.getId(), playlistName);
                }
        );
    }

    private void showPlaylistTitleDialog(String title, String positiveButton, String initialPlaylistName, PlaylistTitleCallback positiveCallback) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourceProvider);
        EditTextCell editText = new EditTextCell(context, getString(R.string.playlist_new_playlist_dialog_et_hint), false, false, MAX_LENGTH_OF_PLAYLIST_NAME, resourceProvider);
        if (!TextUtils.isEmpty(initialPlaylistName)) {
            editText.setText(initialPlaylistName);
        }
        editText.setShowLimitWhenEmpty(true);
        editText.setDivider(true);

        LinearLayout container = new LinearLayout(context);
        container.setPadding(LocaleController.isRTL ? dp(24) : 0, dp(24), LocaleController.isRTL ? 0 : dp(24), 0);
        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT)));

        builder.setTitle(title);
        builder.setView(container);
        builder.setPositiveButton(positiveButton, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            View button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String playlistName = editText.getText().toString().trim();
                if (playlistName.isEmpty()) {
                    Toast.makeText(context, getString(R.string.playlist_error_message_empty_playlist_name), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (positiveCallback != null) {
                    positiveCallback.run(playlistName);
                }
                dialog.dismiss();
            });
            editText.requestFocus();
            if (!TextUtils.isEmpty(initialPlaylistName)) {
                editText.editText.setSelection(0, editText.editText.length());
            }
            AndroidUtilities.showKeyboard(editText);
        });
        showDialog(dialog);
    }

    private void showPlaylistOptionsDialog(Playlist playlist) {
        Context context = getContext();
        if (context == null || playlist == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourceProvider);
        builder.setTitle(playlist.getName());
        builder.setItems(new CharSequence[]{
                getString(R.string.Rename),
                getString(R.string.Delete)
        }, (dialog, which) -> {
            if (which == 0) {
                showRenamePlaylistDialog(playlist);
            } else if (which == 1) {
                showDeletePlaylistDialog(playlist);
            }
        });
        showDialog(builder.create());
    }

    private void showDeletePlaylistDialog(Playlist playlist) {
        Context context = getContext();
        if (context == null || playlist == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourceProvider);
        builder.setTitle(getString(R.string.playlist_delete_playlist));
        builder.setMessage(LocaleController.formatString(R.string.playlist_delete_playlist_confirm, playlist.getName()));
        builder.setPositiveButton(getString(R.string.Delete), (dialog, which) -> {
            if (globalMusicController != null) {
                globalMusicController.deletePlaylist(playlist.getId());
            }
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        showDialog(dialog);
        dialog.redPositive();
    }

    private interface PlaylistTitleCallback {
        void run(String playlistName);
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
                    if (child instanceof PlaylistCell) {
                        ((PlaylistCell) child).updateColors();
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
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{PlaylistCell.class}, null, null, cellDelegate, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{PlaylistCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{PlaylistCell.class}, new String[]{"imageView"}, null, null, cellDelegate, Theme.key_featuredStickers_addButton));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{PlaylistCell.class}, new String[]{"moreView"}, null, null, cellDelegate, Theme.key_windowBackgroundWhiteGrayText3));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{PlaylistCell.class}, null, null, cellDelegate, Theme.key_divider));

        return themeDescriptions;
    }

    private class PlaylistAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        private PlaylistAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return filteredPlaylists.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new PlaylistCell(context, resourceProvider));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Playlist playlist = getPlaylistAt(position);
            if (playlist != null) {
                PlaylistCell cell = (PlaylistCell) holder.itemView;
                boolean showReorderHandle = canReorderPlaylists();
                View.OnTouchListener onReorderTouch = (v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN && itemTouchHelper != null && showReorderHandle) {
                        itemTouchHelper.startDrag(listView.getChildViewHolder(cell));
                    }
                    return false;
                };
                cell.setPlaylist(playlist, position != getItemCount() - 1, showReorderHandle, onReorderTouch);
            }
        }
    }

    @SuppressLint("ViewConstructor")
    private static class PlaylistCell extends FrameLayout {

        private final TextView nameTextView;
        private final FrameLayout iconBackground;
        private final ImageView imageView;
        private final ImageView moreView;
        private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Theme.ResourcesProvider resourcesProvider;
        private boolean needDivider;

        private PlaylistCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setWillNotDraw(false);
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
            setPadding(0, 0, 0, 0);

            iconBackground = new FrameLayout(context);
            addView(iconBackground, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 12, 0, 12, 0));

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageResource(R.drawable.files_music);
            iconBackground.addView(imageView, LayoutHelper.createFrame(28, 28, Gravity.CENTER));

            nameTextView = new TextView(context);
            nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameTextView.setTypeface(AndroidUtilities.bold());
            nameTextView.setSingleLine(true);
            nameTextView.setEllipsize(TextUtils.TruncateAt.END);
            nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), LocaleController.isRTL ? 64 : 72, 0, LocaleController.isRTL ? 72 : 64, 0));

            moreView = new ImageView(context);
            moreView.setScaleType(ImageView.ScaleType.CENTER);
            moreView.setImageResource(R.drawable.list_reorder);
            moreView.setContentDescription(LocaleController.getString(R.string.playlist_screen_item_reorder_playlist));
            addView(moreView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 6, 0, 6, 0));

            updateColors();
        }

        private void setPlaylist(Playlist playlist, boolean divider, boolean showReorderHandle, View.OnTouchListener onReorderTouchListener) {
            nameTextView.setText(playlist.getName());
            moreView.setVisibility(showReorderHandle ? View.VISIBLE : View.GONE);
            moreView.setOnTouchListener(showReorderHandle ? onReorderTouchListener : null);
            needDivider = divider;
            invalidate();
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
            if (nameTextView != null) {
                nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            }
            if (moreView != null) {
                moreView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider), PorterDuff.Mode.SRC_IN));
                moreView.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), 1, dp(20)));
            }
            dividerPaint.setColor(Theme.getColor(Theme.key_divider, resourcesProvider));
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(72) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
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
