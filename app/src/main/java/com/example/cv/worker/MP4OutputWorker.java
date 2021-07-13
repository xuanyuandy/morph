package com.example.cv.worker;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.hzy.face.morpher.MorpherApi;
import com.example.cv.consts.VideoConst;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MP4OutputWorker implements Runnable {
    private static final String TAG = MP4OutputWorker.class.getSimpleName();

    private static final int FRAME_RATE = 15;
    private static final int COMPRESS_RATIO = 64;
    private static final int I_FRAME_INTERVAL = 5;
    private static final int CHECK_OUTPUT_DELAY = 100;
    private final int mVideoWidth;
    private final int mVideoHeight;

    private String mOutputVideoPath;
    private MediaMuxer mMediaMuxer;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private int mVideoTrack;
    private MediaCodec.BufferInfo mBufferInfo;
    private long mEncoderTimeUs;
    private byte[] mInputYUVData;
    private long mDequeueTimeoutUS = 10_000L;
    private MediaCodecInfo mMediaCodecInfo;
    private String mMediaMimeType;
    private int mColorFormat;
    private volatile boolean mMuxerRunning;
    private EncoderCallback mEncoderCallback;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public MP4OutputWorker(String filePath, int width, int height) {
        mOutputVideoPath = filePath;
        mVideoWidth = width;
        mVideoHeight = height;
        mBufferInfo = new MediaCodec.BufferInfo();
        mInputYUVData = new byte[width * height * 3 / 2];
        setupMediaCodec();
        try {
            mMediaMuxer = new MediaMuxer(mOutputVideoPath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCallback(EncoderCallback callback) {
        mEncoderCallback = callback;
    }

    public void start() {
        try {
            mEncoderTimeUs = 0;
            mMediaCodec.start();
            mMuxerRunning = true;
            new Thread(this).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void release() {
        try {
            mMediaCodec.flush();
            mMuxerRunning = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void run() {
        if (mEncoderCallback != null) {
            mEncoderCallback.onStart();
        }
        while (mMuxerRunning) {
            try {
                Thread.sleep(CHECK_OUTPUT_DELAY);
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, mDequeueTimeoutUS);
                do {
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.i(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                        MediaFormat newFormat = mMediaCodec.getOutputFormat();
                        mVideoTrack = mMediaMuxer.addTrack(newFormat);
                        mMediaMuxer.start();
                    } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.i(TAG, "INFO_TRY_AGAIN_LATER");
                    } else if (outputBufferIndex >= 0) {
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            Log.i(TAG, "BUFFER_FLAG_CODEC_CONFIG");
                        } else {
                            ByteBuffer outputBuffer;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                            } else {
                                outputBuffer = mMediaCodec.getOutputBuffers()[outputBufferIndex];
                            }
                            if (outputBuffer != null) {
                                Log.i(TAG, "DATA Frame: " + mBufferInfo.size);
                                outputBuffer.position(mBufferInfo.offset);
                                outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                                mMediaMuxer.writeSampleData(mVideoTrack, outputBuffer, mBufferInfo);
                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            }
                        }
                    }
                    if (mMuxerRunning) {
                        outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, mDequeueTimeoutUS);
                    } else {
                        break;
                    }
                } while (outputBufferIndex > 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            mMediaCodec.stop();
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mEncoderCallback != null) {
            mEncoderCallback.onFinish();
        }
    }


    public void queenFrame(Bitmap bitmap, long delayUs) {
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(mDequeueTimeoutUS);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            } else {
                inputBuffer = mMediaCodec.getInputBuffers()[inputBufferIndex];
            }
            if (inputBuffer != null) {
                inputBuffer.clear();
                MorpherApi.bitmap2YUV(bitmap, mInputYUVData, mColorFormat);
                inputBuffer.put(mInputYUVData);
                mMediaCodec.queueInputBuffer(inputBufferIndex,
                        0, mInputYUVData.length, mEncoderTimeUs, 0);
                mEncoderTimeUs += delayUs;
            }
        }
    }

    private void setupMediaCodec() {
        try {
            selectMediaCodecInfo();
            if (mMediaCodecInfo != null) {
                mMediaFormat = MediaFormat.createVideoFormat(mMediaMimeType, mVideoWidth, mVideoHeight);
                int bitRate = mVideoWidth * mVideoHeight * 64 * FRAME_RATE / COMPRESS_RATIO;
                mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                mMediaCodec = MediaCodec.createByCodecName(mMediaCodecInfo.getName());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mMediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
                    mMediaFormat.setInteger(MediaFormat.KEY_COMPLEXITY,
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
                }
                mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
                mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
                LogUtils.i("Input COLOR_FORMAT: [" + mColorFormat + ']');
                mMediaCodec.configure(mMediaFormat, null, null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getAvailableColorFormat(MediaCodecInfo codecInfo, String mime) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mime);
        for (int format : capabilities.colorFormats) {
            for (int af : VideoConst.COLOR_FORMAT_ORDER) {
                if (af == format) {
                    return format;
                }
            }
        }
        return 0;
    }

    /**
     * Choose proper MIME type, Color format and MediaCodecInfo
     */
    private void selectMediaCodecInfo() {
        int codecCount = MediaCodecList.getCodecCount();
        int mimeIndex = 0, colorFormat = 0;
        MediaCodecInfo selectCodec = null;
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                for (String mime : codecInfo.getSupportedTypes()) {
                    for (int j = 1; j < VideoConst.MIMETYPE_PRIORITY.length; j++) {
                        // I want the biggest priority mime
                        if (mime.equalsIgnoreCase(VideoConst.MIMETYPE_PRIORITY[j]) && j > mimeIndex) {
                            int cf = getAvailableColorFormat(codecInfo, mime);
                            // color format is available
                            if (cf > 0) {
                                colorFormat = cf;
                                mimeIndex = j;
                                selectCodec = codecInfo;
                            }
                        }
                    }
                }
            }
        }
        if (mimeIndex != 0) {
            mColorFormat = colorFormat;
            mMediaCodecInfo = selectCodec;
            mMediaMimeType = VideoConst.MIMETYPE_PRIORITY[mimeIndex];
            LogUtils.d("MediaCodec Found: " + mMediaCodecInfo.getName());
            LogUtils.d("MediaCodec MimeType: " + mMediaMimeType);
        } else {
            LogUtils.e("No Good MediaCodec Found!!");
        }
    }
}
