/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.Direction;
import org.mozilla.focus.utils.Edge;

import java.util.HashSet;
import java.util.Set;

public class Cursor extends View {

    private final int VELOCITY = 4;
    private float CURSOR_SIZE = 45;
    private final int MAX_SPEED = 15;

    public CursorEvent cursorEvent;
    private Paint paint;
    private int x;
    private int y;
    private int speed = 1;
    private Set<Direction> activeDirections = new HashSet<>();
    private int maxHeight;
    private int maxWidth;

    private boolean isInit;
    private boolean moving;

    // Make sure we run the update on the main thread
    private Handler handler = new Handler();
    private Runnable tick = new Runnable() {
        @Override
        public void run() {
            move();
            handler.postDelayed(this, 20);
        }
    };


    public Cursor(Context context, AttributeSet attrs) {
        super(context, attrs);

        isInit = true;
        // create the Paint and set its color
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.colorProgressGradientEnd));
        paint.setAlpha(100);
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public void moveCursor(Direction direction) {
        activeDirections.add(direction);
        
        if (!moving) {
            moving = true;
            handler.post(tick);
        }
    }

    public void stopMoving(Direction direction) {
        activeDirections.remove(direction);
        if (activeDirections.size() == 0) {
            handler.removeCallbacks(tick);
            moving = false;
        }
    }

    private void move() {
        for (Direction direction : activeDirections) {
            moveOneDirection(direction, speed);
        }

        speed = Math.min(MAX_SPEED, speed + VELOCITY);
        invalidate();
    }

    private void moveOneDirection(Direction direction, int amount) {
        switch (direction) {
            case DOWN:
                if (y >= (maxHeight - CURSOR_SIZE)) {
                    cursorEvent.cursorHitEdge(Edge.BOTTOM);
                    return;
                }

                y = y + amount;
                break;
            case LEFT:
                if (x <= (0 + CURSOR_SIZE)) {
                    cursorEvent.cursorHitEdge(Edge.LEFT);
                    return;
                }

                x = x - amount;
                break;
            case RIGHT:
                if (x >= (maxWidth - CURSOR_SIZE)) {
                    cursorEvent.cursorHitEdge(Edge.RIGHT);
                    return;
                }
                x = x + amount;
                break;
            case UP:
                if (y <= (0 + CURSOR_SIZE)) {
                    cursorEvent.cursorHitEdge(Edge.TOP);
                    return;
                }

                y = y - amount;
                break;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (isInit) {
            maxHeight = getHeight();
            maxWidth = getWidth();
            x = maxWidth / 2;
            y = maxHeight / 2;
            isInit = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(x, y, CURSOR_SIZE, paint);
    }

}
