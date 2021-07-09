package com.example.cv.utils;

import android.util.Log;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.Utils;
// import something of other project
import com.hzy.face.morpher.Seeta2Api;
import com.example.cv.consts.AppConst;

import java.io.File;
import java.io.InputStream;

public class ModelFileUtils {
    // search file folder
    public static String getCascadePath() {
        return new File(Utils.getApp().getFilesDir(), AppConst.CASCADE_DIR).getPath();
    }

    public static String getCascadeFacePath() {
        String dirPath = getCascadePath();
        return new File(dirPath, AppConst.CASCADE_FACE_FILE).getPath();
    }

    /**
     * Prepare cascade files and return the cascade path
     *
     * @return the cascade path for face recogn
     */
    public static String ensureCascadePath() {
        try {
            String[] fileNames = Utils.getApp().getAssets().list(AppConst.CASCADE_DIR);
            File cascadeDir = new File(Utils.getApp().getFilesDir(), AppConst.CASCADE_DIR);
            Log.v("ensureCascadePath",cascadeDir.getName());
            if (!cascadeDir.exists()) {
                cascadeDir.mkdirs();
            }
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    File outFile = new File(cascadeDir, fileName);
                    if (!outFile.exists()) {
                        InputStream is = Utils.getApp().getAssets()
                                .open(AppConst.CASCADE_DIR + File.separator + fileName);
                        FileIOUtils.writeFileFromIS(outFile, is);
                    }
                }
            }
            return cascadeDir.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // make sure that the seet library source exist
    public static void initSeetaApi() {
        try {
            if (!Seeta2Api.INSTANCE.isInited()) {
                String seetaPath = ensureSeetaPath();
                String fdPath = new File(seetaPath, AppConst.SEETA_FACE_DATA).getPath();
                String pdPath = new File(seetaPath, AppConst.SEETA_POINTS_DATA).getPath();
                Seeta2Api.INSTANCE.init(fdPath, pdPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String ensureSeetaPath() {
        try {
            String[] fileNames = Utils.getApp().getAssets().list(AppConst.SEETA_DIR);
            File cascadeDir = new File(Utils.getApp().getFilesDir(), AppConst.SEETA_DIR);
            if (!cascadeDir.exists()) {
                cascadeDir.mkdirs();
            }
            if (fileNames != null) {
                for (String fileName : fileNames) {
                    File outFile = new File(cascadeDir, fileName);
                    if (!outFile.exists()) {
                        InputStream is = Utils.getApp().getAssets()
                                .open(AppConst.SEETA_DIR + File.separator + fileName);
                        FileIOUtils.writeFileFromIS(outFile, is);
                    }
                }
            }
            return cascadeDir.getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
