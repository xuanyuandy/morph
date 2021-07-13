package com.example.cv.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.bilibili.burstlinker.BurstLinker;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.Utils;
import com.bumptech.glide.Glide;
import com.example.cv.activity.GifResultActivity;
import com.example.cv.worker.EncoderCallback;
import com.example.cv.worker.MP4OutputWorker;
import com.example.cv.worker.MorphCallback;
import com.example.cv.worker.MultiMorphWorker;
import com.hzy.face.morpher.MorpherApi;
import com.example.cv.R;
import com.example.cv.bean.FaceImage;
import com.example.cv.consts.RequestCode;
import com.example.cv.consts.RouterHub;
import com.example.cv.utils.ActionUtils;
import com.example.cv.utils.ModelFileUtils;
import com.example.cv.utils.ConfigUtils;
import com.example.cv.utils.FaceUtils;
import com.example.cv.utils.SpaceUtils;
import com.example.cv.widget.Ratio34ImageView;
import com.hzy.face.morpher.Seeta2Api;
import com.yalantis.ucrop.UCrop;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

@Route(path = RouterHub.My_Final_ACTIVITY)
public class MyFinalActivity extends AppCompatActivity {


    @BindView(R.id.my_imageview_second)
    Ratio34ImageView mImageviewSecond;
    @BindView(R.id.my_alpha_text)
    TextView mAlphaText;
    @BindView(R.id.my_alpha_progress)
    ProgressBar mAlphaProgress;
    @BindView(R.id.spinner)
    Spinner spinner;

    String arr[];

    Ratio34ImageView mDialogImageView;
    private ProgressBar mDialogProgress;
    private Dialog mImageDialog;
    private MultiMorphWorker mMorphWorker;
    private boolean mMorphSave;
    private int mTransFrameCount;
    private MP4OutputWorker mVideoWorker;
    private int mFrameSpaceUs;
    private String mOutputPath;



    private BurstLinker mBurstLinker;
    private String mGifFilePath;
    private Point mImageSize;
    private int mFrameDelayMs;
    // ExecutorService
    private ExecutorService mFaceExecutor;
    private ProgressDialog mProgressDialog;
    private float mMorphAlpha = -1f;
    private volatile boolean mMorphRunning = false;
    private List<FaceImage> mFaceImages;
    FaceImage FirstImage;
    private List<ImageView> mImageViews;
    private Bitmap mOutputBitmap;
    private String mSelectPath;
    private int mCurrentIndex;
    private Bitmap bmp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initConfigurations();
//        EventBus.getDefault().register(this);
        SpaceUtils.clearUsableSpace();
        prepareMorphData();
        initPageDialogs();
        // this is the normal use of butterKnife
        setContentView(R.layout.activity_my_morph);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mImageViews = new LinkedList<>();
        mImageViews.add(null);
        mImageViews.add(mImageviewSecond);
        mOutputBitmap = Bitmap.createBitmap(mImageSize.x, mImageSize.y, Bitmap.Config.ARGB_8888);

        mFaceExecutor = Executors.newSingleThreadExecutor();
        SpaceUtils.clearUsableSpace();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.loading_wait_tips));
        mProgressDialog.setCancelable(false);
        mFaceExecutor.submit(ModelFileUtils::initSeetaApi);
        spininit();

    }


    private void spininit(){
        arr = getResources().getStringArray(R.array.myarray);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast.makeText(MyFinalActivity.this, "你选中了" + arr[i], Toast.LENGTH_SHORT).show();
                test(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }
    private void test(int id){
        if(id == 0){
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.beauty3);
        }else if(id == 1){
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pyy);
        }
//        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.beauty3);
        bmp = zoomImg(bmp,mImageSize.x,mImageSize.y);
        assert(bmp != null);
        File imgFile = SpaceUtils.newUsableFile();
        mSelectPath = imgFile.getPath();
        // 将当前image存到任意分配的路径下
        // R.drawable.beauty3 -> imgFile
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imgFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        try {
            fos.flush();
            fos.close();
            Toast.makeText(this, "success store", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }


        PointF[] points = Seeta2Api.INSTANCE.detectLandmarks(bmp, true);
        if (points.length > 0) {
            int[] indices = MorpherApi.getSubDivPointIndex(bmp.getWidth(),
                    bmp.getHeight(), points);
            if (indices != null && indices.length > 0) {
                Toast.makeText(this,"success",Toast.LENGTH_SHORT).show();
                FirstImage = new FaceImage(mSelectPath,points,indices);

            }
        }

        mFaceImages.set(0, FirstImage);
//        assert (mFaceImages.get(0) != null);


    }

    private void startVideoSaver() {
        mOutputPath = SpaceUtils.newUsableFile().getPath();
        mVideoWorker = new MP4OutputWorker(mOutputPath, mImageSize.x, mImageSize.y);
        mVideoWorker.setCallback(new EncoderCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onFinish() {

            }
        });
        mVideoWorker.start();
    }

    private void prepareMorphData() {
        mOutputBitmap = Bitmap.createBitmap(mImageSize.x, mImageSize.y, Bitmap.Config.ARGB_8888);
        mMorphWorker = new MultiMorphWorker();
        mMorphWorker.setOutBitmap(mOutputBitmap);
        mMorphWorker.setTransFrames(mTransFrameCount);
        mMorphWorker.setCallback(new MorphCallback() {
            @Override
            protected void onStart() {
                if (mMorphSave) {
                    startVideoSaver();
                }
            }

            @Override
            protected void onOneFrame(Bitmap bitmap, int index, float alpha) {
                if (mMorphSave) {
                    mVideoWorker.queenFrame(bitmap, mFrameSpaceUs);
                }
                int progress = (int) ((index + alpha) * 100) / mFaceImages.size();
                runOnUiThread(() -> {
                    mDialogImageView.setImageBitmap(bitmap);
                    mDialogProgress.setProgress(progress);
                });
            }

            @Override
            protected void onAbort() {
                if (mMorphSave) {
                    mVideoWorker.release();
                }
            }

            @Override
            protected void onFinish() {
                if (mMorphSave) {
                    mVideoWorker.release();
                }
            }
        });
    }

    private void initPageDialogs() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.loading_wait_tips));
        mProgressDialog.setCancelable(false);
        mImageDialog = new Dialog(this);
        mImageDialog.setCancelable(false);
        mImageDialog.setContentView(R.layout.dialog_image_preview);
        mDialogImageView = mImageDialog.findViewById(R.id.dialog_image_view);
        mDialogProgress = mImageDialog.findViewById(R.id.dialog_progress);
        mImageDialog.findViewById(R.id.dialog_btn_cancel)
                .setOnClickListener(view -> {
                    mMorphWorker.abort();
                    mImageDialog.dismiss();
                });
    }



    public Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }
    // get some arguments
    // get some 图像分辨率配置,过渡帧数,gif 持续时间


    private void initConfigurations() {
        mFaceImages = new LinkedList<>();
        mFaceImages.add(null);
        mFaceImages.add(null);
        mImageSize = ConfigUtils.getConfigResolution();
        int frames = ConfigUtils.getConfigFrameCount();
        int duration = ConfigUtils.getConfigTransDuration();
        mFrameDelayMs = duration / frames;

        mTransFrameCount = ConfigUtils.getConfigFrameCount() * 10;
        mFrameSpaceUs = duration * 1000 / mTransFrameCount;
    }

    @Override
    protected void onPause() {
        mMorphRunning = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMorphRunning = false;
        mFaceExecutor.shutdownNow();
        mOutputBitmap.recycle();
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick({
            R.id.my_imageview_second,
            R.id.my_btn_start_morph,
            R.id.my_btn_save_gif})
    public void onViewClicked(View view) {
        mMorphRunning = false;
        switch (view.getId()) {
            case R.id.my_imageview_second:
                mCurrentIndex = 1;
                ActionUtils.startImageContentAction(this, RequestCode.CHOOSE_IMAGE);
                break;
            case R.id.my_btn_start_morph:
//                startMorphProcess(false);
                morphFaceImages(false);
                break;
            case R.id.my_btn_save_gif:
//                test();
//                startMorphProcess(true);
                break;
        }
    }

    private void snakeBarShow(String msg) {
        SnackbarUtils.with(mImageviewSecond).setMessage(msg).show();
    }


    private void morphFaceImages(boolean isSave) {
        if (mFaceImages.size() >= 2) {
            Glide.with(this).load(mFaceImages.get(0).path).into(mDialogImageView);
            mDialogProgress.setProgress(0);
            mImageDialog.show();
            mMorphWorker.setFaceImages(mFaceImages);
            mMorphSave = isSave;
            mFaceExecutor.submit(mMorphWorker);
        } else {
            snakeBarShow(getString(R.string.select_2images_tips));
        }
    }


    // receive the information from the image intent
    // this function may be use twice
    // first choose_image then crop_image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // choose a image
        if (requestCode == RequestCode.CHOOSE_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri dataUri = data.getData();
                    if (dataUri != null) {
                        // get a random name
                        File imgFile = SpaceUtils.newUsableFile();
                        mSelectPath = imgFile.getPath();
                        Log.v("path",mSelectPath);
                        // the image intent just return a simple image
                        // the Ucrop(裁剪) is solved after the image intent
                        UCrop.Options options = new UCrop.Options();
                        options.setCompressionQuality(100);
                        UCrop.of(dataUri, Uri.fromFile(imgFile))
                                .withOptions(options)
                                .withMaxResultSize(mImageSize.x, mImageSize.y)
                                .withAspectRatio(3, 4)
                                .start(this, RequestCode.CROP_IMAGE);
                    }
                }
            }
        } else if (requestCode == RequestCode.CROP_IMAGE) {
            // crop a image
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Glide.with(this).load(mSelectPath).into(mImageViews.get(mCurrentIndex));
                    startDetectFaceInfo();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // this first detect the face picture after croping the picture
    private void startDetectFaceInfo() {
        mProgressDialog.show();
        mFaceExecutor.submit(() -> {
            // first begin the face morphing
            FaceImage faceImage = FaceUtils.getFaceFromPath(
                    mSelectPath, mImageSize.x, mImageSize.y);
            // each time detect the image, we should send it to the global value
            mFaceImages.set(mCurrentIndex, faceImage);
            // modify the progressDialog
            runOnUiThread(() -> {
                mProgressDialog.dismiss();
                if (faceImage == null) {
                    mImageViews.get(mCurrentIndex).setImageResource(R.drawable.ic_head);
                    // snakeBarShow can replace Toast
                    snakeBarShow(getString(R.string.no_face_detected));
                }
            });
        });
    }




    private void routerShareGifImage() {
        if (!StringUtils.isTrimEmpty(mGifFilePath)) {
            ARouter.getInstance().build(RouterHub.GIF_RESULT_ACTIVITY)
                    .withString(GifResultActivity.EXTRA_FILE_PATH, mGifFilePath)
                    .navigation(this);
        }
    }
}
