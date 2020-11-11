package com.example.uitest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Description This activity moves the numbered tiles according to the result of eye gesture recognition
 * @reference Part of the 2048 game code comes from https://www.youtube.com/watch?v=6ojV--thA5c, which will be specified in the code.
 */

public class RunActivity extends AppCompatActivity {

    private String TAG = "RunActivity";

    //test
    private float mse_centre, mse_close, mse_down, mse_up, mse_left, mse_right;

    //Templates
    private ImageView centreView, closeView, leftView, rightView, upView, downView;

    private Bitmap bitmapL_Centre, bitmapL_Close;
    private Bitmap bitmapL_Left, bitmapL_Right;
    private Bitmap bitmapL_Up, bitmapL_Down;

    private Bitmap bitmapR_Centre, bitmapR_Close;
    private Bitmap bitmapR_Left, bitmapR_Right;
    private Bitmap bitmapR_Up, bitmapR_Down;

    private FileInputStream inputStream;
    private String prefix =  "//data/data/com.example.uitest/";
    private TextView infoView;
    private TextView statusView;
    private TextView promptView;

    //pixel array of templates
    private int[] temL_Cen = new int[3200];
    private int[] temL_Close = new int[3200];
    private int[] temL_Left = new int[3200];
    private int[] temL_Right = new int[3200];
    private int[] temL_Up = new int[3200];
    private int[] temL_Down = new int[3200];

    private int[] temR_Cen = new int[3200];
    private int[] temR_Close = new int[3200];
    private int[] temR_Left = new int[3200];
    private int[] temR_Right = new int[3200];
    private int[] temR_Up = new int[3200];
    private int[] temR_Down = new int[3200];


    private long mframestart, mframeend, mframetime;
    private long framecount;
    private Camera mCamera;

    private ImageView mImageView;
    private SurfaceTexture surfacetexture;

    private int viewWidth, viewHeight;
    private int eyeWidth, eyeHeigth;
    private int cameraWidth, cameraHeight;

    //play sound
    private SoundPool mSoundPool = null;
    private HashMap<Integer, Integer> soundID = new HashMap<>();

    //game variables
    private ImageView box11, box12, box13, box14;
    private ImageView box21, box22, box23, box24;
    private ImageView box31, box32, box33, box34;
    private ImageView box41, box42, box43, box44;
    Intent context;
    private TextView text11, text12, text13, text14;
    private TextView text21, text22, text23, text24;
    private TextView text31, text32, text33, text34;
    private TextView text41, text42, text43, text44;
    private LinearLayout layout;
    private int matrix[][], saveMatrix[][];
    private EyeDatabase database;

    private int random_i;
    private int random_j;

    private int left_n, right_n, up_n, down_n, centre_n, back_n, retreat_n = 0;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("run");
    }

    public RunActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);

        surfacetexture = new SurfaceTexture(10);
        mImageView = (ImageView)findViewById(R.id.camera_view2);

        statusView = (TextView)findViewById(R.id.status_view);
        promptView = (TextView)findViewById(R.id.prompt);
        promptView.setText("Tips: return to the homepage by winking your left eye" + "\n"
                           + "retreat one step by winking your right eye");

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
//        Log.i(TAG, "load dlib model:" + runTime + "ms");
//        Log.i(TAG, "Model loading completed");

        //load the templates
        try {
            loadTemplates();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        initView();
        newGame();
        //keepPlay();

        initSoundPool();
    }

    private void initSoundPool(){
        mSoundPool = new SoundPool(6, AudioManager.STREAM_SYSTEM,5);
        soundID.put(1,mSoundPool.load(this, R.raw.left,1));
        soundID.put(2,mSoundPool.load(this, R.raw.right,1));
        soundID.put(3,mSoundPool.load(this, R.raw.up,1));
        soundID.put(4,mSoundPool.load(this, R.raw.down,1));
        soundID.put(5,mSoundPool.load(this, R.raw.retreat,1));
        soundID.put(6,mSoundPool.load(this, R.raw.retreatonce, 1));
    }

    private void playSound(int soundId){
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC);  //adjust the sound level
        float streamVolumeMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;
        mSoundPool.play(soundID.get(soundId),volume,volume,1,0,1);
    }

    private void loadTemplates() throws FileNotFoundException {

        bitmapL_Centre = load("leftEye/L_centre.jpg");
        bitmapL_Left = load("leftEye/L_left.jpg");
        bitmapL_Right = load("leftEye/L_right.jpg");
        bitmapL_Up = load("leftEye/L_up.jpg");
        bitmapL_Down = load("leftEye/L_down.jpg");
        bitmapL_Close = load("leftEye/L_close.jpg");

        bitmapR_Centre = load("rightEye/R_centre.jpg");
        bitmapR_Left = load("rightEye/R_left.jpg");
        bitmapR_Right = load("rightEye/R_right.jpg");
        bitmapR_Up = load("rightEye/R_up.jpg");
        bitmapR_Down = load("rightEye/R_down.jpg");
        bitmapR_Close = load("rightEye/R_close.jpg");

//        centreView.setImageBitmap(bitmapCentre);
//        closeView.setImageBitmap(bitmapClose);
//        leftView.setImageBitmap(bitmapLeft);
//        rightView.setImageBitmap(bitmapRight);
//        upView.setImageBitmap(bitmapUp);
//        downView.setImageBitmap(bitmapDown);

        //normalize the templates
        normalize(bitmapL_Centre, temL_Cen);
        normalize(bitmapL_Close, temL_Close);
        normalize(bitmapL_Left, temL_Left);
        normalize(bitmapL_Right, temL_Right);
        normalize(bitmapL_Up, temL_Up);
        normalize(bitmapL_Down, temL_Down);

        normalize(bitmapR_Centre, temR_Cen);
        normalize(bitmapR_Close, temR_Close);
        normalize(bitmapR_Left, temR_Left);
        normalize(bitmapR_Right, temR_Right);
        normalize(bitmapR_Up, temR_Up);
        normalize(bitmapR_Down, temR_Down);

    }

    private Bitmap load(String fileName) throws FileNotFoundException {
        String str = prefix + fileName;
        inputStream = new FileInputStream(str);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null){
            int scale = 10;  //Reduce the frame to increase processing speed
            cameraWidth = 1920;
            cameraHeight = 1080;
            viewWidth = cameraWidth / scale;
            viewHeight = cameraHeight / scale;
            eyeWidth = 40;
            eyeHeigth = 80;
            initCamera();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveProcess();
        mSoundPool.release();
        mSoundPool = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSoundPool.release();
        mSoundPool = null;
    }

    /*
    the start of the copied code from the url
     */
    public void saveProcess() {
        database = new EyeDatabase(this);
        database.setProcess(matrix);
        database.close();
    }

    public void keepPlay() {
        database = new EyeDatabase(this);
        matrix = database.getProcess(this);
        database.close();
        setSaveMatrix();
        setBox();
    }

    public void newGame() {
        database = new EyeDatabase(this);
        database.close();
        for (int i = 0; i < 25; i++) matrix[i / 5][i % 5] = 0;
        randomNumber();   // generate 2 numbers at first
        randomNumber();
        setSaveMatrix();
        setBox();
    }

    public void initView() {
        matrix = new int[5][5];
        saveMatrix = new int[5][5];

        layout = (LinearLayout) findViewById(R.id.board);

        box11 = (ImageView) findViewById(R.id.box11);
        box12 = (ImageView) findViewById(R.id.box12);
        box13 = (ImageView) findViewById(R.id.box13);
        box14 = (ImageView) findViewById(R.id.box14);

        box21 = (ImageView) findViewById(R.id.box21);
        box22 = (ImageView) findViewById(R.id.box22);
        box23 = (ImageView) findViewById(R.id.box23);
        box24 = (ImageView) findViewById(R.id.box24);

        box31 = (ImageView) findViewById(R.id.box31);
        box32 = (ImageView) findViewById(R.id.box32);
        box33 = (ImageView) findViewById(R.id.box33);
        box34 = (ImageView) findViewById(R.id.box34);

        box41 = (ImageView) findViewById(R.id.box41);
        box42 = (ImageView) findViewById(R.id.box42);
        box43 = (ImageView) findViewById(R.id.box43);
        box44 = (ImageView) findViewById(R.id.box44);

        //number
        text11 = (TextView) findViewById(R.id.text11);
        text12 = (TextView) findViewById(R.id.text12);
        text13 = (TextView) findViewById(R.id.text13);
        text14 = (TextView) findViewById(R.id.text14);

        text21 = (TextView) findViewById(R.id.text21);
        text22 = (TextView) findViewById(R.id.text22);
        text23 = (TextView) findViewById(R.id.text23);
        text24 = (TextView) findViewById(R.id.text24);

        text31 = (TextView) findViewById(R.id.text31);
        text32 = (TextView) findViewById(R.id.text32);
        text33 = (TextView) findViewById(R.id.text33);
        text34 = (TextView) findViewById(R.id.text34);

        text41 = (TextView) findViewById(R.id.text41);
        text42 = (TextView) findViewById(R.id.text42);
        text43 = (TextView) findViewById(R.id.text43);
        text44 = (TextView) findViewById(R.id.text44);

    }

    public void setBox() {

        box11.setImageResource(getBackground(matrix[1][1]));
        box12.setImageResource(getBackground(matrix[1][2]));
        box13.setImageResource(getBackground(matrix[1][3]));
        box14.setImageResource(getBackground(matrix[1][4]));

        box21.setImageResource(getBackground(matrix[2][1]));
        box22.setImageResource(getBackground(matrix[2][2]));
        box23.setImageResource(getBackground(matrix[2][3]));
        box24.setImageResource(getBackground(matrix[2][4]));

        box31.setImageResource(getBackground(matrix[3][1]));
        box32.setImageResource(getBackground(matrix[3][2]));
        box33.setImageResource(getBackground(matrix[3][3]));
        box34.setImageResource(getBackground(matrix[3][4]));

        box41.setImageResource(getBackground(matrix[4][1]));
        box42.setImageResource(getBackground(matrix[4][2]));
        box43.setImageResource(getBackground(matrix[4][3]));
        box44.setImageResource(getBackground(matrix[4][4]));

        String matrixS[][] = new String[5][5];
        for (int i = 0; i < 25; i++) {
            if (matrix[i / 5][i % 5] == 0) matrixS[i / 5][i % 5] = "";
            else matrixS[i / 5][i % 5] = String.valueOf(matrix[i / 5][i % 5]);
        }

        setSizetext();
        text11.setText(matrixS[1][1]);
        text12.setText(matrixS[1][2]);
        text13.setText(matrixS[1][3]);
        text14.setText(matrixS[1][4]);
        text21.setText(matrixS[2][1]);
        text22.setText(matrixS[2][2]);
        text23.setText(matrixS[2][3]);
        text24.setText(matrixS[2][4]);
        text31.setText(matrixS[3][1]);
        text32.setText(matrixS[3][2]);
        text33.setText(matrixS[3][3]);
        text34.setText(matrixS[3][4]);
        text41.setText(matrixS[4][1]);
        text42.setText(matrixS[4][2]);
        text43.setText(matrixS[4][3]);
        text44.setText(matrixS[4][4]);
    }


    public void setSizetext() {
        if (matrix[1][1] <= 512) text11.setTextSize(35);
        else text11.setTextSize(25);
        if (matrix[2][1] <= 512) text21.setTextSize(35);
        else text21.setTextSize(25);
        if (matrix[3][1] <= 512) text31.setTextSize(35);
        else text31.setTextSize(25);
        if (matrix[4][1] <= 512) text41.setTextSize(35);
        else text41.setTextSize(25);

        if (matrix[1][2] <= 512) text12.setTextSize(35);
        else text12.setTextSize(25);
        if (matrix[2][2] <= 512) text22.setTextSize(35);
        else text22.setTextSize(25);
        if (matrix[3][2] <= 512) text32.setTextSize(35);
        else text32.setTextSize(25);
        if (matrix[4][2] <= 512) text42.setTextSize(35);
        else text42.setTextSize(25);

        if (matrix[1][3] <= 512) text13.setTextSize(35);
        else text13.setTextSize(25);
        if (matrix[2][3] <= 512) text23.setTextSize(35);
        else text23.setTextSize(25);
        if (matrix[3][3] <= 512) text33.setTextSize(35);
        else text33.setTextSize(25);
        if (matrix[4][3] <= 512) text43.setTextSize(35);
        else text43.setTextSize(25);


        if (matrix[1][4] <= 512) text14.setTextSize(35);
        else text14.setTextSize(25);
        if (matrix[2][4] <= 512) text24.setTextSize(35);
        else text24.setTextSize(25);
        if (matrix[3][4] <= 512) text34.setTextSize(35);
        else text34.setTextSize(25);
        if (matrix[4][4] <= 512) text44.setTextSize(35);
        else text44.setTextSize(25);

    }

    /*
    Attach the corresponding color to each block
     */
    public int getBackground(int n) {
        if (n == 0) return R.drawable.box0;
        switch (n % 2048) {
            case 2: return R.drawable.box2;
            case 4: return R.drawable.box4;
            case 8: return R.drawable.box8;
            case 16: return R.drawable.box16;
            case 32: return R.drawable.box32;
            case 64: return R.drawable.box64;
            case 128: return R.drawable.box128;
            case 256: return R.drawable.box256;
            case 512: return R.drawable.box512;
            case 1024: return R.drawable.box1024;
            case 0: return R.drawable.box2048;
        }
        return 0;
    }

    public void setSaveMatrix() {
        for (int i = 1; i < 25; i++)
            saveMatrix[i / 5][i % 5] = matrix[i / 5][i % 5];
    }
    //the end of the copied code


    private void initCamera(){
        mCamera = Camera.open(1);   // 0 back 1 front
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        for(int i=0;i<previewSizes.size();i++){
            Log.v("CAMERA_1","width:" + previewSizes.get(i).width +"\theight:" + previewSizes.get(i).height);
        }
        if (mCamera != null) {
            try {
                framecount = 0;

                Camera.Parameters parameters = mCamera.getParameters();
                //Set the size of the preview photo
                parameters.setPreviewSize(cameraWidth, cameraHeight);
                mCamera.setParameters(parameters);
                previewCallBack pre = new previewCallBack();
                mCamera.setPreviewCallback(pre);
                mCamera.setPreviewTexture(surfacetexture);
                mCamera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    The original code obtained from the url was modified to generate animation for each new numbered tile
     */
    public void randomNumber() {
        Random random = new Random();
        while (true) {
            random_i = random.nextInt(4) + 1;
            random_j = random.nextInt(4) + 1;
            if (matrix[random_i][random_j] == 0) {
                if (random.nextInt(11) < 10)
                    matrix[random_i][random_j] = 2;
                else matrix[random_i][random_j] = 4;
                break;
            }
        }

        switch (random_i){
            case 1:
                switch (random_j){
                    case 1: setAppearAnim(box11); break;
                    case 2: setAppearAnim(box12); break;
                    case 3: setAppearAnim(box13); break;
                    case 4: setAppearAnim(box14); break;
                }
                break;
            case 2:
                switch (random_j){
                    case 1: setAppearAnim(box21); break;
                    case 2: setAppearAnim(box22); break;
                    case 3: setAppearAnim(box23); break;
                    case 4: setAppearAnim(box24); break;
                }
                break;
            case 3:
                switch (random_j){
                    case 1: setAppearAnim(box31); break;
                    case 2: setAppearAnim(box32); break;
                    case 3: setAppearAnim(box33); break;
                    case 4: setAppearAnim(box34); break;
                }
                break;
            case 4:
                switch (random_j){
                    case 1: setAppearAnim(box41); break;
                    case 2: setAppearAnim(box42); break;
                    case 3: setAppearAnim(box43); break;
                    case 4: setAppearAnim(box44); break;
                }
                break;
            default: break;
        }
    }

    public void setAppearAnim(ImageView imageView){
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.1f,1,0.1f,1,
                Animation.RELATIVE_TO_SELF,0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(120);
        imageView.setAnimation(null);
        imageView.startAnimation(scaleAnimation);
    }

    /*
    The original code obtained from the url was modified to provide sound prompts
     */
    public void retreat() {
        boolean check = true;
        for (int i = 1; i < 25; i++) {
            if (matrix[i / 5][i % 5] != saveMatrix[i / 5][i % 5]) {
                check = false;
                break;
            }
        }
        if (check == true) {
            Toast toast = Toast.makeText(this, "you can only retreat once", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
            playSound(6);
            return;
        }

        playSound(5);

        for (int i = 1; i < 25; i++)
            matrix[i / 5][i % 5] = saveMatrix[i / 5][i % 5];
        setBox();
    }

    /*
    The original code obtained from the url was modified to comply with the game rules I designed
     */
    public void gameOver() {
        for (int i = 1; i < 5; i++)
            for (int j = 1; j < 5; j++)
                if (matrix[i][j] == 0)
                    return;
        String check = "game over";
        for (int i = 1; i < 5; i++) {
            for (int j = 1; j < 4; j++) {
                if (matrix[i][j] == matrix[i][j + 1]) check = "ok";
                if (matrix[j][i] == matrix[j + 1][i]) check = "ok";
            }
        }

        for (int i = 1; i < 5; i++){
            for (int j = 1; j < 5; j++){
                if (matrix[i][j] == 2048) {
                    check = "win";
                    break;
                }
            }
        }

        if (check.equals("game over")) {
            Toast toast = Toast.makeText(getApplicationContext(), "Game Over", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER,0,0);
            LinearLayout layout = (LinearLayout) toast.getView();
            TextView tv = (TextView) layout.getChildAt(0);
            tv.setTextSize(30);
            toast.show();
            newGame();
        }

        if (check.equals("win")) {
            Toast toast = Toast.makeText(getApplicationContext(), "You win!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            LinearLayout layout = (LinearLayout) toast.getView();
            TextView tv = (TextView) layout.getChildAt(0);
            tv.setTextSize(30);
            toast.show();
            newGame();
        }
    }

    class previewCallBack implements Camera.PreviewCallback {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            boolean check = false;
//            setSaveMatrix();

            int[] compareL_Img = new int[3200];
            int[] compareR_Img = new int[3200];
            String L_str = null;
            String R_str = null;

            framecount = framecount + 1;
            mframestart = System.currentTimeMillis();
            Bitmap bmp = Bitmap.createBitmap(viewWidth,viewHeight, Bitmap.Config.ARGB_8888);
            Bitmap leftEyeBitmap = Bitmap.createBitmap(eyeWidth,eyeHeigth, Bitmap.Config.ARGB_8888);
            Bitmap rightEyeBitmap = Bitmap.createBitmap(eyeWidth,eyeHeigth, Bitmap.Config.ARGB_8888);
            Bitmap eyesshow = Bitmap.createBitmap(eyeWidth,eyeHeigth * 2, Bitmap.Config.ARGB_8888);
            int detected = decode(data, bmp, cameraHeight, cameraWidth, viewHeight, viewWidth,
                    eyeHeigth, eyeWidth, leftEyeBitmap, rightEyeBitmap, eyesshow);

            bmp = rotateBimap(RunActivity.this, -90, bmp);
            eyesshow = rotateBimap(RunActivity.this, -90, eyesshow);
            if(detected > 0){
                leftEyeBitmap = rotateBimap(RunActivity.this, -90, leftEyeBitmap);
                rightEyeBitmap = rotateBimap(RunActivity.this, -90, rightEyeBitmap);
                normalize(leftEyeBitmap, compareL_Img);
                normalize(rightEyeBitmap, compareR_Img);

                L_str = leftCompare(compareL_Img);
                R_str = rightCompare(compareR_Img);
            }


            mImageView.setImageBitmap(bmp);
            //mEyeView.setImageBitmap(eyesshow);
            //mEyeView.setImageBitmap(leftEyeBitmap);
            mframeend = System.currentTimeMillis();
            mframetime = mframeend - mframestart;
            //String show = "average fps:" + 1000 / mframetime;
//            mTextView.setText(show);
            //mTextView.setText("centre: "+ mse_centre +"  left: "+ mse_left + "  right: " + mse_right + "\n"
//                              + "up: "+ mse_up + "  down: "+ mse_down);

            //statusView.setText("mse[5]"+ mse_down+"\n"+"result: " + R_str);
            //statusView.setText("str right eye" +"\n" + "R_centre: " + R_centre + "\n" + "R_close: " + R_close);

            //Determine the direction of the eyes
            if(L_str == "look left" && R_str == "look left") {
                left_n++;
                if (left_n == 4) {
                    Toast toast = Toast.makeText(RunActivity.this, "left", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                   // left_n++;
                    //right_n = up_n = down_n = 0;
                    down_n = 6; centre_n = 0;
                    setSaveMatrix();
                    leftFunc(check);
                    playSound(1);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("left " + left_n);
            }
            else if(L_str == "look right" && R_str == "look right"){
                right_n++;
                if (right_n == 4) {
                    Toast toast = Toast.makeText(RunActivity.this, "right", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    //right_n++;
                    //left_n = up_n = down_n = 0;
                    down_n = 6; centre_n = 0;
                    setSaveMatrix();
                    rightFunc(check);
                    playSound(2);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("right " + right_n);

            }
            else if(L_str == "look up" && R_str == "look up"){
                up_n++;
                if (up_n == 4){
                    Toast toast = Toast.makeText(RunActivity.this, "up", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    //left_n = right_n = down_n = 0;
                    down_n = 6; centre_n = 0;
                    setSaveMatrix();
                    upFunc(check);
                    playSound(3);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("up "+ up_n);
            }
            else if(L_str == "look down" ){ //&& R_str == "look down"
                down_n++;
                if (down_n == 5){
                    Toast toast = Toast.makeText(RunActivity.this, "down", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    //down_n++;
                    //left_n = right_n = up_n =0;
                    centre_n = 0;
                    setSaveMatrix();
                    downFunc(check);
                    playSound(4);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("down "+ down_n);
            }
            else if(L_str == "look centre" && R_str == "look centre"){
                centre_n++;
                if(centre_n >= 5){
                    left_n = right_n = up_n = down_n = back_n = retreat_n = 0;
                }
                //statusView.setText("centre");
                //statusView.setText("result: " + str);
            }else if(L_str == "close" && R_str == "close") {
                down_n = 6;
                //centre_n = 0;
                //statusView.setText("result: " + str);
            }else if(L_str == "close" && R_str != "close"){
                back_n++;
                if(back_n == 5){
                    Toast toast = Toast.makeText(RunActivity.this, "back to homepage", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    down_n = centre_n = 0;
                    Intent intent = new Intent(RunActivity.this, StartActivity.class);
                    startActivity(intent);
//                    finish();
                }
            }else if(L_str != "close" && R_str == "close"){
                retreat_n++;
                if(retreat_n == 5){
                    Toast toast = Toast.makeText(RunActivity.this, "retreat", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    retreat();
                    down_n = centre_n = 0;
                }

            }
        }
    }

    /*
    The leftFunc, rightFunc, upFunc and downFunc come from the url, and they were combined with eye gesture recognition
     */
    public void leftFunc(boolean check){
        for(int i=1;i<5;i++){
            for(int j=1;j<4;j++)
            {
                if(matrix[i][j]!=0)
                {
                    for(int k=j+1; k<5;k++)
                    {
                        if(matrix[i][k]==matrix[i][j])
                        {
                            check = true;
                            matrix[i][j] += matrix[i][k];
                            matrix[i][k] = 0;
                            j=k;
                            break;
                        }else if(matrix[i][k]!=0) break;
                    }
                }
            }
            for(int j=1;j<4;j++)
            {
                if(matrix[i][j] == 0)
                {
                    for(int k=j+1; k<5;k++)
                    {
                        if(matrix[i][k]!=0)
                        {
                            check = true;
                            matrix[i][j] = matrix[i][k];
                            matrix[i][k] = 0;
                            break;
                        }
                    }
                }
            }
        }
        if(check == true)
            randomNumber();
        gameOver();
        setBox();
    }

    public void rightFunc(boolean check){
        for (int i = 1; i < 5; i++) {
            for (int j = 4; j > 1; j--) {

                if (matrix[i][j] != 0) {
                    for (int k = j - 1; k > 0; k--) {
                        if (matrix[i][k] == matrix[i][j]) {
                            check = true;
                            matrix[i][j] += matrix[i][k];
                            matrix[i][k] = 0;
                            j = k;
                            break;
                        } else if (matrix[i][k] != 0) break;
                    }
                }
            }
            for (int j = 4; j > 1; j--) {
                if (matrix[i][j] == 0) {
                    for (int k = j - 1; k > 0; k--) {
                        if (matrix[i][k] != 0) {
                            check = true;
                            matrix[i][j] = matrix[i][k];
                            matrix[i][k] = 0;
                            break;
                        }
                    }
                }
            }
        }
        if(check == true)
            randomNumber();
        gameOver();
        setBox();
    }

    public void upFunc(boolean check){
        for (int i = 1; i < 5; i++) {
            for (int j = 1; j < 4; j++) {
                if (matrix[j][i] != 0) {
                    for (int k = j + 1; k < 5; k++) {
                        if (matrix[k][i] == matrix[j][i]) {
                            check = true;
                            matrix[j][i] += matrix[k][i];
                            matrix[k][i] = 0;
                            j = k;
                            break;
                        }
                        if (matrix[k][i] != 0) break;
                    }
                }
            }
            for (int j = 1; j < 4; j++) {
                if (matrix[j][i] == 0) {
                    for (int k = j + 1; k < 5; k++) {
                        if (matrix[k][i] != 0) {
                            check = true;
                            matrix[j][i] = matrix[k][i];
                            matrix[k][i] = 0;
                            break;
                        }
                    }
                }
            }
        }
        if(check == true)
            randomNumber();
        gameOver();
        setBox();
    }

    public void downFunc(boolean check){
        for (int i = 1; i < 5; i++) {
            for (int j = 4; j > 1; j--) {
                if (matrix[j][i] != 0) {
                    for (int k = j - 1; k > 0; k--) {
                        if (matrix[k][i] == matrix[j][i]) {
                            check = true;
                            matrix[j][i] += matrix[k][i];
                            matrix[k][i] = 0;
                            j = k;
                            break;
                        } else if (matrix[k][i] != 0) break;
                    }
                }
            }
            for (int j = 4; j > 1; j--) {
                if (matrix[j][i] == 0) {
                    for (int k = j - 1; k > 0; k--) {
                        if (matrix[k][i] != 0) {
                            check = true;
                            matrix[j][i] = matrix[k][i];
                            matrix[k][i] = 0;
                            break;
                        }
                    }
                }
            }
        }

        if (check == true)
            randomNumber();
        gameOver();
        setBox();
    }

    public void normalize(Bitmap img, int[] pixels) {
        int width = img.getWidth();
        int height = img.getHeight();
        img.getPixels(pixels, 0, width, 0, 0, width, height);

        int sumPixel = 0;
        for (int pixel : pixels) {
            sumPixel += pixel;
        }
        int avgPixel = sumPixel / pixels.length;

        for (int x=0; x<width*height; x++){
            pixels[x] = pixels[x] - avgPixel;
        }
    }

    public float mse(int[] pixels_a, int[] pixels_com){
        float sum = 0.0f;
        float diff;
        float mse;

        for(int x=0; x<3200; x++){
            diff = pixels_a[x] - pixels_com[x];
            sum = sum + diff*diff;
        }
        mse = sum / pixels_a.length;
        mse = (float) (mse/Math.pow(2,32)*Math.pow(2,8));
        return mse;
    }

    public String leftCompare(int[] compareImg){
        String str;
        float[] mse = new float[6];

        mse[0] = mse(temL_Cen, compareImg);
        mse[1] = mse(temL_Close, compareImg);
        mse[2] = mse(temL_Left, compareImg);
        mse[3] = mse(temL_Right, compareImg);
        mse[4] = mse(temL_Up, compareImg);
        mse[5] = mse(temL_Down, compareImg);

        mse_centre = mse[0];
        mse_close = mse[1];
        mse_left = mse[2];
        mse_right = mse[3];
        mse_up = mse[4];
        mse_down = mse[5];

        int index = 0;
        for(int i = 1; i<mse.length; i++){
            if(mse[i] < mse[index]){
                index = i;
            }
        }

        if(index == 1) {
            str = "close";
        }else if(index == 2){
            str = "look left";
        }else if(index == 3){
            str = "look right";
        }else if(index == 4){
            str = "look up";
        }else if(index == 5){
            str = "look down";
        }else{
            str = "look centre";
        }

        return str;
    }

    public String rightCompare(int[] compareImg){
        String str;
        float[] mse = new float[6];

        mse[0] = mse(temR_Cen, compareImg);
        mse[1] = mse(temR_Close, compareImg);
        mse[2] = mse(temR_Left, compareImg);
        mse[3] = mse(temR_Right, compareImg);
        mse[4] = mse(temR_Up, compareImg);
        mse[5] = mse(temR_Down, compareImg);

        mse_centre = mse[0];
        mse_close = mse[1];
        mse_left = mse[2];
        mse_right = mse[3];
        mse_up = mse[4];
        mse_down = mse[5];

        int index = 0;
        for(int i = 1; i<mse.length; i++){
            if(mse[i] < mse[index]){
                index = i;
            }
        }

        if(index == 1) {
            str = "close";
        }else if(index == 2){
            str = "look left";
        }else if(index == 3){
            str = "look right";
        }else if(index == 4){
            str = "look up";
        }else if(index == 5){
            str = "look down";
        }else{
            str = "look centre";
        }

        return str;
    }


    public Bitmap rotateBimap(Context context, float degree, Bitmap srcBitmap) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degree);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()
                , matrix, true);
        return bitmap;
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
