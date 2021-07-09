package com.example.cv.utils;

import java.io.File;

public interface SaveMediaCallback {
    void onSuccess(File dst);

    void onFail();
}
