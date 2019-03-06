package com.ltvu93.photowithtagsview.widget;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

class DoubleTapZoom {

    private Interpolator interpolator;

    private int animationDurationMillis;

    private boolean isFinish = true;

    private long startTimeMillis;

    private float focusX;
    private float focusY;

    private float startZoom;
    private float endZoom;
    private float currentZoom;

    DoubleTapZoom() {
        interpolator = new DecelerateInterpolator();
        animationDurationMillis = 300;
    }

    public float getFocusX() {
        return focusX;
    }

    public float getFocusY() {
        return focusY;
    }

    public void initZoom(float focusX, float focusY, float startZoom, float endZoom) {
        this.focusX = focusX;
        this.focusY = focusY;

        this.startZoom = startZoom;
        this.endZoom = endZoom;
        currentZoom = startZoom;

        startTimeMillis = System.currentTimeMillis();
        isFinish = false;
    }

    public float getCurrentZoom() {
        return currentZoom;
    }

    public boolean computeZoom() {
        if (isFinish) {
            return false;
        }

        long currentTimeMillis = System.currentTimeMillis();
        long elapsedTime = currentTimeMillis - startTimeMillis;

        if (elapsedTime < animationDurationMillis) {
            float t = interpolator.getInterpolation((float) elapsedTime / animationDurationMillis);
            currentZoom = (startZoom + t * (endZoom - startZoom));
        } else{
            currentZoom = endZoom;
            isFinish = true;
        }

        return true;
    }

    public void forceFinished(boolean finished) {
        isFinish = finished;
    }

    public void abortAnimation() {
        isFinish = true;
    }
}
