package com.ltvu93.photowithtagsview;

import android.graphics.Matrix;
import android.graphics.PointF;

public class AppUtil {

    public static PointF mapPoint(Matrix matrix, PointF point){
        float[] src = {point.x, point.y};
        float[] dst = new float[2];
        matrix.mapPoints(dst, src);

        return new PointF(dst[0], dst[1]);
    }
}
