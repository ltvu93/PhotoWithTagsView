package com.ltvu93.photowithtagsview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Switch;

import com.ltvu93.photowithtagsview.widget.PhotoWithTagsView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }

        FrameLayout flTopContainer = findViewById(R.id.activity_main_fl_top_container);
        Switch sbEdit = findViewById(R.id.activity_main_sb_edit);
        Switch sbShow = findViewById(R.id.activity_main_sb_show);
        PhotoWithTagsView photoWithTagsView = findViewById(R.id.activity_main_photo_with_tags_view);

        sbEdit.setOnCheckedChangeListener((view, isChecked) -> {
            if (isChecked) {
                sbShow.setChecked(true);
                sbShow.setEnabled(false);
            } else {
                sbShow.setEnabled(true);
            }
            photoWithTagsView.setEditable(isChecked);
        });
        sbShow.setOnCheckedChangeListener((view, isChecked) ->
                photoWithTagsView.setTagsVisible(isChecked));
        sbEdit.setChecked(false);
        sbShow.setChecked(true);

        photoWithTagsView.setOnClickedListener((x, y) -> {
            if (sbEdit.isChecked())
                return;

            if (flTopContainer.getVisibility() == View.VISIBLE) {
                flTopContainer.setVisibility(View.GONE);
            } else {
                flTopContainer.setVisibility(View.VISIBLE);
            }
        });
        photoWithTagsView.setEditable(false);
        photoWithTagsView.setTagsVisible(true);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        photoWithTagsView.setImageBitmap(bitmap);
        photoWithTagsView.addTagView("tag1", 200, 0);
        photoWithTagsView.addTagView("tag2", bitmap.getWidth() / 2, bitmap.getHeight() / 2);
    }
}
