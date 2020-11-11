package com.example.uitest;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
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

    private ImageView mImageView;
    private TextView mTextView;
    private SurfaceTexture surfacetexture;
    private TextView instruction;

    private int viewWidth, viewHeight;  //width and height of mSurfaceView
    private int eyeWidth, eyeHeigth;
    private int cameraWidth, cameraHeight;
    Timer timer;

    private SoundPool mSoundPool = null;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye);

        surfacetexture=new SurfaceTexture(10);
        mImageView = (ImageView)findViewById(R.id.camera_view);
        //mTextView = (TextView)findViewById(R.id.textView);
        instruction = (TextView)findViewById(R.id.instruction_view);
        instruction.setText("Please make sure that the camera can detect your entire face");
        instruction.setTextSize(25);

        String filename = "haarcascade_frontalface_alt2.xml";
        File face_model = new File(this.getCacheDir() + "/" + filename);
        moveModelData(face_model, filename);
        String tmp = face_model.getAbsolutePath();
        loadFaceModel(tmp);

        filename = "shape_predictor_68.dat";
        File med_model = new File(this.getCacheDir() + "/" + filename);
        moveModelData(med_model, filename);
        tmp = med_model.getAbsolutePath();

        long startTime = System.currentTimeMillis();
        loadMedModel(tmp);
        long endTime = System.currentTimeMillis();
        long runTime = endTime - startTime;


        File leftEye = new File("//data/data/com.example.uitest/leftEye");
        if (!leftEye.exists()) {
            try {
                leftEye.mkdirs();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        File rightEye = new File("//data/data/com.example.uitest/rightEye");
        if (!rightEye.exists()) {
            try {
                rightEye.mkdirs();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        Log.i(TAG, "load dlib model:" + runTime + "ms");
        Log.i(TAG, "load successfully");

        mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        mSoundPool.load(this,R.raw.camera,1);
    }

    private void playSound(){
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float streamVolumeMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;
        mSoundPool.play(1,volume,volume,1,0,1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null){
            int scale = 10;  // Reduce the frame to increase processing speed
            saved = true;
            saveidx = 0;
            cameraWidth = 1920;
            cameraHeight = 1080;
            viewWidth = cameraWidth / scale;
            viewHeight = cameraHeight / scale;
            eyeWidth = 40;  // the height of each eye photo
            eyeHeigth = 80; // the width of each eye photo
            initCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
        mCamera.stopPreview();
    }

    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            saved = true;
        }
    };

    private void initCamera(){
        mCamera = Camera.open(1);   // 0 back, 1 front camera
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        for(int i=0;i<previewSizes.size();i++){
            Log.v("CAMERA_1","width:" + previewSizes.get(i).width +"\theight:" + previewSizes.get(i).height);
        }
        if (mCamera != null) {
            try {
                framecount = 0;

                //Set the size of the preview photo
                parameters.setPreviewSize(cameraWidth, cameraHeight);
                mCamera.setParameters(parameters);
                previewCallBack pre = new previewCallBack();
                mCamera.setPreviewCallback(pre);
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
            mframestart = System.currentTimeMillis();
            Bitmap bmp = Bitmap.createBitmap(viewWidth,viewHeight, Bitmap.Config.ARGB_8888);
            Bitmap leftEyeBitmap = Bitmap.createBitmap(eyeWidth,eyeHeigth, Bitmap.Config.ARGB_8888);
            Bitmap rightEyeBitmap = Bitmap.createBitmap(eyeWidth,eyeHeigth, Bitmap.Config.ARGB_8888);
            Bitmap eyesshow = Bitmap.createBitmap(eyeWidth,eyeHeigth * 2, Bitmap.Config.ARGB_8888);

            int detected = decode(data, bmp, cameraHeight, cameraWidth, viewHeight, viewWidth,
                    eyeHeigth, eyeWidth, leftEyeBitmap, rightEyeBitmap, eyesshow);
            bmp = rotateBimap(EyeActivity.this, -90, bmp);
            //eyesshow = rotateBimap(EyeActivity.this, -90, eyesshow);

            if(detected > 0 && saved && saveidx < 20){
                if(saveidx == 0 || saveidx == 1){
                    instruction.setTextSize(30);
                    instruction.setText("First we need to perform an eye calibration");
                }else if(saveidx == 2 || saveidx == 3) {
                    instruction.setTextSize(25);
                    instruction.setText("Please follow the instructions and make sure the camera can detect your entire face");
                }else if(saveidx == 4 || saveidx == 5){
                    instruction.setTextSize(25);
                    instruction.setText("Please keep the eye gesture until you hear the shutter sound");
                    if(saveidx == 5) playSound();
                }else if(saveidx == 6) {
                    instruction.setText("Now please look at the center of the screen");
                    instruction.setBackgroundColor(0xff3CB371);
                }else if(saveidx == 7){
                    //look centre
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_centre.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_centre.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                    playSound();
                }else if(saveidx == 8){
                    instruction.setText("Please look to the left");
                    instruction.setBackgroundColor(0xffFFA500);
                }else if(saveidx == 9){
                    //look left
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_left.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_left.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                    playSound();
                }else if(saveidx == 10){
                    instruction.setText("Please look to the right");
                    instruction.setBackgroundColor(0xff3CB371);
                }else if(saveidx == 11){
                    //look right
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_right.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_right.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                    playSound();
                }else if(saveidx == 12){
                    instruction.setText("Please look up");
                    instruction.setBackgroundColor(0xffFFA500);
                }else if(saveidx == 13){
                    //look up
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_up.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_up.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                    playSound();
                }else if(saveidx == 14){
                    instruction.setText("Please look down");
                    instruction.setBackgroundColor(0xff3CB371);
                }else if(saveidx == 15){
                    //look down
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_down.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_down.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                    playSound();
                }else if(saveidx == 16) {
                    instruction.setText("Please close your eyes until you hear the shutter sound");
                    instruction.setBackgroundColor(0xffFFA500);
                }else if(saveidx == 17){
                    //eye close
                    leftEyeBitmap = rotateBimap(EyeActivity.this, -90, leftEyeBitmap);
                    String fileName = "//data/data/com.example.uitest/leftEye/L_close.jpg";
                    savebitmap(fileName, leftEyeBitmap);

                    rightEyeBitmap = rotateBimap(EyeActivity.this, -90, rightEyeBitmap);
                    fileName = "//data/data/com.example.uitest/rightEye/R_close.jpg";
                    savebitmap(fileName, rightEyeBitmap);
                    Toast.makeText(EyeActivity.this, "picture saved", Toast.LENGTH_SHORT).show();
                    playSound();
                }else if(saveidx == 18){
                    instruction.setText("Now you can play the game!");
                    instruction.setBackgroundColor(Color.WHITE);
                }else if(saveidx == 19){
                    timer.cancel();
                    saved = false;
                    Intent intent = new Intent(EyeActivity.this, ShowTemplates.class);
                    startActivity(intent);
                }
                saveidx++;
                saved = false;
            }

            if(saveidx >= 20){
                timer.cancel();
            }
            mImageView.setImageBitmap(bmp);
            mframeend = System.currentTimeMillis();
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
    public native int decode(byte[] yuv, Object bitmap, int ch, int cw, int vh, int vw,
                             int eh, int ew, Object leftEye, Object rightEye, Object Eye);
    public native void loadFaceModel(String file_path);
    public native void loadMedModel(String fileName);


}
