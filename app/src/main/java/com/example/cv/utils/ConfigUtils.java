package com.example.cv.utils;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;

import com.blankj.utilcode.util.Utils;
import com.example.cv.R;

// user SharedPreferences to store the data
public class ConfigUtils {

    private static SharedPreferences mPref;

    private static SharedPreferences getPrefrence() {
        if (mPref == null) {
            mPref = PreferenceManager.getDefaultSharedPreferences(Utils.getApp());
        }
        return mPref;
    }

    /**
     * get the image resolution config
     * 图像分辨率配置
     * @return a point with x and y means width and height
     */
    public static Point getConfigResolution() {
        try {
            SharedPreferences pref = getPrefrence();
            String key = Utils.getApp().getString(R.string.pref_resolution_key);
            String[] resolutions = Utils.getApp().getResources().getStringArray(R.array.pref_resolution_array);
            int index = Integer.parseInt(pref.getString(key, "0"));
            String[] result = resolutions[index].split("x");
            return new Point(Integer.parseInt(result[0]), Integer.parseInt(result[1]));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * get the transition frame count
     * 过渡帧数
     * @return int value
     */
    public static int getConfigFrameCount() {
        try {
            SharedPreferences pref = getPrefrence();
            String key = Utils.getApp().getString(R.string.pref_frames_key);
            String[] resolutions = Utils.getApp().getResources().getStringArray(R.array.pref_frames_array);
            int index = Integer.parseInt(pref.getString(key, "0"));
            return Integer.parseInt(resolutions[index]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * gif duration for one transformation
     * 一次转换的 gif 持续时间
     * @return duration
     */
    public static int getConfigTransDuration() {
        try {
            SharedPreferences pref = getPrefrence();
            String key = Utils.getApp().getString(R.string.pref_trans_delay_key);
            return Integer.parseInt(pref.getString(key, "800"));
        } catch (Exception e) {
            return 800;
        }
    }

    public static int getConfigGifQuantizer() {
        try {
            SharedPreferences pref = getPrefrence();
            String key = Utils.getApp().getString(R.string.pref_quantizer_key);
            return Integer.parseInt(pref.getString(key, "2"));
        } catch (Exception e) {
            return 2;
        }
    }

    public static int getConfigGifDitherer() {
        try {
            SharedPreferences pref = getPrefrence();
            String key = Utils.getApp().getString(R.string.pref_ditherer_key);
            return Integer.parseInt(pref.getString(key, "0"));
        } catch (Exception e) {
            return 0;
        }
    }
}
