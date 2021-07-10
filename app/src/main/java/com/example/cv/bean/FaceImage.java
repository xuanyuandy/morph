package com.example.cv.bean;

import android.graphics.PointF;

public class FaceImage {
    public String path;
    public PointF[] points;
    public int[] indices;

    public FaceImage() {
        this.path = "sb";
        this.points = new PointF[]{new PointF(1,2)};
        this.indices = new int[]{1, 2};

    }

    public FaceImage(String path, PointF[] points, int[] indices) {
        this.path = path;
        this.points = points;
        this.indices = indices;
    }
}
