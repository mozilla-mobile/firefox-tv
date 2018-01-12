/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.Direction;
import org.mozilla.focus.utils.Edge;

import java.util.HashSet;
import java.util.Set;

public class Cursor extends View {

    private float CURSOR_SIZE = 45;
    private final int MAX_SPEED = 25;
    private final double FRICTION = 0.98;

    public CursorEvent cursorEvent;
    private Paint paint;
    private int x;
    private int y;
    private float speed = 0;
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
        paint.setAlpha(102);
        paint.setAntiAlias(true);
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    public float getSpeed() {
        return speed;
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
            speed = 0;
        }
    }

    private void move() {
        speed++;
        speed *= FRICTION;
        speed = Math.min(MAX_SPEED, speed);
        boolean isMovingDiagnol = activeDirections.size() > 1;
        float moveSpeed = isMovingDiagnol ? speed / 2 : speed;

        for (Direction direction : activeDirections) {
            moveOneDirection(direction, Math.round(moveSpeed));
        }

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

        paint.setShader(new RadialGradient(x, y, 45f, getResources().getColor(R.color.teal50), getResources().getColor(R.color.photonBlue50), Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, CURSOR_SIZE, paint);
    }

}
