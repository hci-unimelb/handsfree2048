package com.example.uitest;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class EyeActivity extends AppCompatActivity {

    private String TAG = "dlib";
    private long mframestart, mframeend, mframetime;
    private long framecount;
    private Camera mCamera;

    private boolean saved;
    private int saveidx;

    private ImageView mImageView, mEyeView;
    private TextView mTextView;
    private SurfaceTexture surfacetexture;
    private TextView instruction;
    private boolean flag = true;

    private int viewWidth, viewHeight;  //mSurfaceView的宽和高
    private int eyeWidth, eyeHeigth;
    private int cameraWidth, cameraHeight;
    Timer timer;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye);

        // Example of a call to a native method
//        TextView tv = findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
        surfacetexture=new SurfaceTexture(10);
        mImageView = (ImageView)findViewById(R.id.camera_view);
        //mEyeView = (ImageView)findViewById(R.id.eye_view);
        //mTextView = (TextView)findViewById(R.id.textView);
        instruction = (TextView)findViewById(R.id.instruction_view);
        instruction.setText("First we need to perform eye calibration");

        String filename = "haarcascade_frontalface_alt2.xml";
        File face_model = new File(this.getCacheDir() + "/" + filename);
        moveModelData(face_model, filename);
        String tmp = face_model.getAbsolutePath();
        loadFaceModel(tmp);

        filename = "shape_predictor_68.dat";
        File med_model = new File(this.getCacheDir() + "/" + filename);
        moveModelData(med_model, filename);
        tmp = med_model.getAbsolutePath();

        long startTime = System.currentTimeMillis(); //起始时间
        loadMedModel(tmp);
        long endTime = System.currentTimeMillis(); //结束时间
        long runTime = endTime - startTime;

        //新建文件夹
        File leftEye = new File("//data/data/com.example.uitest/leftEye");
        if (!leftEye.exists()) {
            try {
                //按照指定的路径创建文件夹
                leftEye.mkdirs();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        File rightEye = new File("//data/data/com.example.uitest/rightEye");
        if (!rightEye.exists()) {
            try {
                //按照指定的路径创建文件夹
                rightEye.mkdirs();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        Log.i(TAG, "加载dlib模型:" + runTime + "ms");
        Log.i(TAG, "模型载入完成");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null){
            int scale = 10;  // 缩小倍率
            saved = true;   // 是否保存
            saveidx = 0;
            cameraWidth = 1920;
            cameraHeight = 1080;
            viewWidth = cameraWidth / scale;
            viewHeight = cameraHeight / scale;
            eyeWidth = 40;  // 这个是最终显示的时候单个眼睛的高
            eyeHeigth = 80; // 这个是最终显示的时候单个眼睛的宽
            initCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //flag = false;
        timer.cancel();
        mCamera.stopPreview(); //后添的
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            saved = true;
        }
    };

    private void initCamera(){
        mCamera = Camera.open(1);   // 0是后置 1是前置
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        for(int i=0;i<previewSizes.size();i++){
            Log.v("CAMERA_1","width:" + String.valueOf(previewSizes.get(i).width)+"\theight:" +String.valueOf(previewSizes.get(i).height));
        }
        if (mCamera != null) {
            try {
                framecount = 0;

                Camera.Parameters parameters = mCamera.getParameters();
                //设置预览照片的大小
                parameters.setPreviewSize(cameraWidth, cameraHeight);
                mCamera.setParameters(parameters);
                previewCallBack pre = new previewCallBack();//建立预览回调对象
                mCamera.setPreviewCallback(pre); //设置预览回调对象
                mCamera.setPreviewTexture(surfacetexture);
                mCamera.startPreview();
                timer = new Timer();
                timer.schedule(task,0,2000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class previewCallBack implements Camera.PreviewCallback {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            framecount = framecount + 1;
            mframestart = System.currentTimeMillis(); //起始时间
            Bitmap bmp = Bitmap.createBitmap(viewWidth,viewHeight, Bitmap.Config.ARGB_8888);
            Bitmap leftEyeBitmap = Bitmap.createBitmap(eyeWidth,eyeHeigth, Bitmap.Config.ARGB_8888);
            Bitmap rightEyeBitmap = Bitmap.createBitmap(eyeWidth,eyeHeigth, Bitmap.Config.ARGB_8888);
            Bitmap eyesshow = Bitmap.createBitmap(eyeWidth,eyeHeigth * 2, Bitmap.Config.ARGB_8888);

            int detected = decode(data, bmp, cameraHeight, cameraWidth, viewHeight, viewWidth,
                    eyeHeigth, eyeWidth, leftEyeBitmap, rightEyeBitmap, eyesshow);
            bmp = rotateBimap(EyeActivity.this, -90, bmp);
            //eyesshow = rotateBimap(MainActivity.this, -90, eyesshow);

            if(detected > 0 && saved && saveidx < 18){
                if(saveidx == 0 || saveidx == 1){
                    instruction.setTextSize(30);
                    instruction.setText("First we need to perform eye calibration");
                }else if(saveidx == 2 || saveidx == 3){
                    instruction.setTextSize(25);
                    instruction.setText("Please follow the instructions and make sure that the camera can detects your entire face");
                }else if(saveidx == 4) {
                    instruction.setText("Please look at the center of the screen");
                    instruction.setBackgroundColor(Color.YELLOW);
                }else if(saveidx == 5){
                    //look centre
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_centre.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_centre.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                }else if(saveidx == 6){
                    instruction.setText("Please look to the left");
                    instruction.setBackgroundColor(Color.GREEN);
                }else if(saveidx == 7){
                    //look left
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_left.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_left.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                }else if(saveidx == 8){
                    instruction.setText("Please look to the right");
                    instruction.setBackgroundColor(Color.GRAY);
                }else if(saveidx == 9){
                    //look right
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_right.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_right.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                }else if(saveidx == 10){
                    instruction.setText("Please look up");
                    instruction.setBackgroundColor(Color.YELLOW);
                }else if(saveidx == 11){
                    //look up
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_up.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_up.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                }else if(saveidx == 12){
                    instruction.setText("Please look down");
                    instruction.setBackgroundColor(Color.GRAY);
                }else if(saveidx == 13){
                    //look down
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_down.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_down.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                }else if(saveidx == 14) {
                    instruction.setText("Please close your eyes for one minute");
                    instruction.setBackgroundColor(Color.WHITE);
                }else if(saveidx == 15){
                    //eye close
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_close.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_close.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    //播放音效
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                }else if(saveidx == 16){
                    instruction.setText("Now you can play the game!");
                    instruction.setBackgroundColor(Color.WHITE);
                }else if(saveidx == 17){
                    timer.cancel();
                    saved = false;
                    Intent intent = new Intent(EyeActivity.this, RunActivity.class);
                    startActivity(intent);
                }
                saveidx++;
                saved = false;
            }

            if(saveidx >= 18){
                timer.cancel();
            }
            mImageView.setImageBitmap(bmp);
            //mEyeView.setImageBitmap(eyesshow);
            mframeend = System.currentTimeMillis(); //起始时间
            mframetime = mframeend - mframestart;
            String show = "average fps:" + 1000 / mframetime;
            //mTextView.setText(show);
        }
    }

    public Bitmap rotateBimap(Context context, float degree, Bitmap srcBitmap) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degree);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()
                , matrix, true);
        return bitmap;
    }

    public void savebitmap(String filename, Bitmap bmp){
        File file = new File(filename);
        if(file.exists() || file.isDirectory()){
            file.delete();
        }
        //file.createNewFile();
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void moveModelData(File file, String filename){
        if (!file.exists())
            try {
                InputStream is = this.getAssets().open(filename);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native int decode(byte[] yuv, Object bitmap, int ch, int cw, int vh, int vw,
                             int eh, int ew, Object leftEye, Object rightEye, Object Eye);
    public native void loadFaceModel(String file_path);
    public native void loadMedModel(String fileName);

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native String stringFromJNI();


}
