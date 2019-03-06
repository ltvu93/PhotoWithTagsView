package com.ltvu93.photowithtagsview.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;

public class ZoomableImageView extends AppCompatImageView {

    private Matrix imageMatrix = new Matrix();

    private int originalWidth;
    private int originalHeight;

    private boolean isLockReLayout;

    private float minScale;
    private float maxScale;
    private float currentScale;

    private boolean isZoomChanging;

    // Scroller for smooth fling.
    // Link: https://developer.android.com/training/gestures/scroll.html
    private OverScroller mOverScroller = new OverScroller(getContext());
    private int lastFlingX;
    private int lastFlingY;

    private DoubleTapZoom doubleTapZoom = new DoubleTapZoom();

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private OnMatrixChangedListener onMatrixChangedListener;
    private OnImageClickedListener onImageClickedListener;
    private OnInsideImageClickedListener onInsideImageClickedListener;

    public ZoomableImageView(Context context) {
        super(context);

        initView();

    }

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initView();
    }

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initView();
    }

    private void initView() {
        setScaleType(ScaleType.MATRIX);

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        Drawable drawable = getDrawable();

        if (drawable != null) {
            originalWidth = drawable.getIntrinsicWidth();
            originalHeight = drawable.getIntrinsicHeight();
        } else {
            originalWidth = widthSize;
            originalHeight = heightSize;
        }

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(widthSize, originalWidth);
        } else {
            width = originalWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(heightSize, originalHeight);
        } else {
            height = originalHeight;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!isLockReLayout) {
            int width = getWidth();
            int height = getHeight();
            fitImageInView(width, height);
            isLockReLayout = true;
        }
    }

    private void fitImageInView(int viewWidth, int viewHeight) {
        float scaleX = viewWidth / (float) originalWidth;
        float scaleY = viewHeight / (float) originalHeight;

        float scale = Math.min(scaleX, scaleY);

        imageMatrix.reset();
        imageMatrix.setScale(scale, scale);
        imageMatrix.postTranslate(
                viewWidth / 2f - originalWidth * scale / 2,
                viewHeight / 2f - originalHeight * scale / 2);
        setImageMatrix(imageMatrix);

        minScale = scale;
        maxScale = minScale * 5;
        currentScale = minScale;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        scaleGestureDetector.onTouchEvent(event);

        if (!scaleGestureDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }

        return super.onTouchEvent(event);
    }

    /**
     * Compute fling and double tap zoom animation.
     */
    @Override
    public void computeScroll() {

        if (mOverScroller.computeScrollOffset()) {
            int currentFlingX = mOverScroller.getCurrX();
            int currentFlingY = mOverScroller.getCurrY();

            int offsetX = currentFlingX - lastFlingX;
            int offsetY = currentFlingY - lastFlingY;

            moveImage(offsetX, offsetY);

            lastFlingX = currentFlingX;
            lastFlingY = currentFlingY;

            ViewCompat.postInvalidateOnAnimation(this);
        }

        if (doubleTapZoom.computeZoom()) {
            scaleImage(
                    doubleTapZoom.getCurrentZoom() / currentScale,
                    doubleTapZoom.getFocusX(),
                    doubleTapZoom.getFocusY());

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);

        if (onMatrixChangedListener != null) {
            onMatrixChangedListener.onMatrixChanged(new Matrix(matrix));
        }
    }

    private void moveImage(float distanceX, float distanceY) {
        imageMatrix.postTranslate(distanceX, distanceY);
        setImageMatrix(imageMatrix);

        fixTrans();
    }

    private void scaleImage(float scaleFactor, float x, float y) {

        float oldScale = currentScale;
        currentScale *= scaleFactor;
        if (currentScale < minScale) {
            currentScale = minScale;
        } else if (currentScale > maxScale) {
            currentScale = maxScale;
        }
        scaleFactor = currentScale / oldScale;

        imageMatrix.postScale(scaleFactor, scaleFactor, x, y);
        setImageMatrix(imageMatrix);
        fixTrans();
    }

    private void fixTrans() {
        float transX = getMatrixValue(getImageMatrix(), Matrix.MTRANS_X);
        float transY = getMatrixValue(getImageMatrix(), Matrix.MTRANS_Y);

        float fixTransX = getFixTrans(transX, getMinTransX(), getMaxTransX());
        float fixTransY = getFixTrans(transY, getMinTransY(), getMaxTransY());

        imageMatrix.postTranslate(fixTransX, fixTransY);
        setImageMatrix(imageMatrix);
    }

    private float[] matrixValues = new float[9];

    private float getMatrixValue(Matrix matrix, int whichValue) {
        matrix.getValues(matrixValues);
        return matrixValues[whichValue];
    }

    private float getFixTrans(float trans, float minTrans, float maxTrans) {

        if (trans < minTrans) return minTrans - trans;
        if (trans > maxTrans) return maxTrans - trans;

        return 0.0f;
    }

    private float getMinTransX() {
        return calculateMinTrans(getWidth(), getCurrentWidth());
    }

    private float getMinTransY() {
        return calculateMinTrans(getHeight(), getCurrentHeight());
    }

    private float calculateMinTrans(float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return (viewSize - contentSize) / 2;
        } else {
            return viewSize - contentSize;
        }
    }

    private float getMaxTransX() {
        return calculateMaxTrans(getWidth(), getCurrentWidth());
    }

    private float getMaxTransY() {
        return calculateMaxTrans(getHeight(), getCurrentHeight());
    }

    private float calculateMaxTrans(float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return (viewSize - contentSize) / 2;
        } else {
            return 0;
        }
    }

    private float getCurrentWidth() {
        return originalWidth * currentScale;
    }

    private float getCurrentHeight() {
        return originalHeight * currentScale;
    }

    private void fling(int velocityX, int velocityY) {

        final int startX = (int) getMatrixValue(getImageMatrix(), Matrix.MTRANS_X);
        final int startY = (int) getMatrixValue(getImageMatrix(), Matrix.MTRANS_Y);

        lastFlingX = startX;
        lastFlingY = startY;

        mOverScroller.forceFinished(true);
        mOverScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                (int) getMinTransX(),
                (int) getMaxTransX(),
                (int) getMinTransY(),
                (int) getMaxTransY());

        // Trigger computeScroll function
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void doubleTapZoom(float focusX, float focusY) {
        float endZoom;
        if (currentScale == minScale)
            endZoom = maxScale;
        else
            endZoom = minScale;

        doubleTapZoom.initZoom(focusX, focusY, currentScale, endZoom);

        ViewCompat.postInvalidateOnAnimation(this);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            moveImage(-distanceX, -distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) velocityX, (int) velocityY);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            doubleTapZoom(e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (getDrawable() != null) {
                final float x = e.getX();
                final float y = e.getY();

                if (onImageClickedListener != null) {
                    onImageClickedListener.onImageClickedListener(x, y);
                }

                if (onInsideImageClickedListener != null) {
                    RectF currentImageBound = getImageBound();
                    if (currentImageBound.contains(x, y)) {
                        onInsideImageClickedListener.onInsideImageClickedListener(x, y);
                    }
                }
            }

            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isZoomChanging = true;
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float span = detector.getCurrentSpan() - detector.getPreviousSpan();
            if (isZoomChanging && span != 0) {
                scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                return true;
            }

            return super.onScale(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isZoomChanging = false;
            super.onScaleEnd(detector);
        }
    }

    public void setLockReLayout(boolean lockReLayout) {
        isLockReLayout = lockReLayout;
    }

    public RectF getImageBound() {
        final float transX = getMatrixValue(getImageMatrix(), Matrix.MTRANS_X);
        final float transY = getMatrixValue(getImageMatrix(), Matrix.MTRANS_Y);

        return new RectF(
                transX,
                transY,
                transX + getCurrentWidth(),
                transY + getCurrentHeight());
    }

    public void setOnMatrixChangedListener(OnMatrixChangedListener onMatrixChangedListener) {
        this.onMatrixChangedListener = onMatrixChangedListener;
    }

    public void setOnImageClickedListener(OnImageClickedListener onImageClickedListener) {
        this.onImageClickedListener = onImageClickedListener;
    }

    public void setOnInsideImageClickedListener(OnInsideImageClickedListener onInsideImageClickedListener) {
        this.onInsideImageClickedListener = onInsideImageClickedListener;
    }

    public interface OnMatrixChangedListener {
        void onMatrixChanged(Matrix matrix);
    }

    public interface OnImageClickedListener {
        void onImageClickedListener(float x, float y);
    }

    public interface OnInsideImageClickedListener {
        void onInsideImageClickedListener(float x, float y);
    }
}
