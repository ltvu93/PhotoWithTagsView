package com.ltvu93.photowithtagsview.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ltvu93.photowithtagsview.AppUtil;
import com.ltvu93.photowithtagsview.R;

public class TagView extends FrameLayout {

    private EditText etContent;
    private ImageView ivRemove;

    private ViewGroup parentView;

    private Matrix matrix;

    private int originalX;
    private int originalY;

    private OnContentChangedListener onContentChangedListener;

    public TagView(@NonNull Context context) {
        super(context);

        init();
    }

    public TagView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public TagView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        View rootView = LayoutInflater.from(getContext()).inflate(R.layout.item_tag, this);

        etContent = rootView.findViewById(R.id.item_tag_et_content);
        ivRemove = rootView.findViewById(R.id.item_tag_iv_remove);

        etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(onContentChangedListener != null){
                    onContentChangedListener.onContentChanged();
                }
            }
        });
        ivRemove.setOnClickListener(v -> parentView.removeView(this));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updatePosition(this.matrix);
    }

    public void updatePosition(Matrix matrix) {
        this.matrix = matrix;

        PointF viewPosition = AppUtil.mapPoint(matrix, new PointF(originalX, originalY));
        setTranslationX(viewPosition.x - getWidth() / 2f);
        setTranslationY(viewPosition.y);
    }

    public void setParentView(ViewGroup parentView) {
        this.parentView = parentView;
    }

    public void setOriginalPosition(int x, int y) {
        this.originalX = x;
        this.originalY = y;
    }

    public int getOriginalX() {
        return originalX;
    }

    public int getOriginalY() {
        return originalY;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
    }

    public void setEditable(boolean editable) {
        ivRemove.setVisibility(editable ? VISIBLE : INVISIBLE);
        etContent.setEnabled(editable);
    }

    public RectF getViewBound() {
        return new RectF(getTranslationX(),
                getTranslationY(),
                getTranslationX() + getWidth(),
                getTranslationY() + getHeight());
    }

    public void setContent(String content) {
        etContent.setText(content);
    }

    public void requestContentFocus() {
        requestFocus();
        etContent.setSelection(etContent.getText().toString().length());
    }

    public String getContent(){
        return etContent.getText().toString().trim();
    }

    public void setOnContentChangedListener(OnContentChangedListener onContentChangedListener) {
        this.onContentChangedListener = onContentChangedListener;
    }

    interface OnContentChangedListener{
        void onContentChanged();
    }
}
