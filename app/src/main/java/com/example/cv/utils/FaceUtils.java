package com.example.cv.utils;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;

import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.load.engine.Resource;
import com.example.cv.R;
import com.hzy.face.morpher.MorpherApi;
import com.hzy.face.morpher.Seeta2Api;
import com.example.cv.bean.FaceImage;

public class FaceUtils {

    @SuppressLint("DefaultLocale")
    public static Bitmap getBmpWithSize(String path, int width, int height) {
        Bitmap bitmap = ImageUtils.getBitmap(path);
        if (bitmap != null) {
            int oriWidth = bitmap.getWidth();
            int oriHeight = bitmap.getHeight();
            if (oriWidth == width && oriHeight == height) {
                // this bmp is just the right size!!
                return bitmap;
            } else {
                // this bmp need to be scaled!!
                float scaleX = ((float) width) / oriWidth;
                float scaleY = ((float) height) / oriHeight;
                Matrix matrix = new Matrix();
                matrix.postScale(scaleX, scaleY);
                LogUtils.d(String.format("BMP Scale[%dx%d]->[%dx%d]", oriWidth, oriHeight, width, height));
                Bitmap bitmapNew = Bitmap.createBitmap(bitmap, 0, 0, oriWidth, oriHeight, matrix, true);
                // because of the large bitmap,so we should recycle
                bitmap.recycle();
                return bitmapNew;
            }
        }
        return null;
    }

    //
    public static FaceImage getFaceFromPath(String path, int width, int height) {
        try {
            Bitmap bitmap = getBmpWithSize(path, width, height);
            // the INSTANCE is just like the enum's example
            PointF[] points = Seeta2Api.INSTANCE.detectLandmarks(bitmap, true);
            if (points.length > 0) {
                int[] indices = MorpherApi.getSubDivPointIndex(bitmap.getWidth(),
                        bitmap.getHeight(), points);
                if (indices != null && indices.length > 0) {
                    return new FaceImage(path, points, indices);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
