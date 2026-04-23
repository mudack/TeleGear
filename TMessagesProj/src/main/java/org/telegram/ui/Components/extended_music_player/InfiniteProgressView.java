package org.telegram.ui.Components.extended_music_player;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.InfiniteProgress;

public class InfiniteProgressView extends View {

    private final InfiniteProgress progress;
    private final int progressRadius;

    public InfiniteProgressView(Context context, int rad) {
        super(context);
        progressRadius = rad;
        progress = new InfiniteProgress(rad);
    }

    public void setColor(int color){
        progress.setColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = progressRadius * 2 + AndroidUtilities.dp(4); // + stroke

        int width = resolveSize(size, widthMeasureSpec);
        int height = resolveSize(size, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        progress.draw(canvas, cx, cy, 1f);

        postInvalidateOnAnimation();
    }
}