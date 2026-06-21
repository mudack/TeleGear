package org.telegram.ui.extended_music_screens.playlists;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.CustomPhoneKeyboardView;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ProxyDrawable;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.SlideView;
import org.telegram.ui.Components.TransformableLoginButtonView;
import org.telegram.ui.Components.VerticalPositionAutoAnimator;
import org.telegram.ui.LoginActivity;
import org.telegram.ui.ProxyListActivity;

public class PlaylistActivity extends BaseFragment{

    private SizeNotifierFrameLayout sizeNotifierFrameLayout;

    private ActionBarMenuItem addPlaylistMenuItem;

    @Override
    public View createView(Context context) {

        sizeNotifierFrameLayout = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        fragmentView = sizeNotifierFrameLayout;
        actionBar.setAddToContainer(false);
        ActionBarMenu menu = actionBar.createMenu();
        addPlaylistMenuItem = menu.addItem(create_new_playlist, R.drawable.msg_add);
        addPlaylistMenuItem.setContentDescription(LocaleController.getString(R.string.playlist_screen_item_create_playlist));
        addPlaylistMenuItem.setVisibility(View.VISIBLE);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                Toast.makeText(context, "id " + id, Toast.LENGTH_SHORT).show();
//                if (id == 0) {
//                    onDoneButtonPressed();
//                } else if (id == -1) {
//                    if (onBackPressed(true)) {
//                        finishFragment();
//                    }
//                }
            }
        });


        return fragmentView = sizeNotifierFrameLayout;
    }

    @Override
    public void setInMenuMode(boolean value) {
        super.setInMenuMode(value);
        if (actionBar != null) {
            actionBar.createMenu().setVisibility(inMenuMode ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void clearViews() {
        if (fragmentView != null) {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                try {
                    onRemoveFromParent();
                    parent.removeViewInLayout(fragmentView);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            fragmentView = null;
        }
        if (actionBar != null) {
            actionBar = null;
        }
//        clearSheets();
        parentLayout = null;
    }

    private final static int create_new_playlist = 10;
}
