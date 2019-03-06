package com.ltvu93.photowithtagsview.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ltvu93.photowithtagsview.AppUtil;

import java.util.ArrayList;
import java.util.List;

public class PhotoWithTagsView extends FrameLayout {

    private ZoomableImageView zoomableImageView;

    private boolean isEditable;
    private boolean isTagVisible = true;

    private float touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    private boolean isDraggingTagView;
    private TagView dragTagView;
    private float dragStartX;
    private float dragStartY;

    private boolean isTagsChanged;

    public PhotoWithTagsView(@NonNull Context context) {
        super(context);

        initView(context);
    }

    public PhotoWithTagsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initView(context);
    }

    public PhotoWithTagsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initView(context);
    }

    private void initView(Context context) {
        zoomableImageView = new ZoomableImageView(context);
        zoomableImageView.setOnMatrixChangedListener(this::updateTagViewsPosition);
        zoomableImageView.setOnInsideImageClickedListener(this::onInsideImageClicked);

        addView(zoomableImageView);
    }

    private void updateTagViewsPosition(Matrix matrix) {
        for (int i = 1; i < getChildCount(); i++) {
            TagView tagView = (TagView) getChildAt(i);
            tagView.updatePosition(matrix);
        }
    }

    private void onInsideImageClicked(float x, float y) {
        if (isTagVisible && isEditable) {
            Matrix inverseMatrix = new Matrix();
            zoomableImageView.getImageMatrix().invert(inverseMatrix);
            PointF originalImageBasePosition = AppUtil.mapPoint(inverseMatrix, new PointF(x, y));

            TagView lastTagView = getLastTagView();
            if (lastTagView != null && TextUtils.isEmpty(lastTagView.getContent())) {
                lastTagView.setOriginalPosition(
                        (int) originalImageBasePosition.x, (int) originalImageBasePosition.y);
                lastTagView.updatePosition(zoomableImageView.getImageMatrix());
            } else {
                addTagView("", (int) originalImageBasePosition.x, (int) originalImageBasePosition.y);
            }

            if (!isTagsChanged) {
                isTagsChanged = true;
            }
        }
    }

    private TagView getLastTagView() {
        if (getChildCount() > 1) {
            return (TagView) getChildAt(getChildCount() - 1);
        } else {
            return null;
        }
    }

    public void addTagView(String content, int originalX, int originalY) {
        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        TagView tagView = new TagView(getContext());

        tagView.setLayoutParams(layoutParams);
        tagView.setParentView(this);
        tagView.setMatrix(zoomableImageView.getImageMatrix());
        tagView.setContent(content);
        tagView.setOriginalPosition(originalX, originalY);
        tagView.requestContentFocus();
        tagView.setVisibility(isTagVisible ? VISIBLE : INVISIBLE);
        tagView.setEditable(isEditable);
        tagView.setOnContentChangedListener(() -> {
            if (!isTagsChanged) {
                isTagsChanged = true;
            }
        });

        addView(tagView);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isTagVisible && isEditable) {
            float currentX = event.getX();
            float currentY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d("PhotoWithTagsView", "onInterceptTouchEvent ACTION_DOWN");

                    if (isInTagViewBound(currentX, currentY)) {
                        isDraggingTagView = true;
                        dragStartX = currentX;
                        dragStartY = currentY;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("PhotoWithTagsView", "onInterceptTouchEvent ACTION_MOVE");

                    if (isDraggingTagView) {
                        float xDiff = Math.abs(currentX - dragStartX);
                        float yDiff = Math.abs(currentY - dragStartY);
                        float diff = Math.max(xDiff, yDiff);
                        if (diff > touchSlop && dragTagView != null) {
                            return true;
                        }
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    Log.d("PhotoWithTagsView", "onInterceptTouchEvent ACTION_CANCEL");

                    isDraggingTagView = false;
                    dragTagView = null;
                    break;
            }
        }

        return super.onInterceptTouchEvent(event);
    }

    private boolean isInTagViewBound(float x, float y) {
        for (int i = getChildCount() - 1; i >= 1; i--) {
            TagView tagView = (TagView) getChildAt(i);
            if (tagView.getViewBound().contains(x, y)) {
                dragTagView = tagView;
                return true;
            }
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isTagVisible && isEditable) {
            if (!isTagsChanged) {
                isTagsChanged = true;
            }

            float currentX = event.getX();
            float currentY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    Log.d("PhotoWithTagsView", "onTouchEvent ACTION_MOVE");

                    RectF imageBound = zoomableImageView.getImageBound();
                    if (currentX < imageBound.left) {
                        currentX = imageBound.left;
                    } else if (currentX > imageBound.right) {
                        currentX = imageBound.right;
                    }

                    if (currentY < imageBound.top) {
                        currentY = imageBound.top;
                    } else if (currentY > imageBound.bottom) {
                        currentY = imageBound.bottom;
                    }

                    dragTagView.setTranslationX(currentX - dragTagView.getWidth() / 2f);
                    dragTagView.setTranslationY(currentY);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    Log.d("PhotoWithTagsView", "onTouchEvent ACTION_CANCEL");

                    // Convert current tag view position to original image base position
                    // in order to prevent flick after drag.
                    Matrix invertMatrix = new Matrix();
                    zoomableImageView.getImageMatrix().invert(invertMatrix);
                    PointF viewPosition = new PointF(
                            dragTagView.getTranslationX() + dragTagView.getWidth() / 2f,
                            dragTagView.getTranslationY());
                    PointF newOriginalPosition = AppUtil.mapPoint(
                            invertMatrix, new PointF(viewPosition.x, viewPosition.y));
                    dragTagView.setOriginalPosition((int) newOriginalPosition.x, (int) newOriginalPosition.y);

                    Log.d("OriginalPosition", dragTagView.getOriginalX() + " " + dragTagView.getOriginalY());

                    isDraggingTagView = false;
                    dragTagView = null;

                    return true;
            }
        }

        return super.onTouchEvent(event);
    }

    public void setImageBitmap(Bitmap bitmap) {
        zoomableImageView.setImageBitmap(bitmap);
        zoomableImageView.setLockReLayout(false);
        zoomableImageView.requestLayout();
    }

    public void setEditable(boolean editable) {
        isEditable = editable;

        for (int i = 1; i < getChildCount(); i++) {
            TagView tagView = (TagView) getChildAt(i);
            tagView.setEditable(editable);
            if (i == getChildCount() - 1) {
                tagView.requestContentFocus();
            }
        }
    }

    public void setTagsVisible(boolean isVisible) {
        isTagVisible = isVisible;

        for (int i = 1; i < getChildCount(); i++) {
            TagView tagView = (TagView) getChildAt(i);
            tagView.setVisibility(isVisible ? VISIBLE : INVISIBLE);
            if (i == getChildCount() - 1) {
                tagView.requestContentFocus();
            }
        }
    }

    public List<TagView> getTagViews() {
        List<TagView> tagViews = new ArrayList<>();
        for (int i = 1; i < getChildCount(); i++) {
            TagView tagView = (TagView) getChildAt(i);
            tagViews.add(tagView);
        }
        return tagViews;
    }


    public void setOnClickedListener(ZoomableImageView.OnImageClickedListener onImageClickedListener) {
        zoomableImageView.setOnImageClickedListener(onImageClickedListener);
    }

    public boolean isTagsChanged() {
        return isTagsChanged;
    }
}
