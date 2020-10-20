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
import android.view.MotionEvent;
import android.view.View;
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

public class RunActivity extends AppCompatActivity {

    private String TAG = "RunActivity";

    //测试用
    private float mse_centre;
    private float mse_close;
    private float mse_down;
    private float mse_up;
    private float mse_left;
    private float mse_right;

    //Templates
    private ImageView centreView;
    private ImageView closeView;
    private ImageView leftView;
    private ImageView rightView;
    private ImageView upView;
    private ImageView downView;

    private Bitmap bitmapCentre;
    private Bitmap bitmapClose;
    private Bitmap bitmapLeft;
    private Bitmap bitmapRight;
    private Bitmap bitmapUp;
    private Bitmap bitmapDown;

    private TextView infoView;
    private TextView statusView;

    //样本的pixel数组
    private int[] temCen = new int[3200];
    private int[] temClose = new int[3200];
    private int[] temLeft = new int[3200];
    private int[] temRight = new int[3200];
    private int[] temUp = new int[3200];
    private int[] temDown = new int[3200];

    private long mframestart, mframeend, mframetime;
    private long framecount;
    private Camera mCamera;

    private boolean saved;
    private int saveidx;

    private ImageView mImageView, mEyeView;
    private TextView mTextView;
    private SurfaceTexture surfacetexture;

    private int viewWidth, viewHeight;//mSurfaceView的宽和高
    private int eyeWidth, eyeHeigth;
    private int cameraWidth, cameraHeight;
    Timer timer;

    //play sound
    private SoundPool mSoundPool = null;
    private HashMap<Integer, Integer> soundID = new HashMap<>();

    //游戏变量
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
    private int scores = 0;
    Button retreat, reset;
    private int matrix[][], saveMatrix[][];
    private TextView score, best_score;
    private Database database;

    private int left_n = 0;
    private int right_n = 0;
    private int up_n = 0;
    private int down_n = 0;
    private int centre_n = 0;

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
        //mEyeView = (ImageView)findViewById(R.id.eye_view2);
        //mTextView = (TextView)findViewById(R.id.textView2);

        //展示存储的图片
//        centreView = (ImageView) findViewById(R.id.centre_view);
//        closeView = (ImageView) findViewById(R.id.close_view);
//        leftView = (ImageView) findViewById(R.id.left_view);
//        rightView = (ImageView) findViewById(R.id.right_view);
//        upView = (ImageView) findViewById(R.id.up_view);
//        downView = (ImageView) findViewById(R.id.down_view);

        //展示长宽信息
        //infoView = (TextView)findViewById(R.id.info);

        //展示状态信息
        //statusView = (TextView)findViewById(R.id.status_view);

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

        //读取图片
        loadTemplates();

        initView();
        newGame();   //默认新游戏
        //keepPlay();

        initSoundPool();
    }

    private void initSoundPool(){
        mSoundPool = new SoundPool(4, AudioManager.STREAM_SYSTEM,5);
        soundID.put(1,mSoundPool.load(this, R.raw.left,1));
        soundID.put(2,mSoundPool.load(this, R.raw.right,1));
        soundID.put(3,mSoundPool.load(this, R.raw.up,1));
        soundID.put(4,mSoundPool.load(this, R.raw.down,1));
    }

    private void playSound(int soundId){
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC);  //根据媒体音量调节
        float streamVolumeMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = streamVolumeCurrent / streamVolumeMax;
        mSoundPool.play(soundID.get(soundId),volume,volume,1,0,1);
    }

    private void loadTemplates(){

//        String left = "//data/data/com.example.uitest/leftEye/left_5.jpg";
//        String right = "//data/data/com.example.uitest/leftEye/left_7.jpg";
//        String up = "//data/data/com.example.uitest/leftEye/left_9.jpg";
//        String down = "//data/data/com.example.uitest/leftEye/left_11.jpg";

        String centre = "//data/data/com.example.uitest/leftEye/L_centre.jpg";
        String close = "//data/data/com.example.uitest/leftEye/L_close.jpg";
        String left = "//data/data/com.example.uitest/leftEye/L_left.jpg";
        String right = "//data/data/com.example.uitest/leftEye/L_right.jpg";
        String up = "//data/data/com.example.uitest/leftEye/L_up.jpg";
        String down = "//data/data/com.example.uitest/leftEye/L_down.jpg";

        try {
            FileInputStream inputStream0 = new FileInputStream(centre);
            FileInputStream inputStream1 = new FileInputStream(close);
            FileInputStream inputStream2 = new FileInputStream(left);
            FileInputStream inputStream3 = new FileInputStream(right);
            FileInputStream inputStream4 = new FileInputStream(up);
            FileInputStream inputStream5 = new FileInputStream(down);

            bitmapCentre = BitmapFactory.decodeStream(inputStream0);
            bitmapClose = BitmapFactory.decodeStream(inputStream1);
            bitmapLeft = BitmapFactory.decodeStream(inputStream2);
            bitmapRight = BitmapFactory.decodeStream(inputStream3);
            bitmapUp = BitmapFactory.decodeStream(inputStream4);
            bitmapDown = BitmapFactory.decodeStream(inputStream5);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG,"file not found");
        }

//        centreView.setImageBitmap(bitmapCentre);
//        closeView.setImageBitmap(bitmapClose);
//        leftView.setImageBitmap(bitmapLeft);
//        rightView.setImageBitmap(bitmapRight);
//        upView.setImageBitmap(bitmapUp);
//        downView.setImageBitmap(bitmapDown);

        normalize(bitmapCentre, temCen);
        normalize(bitmapClose, temClose);
        normalize(bitmapLeft, temLeft);
        normalize(bitmapRight, temRight);
        normalize(bitmapUp, temUp);
        normalize(bitmapDown, temDown);

        int width = bitmapUp.getWidth();
        int height = bitmapUp.getHeight();
        //textView.setText("width =  " + width + " height =  "+ height);
        //textView.setText("temLeft[25] =  " + temLeft[25]);

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
        timer.cancel();
        saveProcess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSoundPool.release();
        mSoundPool = null;
    }

    public void saveProcess() {
        database = new Database(this);
        database.setProcess(matrix);
        if (Integer.valueOf(database.getBestScore2048(this)) < scores) {
            database.setBestScore2048(scores);
        }
        database.setDiemHT2048(scores);
        database.close();
    }

    public void keepPlay() {
        database = new Database(this);
        best_score.setText(database.getBestScore2048(this));
        scores = Integer.valueOf(database.getDiemHT2048(this));
        matrix = database.getQuatrinh(this);
        database.close();
        setSaveMatrix();
        setBox();
    }

    public void newGame() {
        database = new Database(this);
        if (Integer.valueOf(database.getBestScore2048(this)) < scores) {
            database.setBestScore2048(scores);
        }
        //best_score.setText(database.getBestScore2048(this));
        database.close();
        scores = 0;
        for (int i = 0; i < 25; i++) matrix[i / 5][i % 5] = 0;
        randomNumber();   //最开始初始化两个数字
        randomNumber();
        setSaveMatrix();
        setBox();
    }

    void initView() {
        matrix = new int[5][5];
        saveMatrix = new int[5][5];
        //score = (TextView) findViewById(R.id.score);
        //best_score = (TextView) findViewById(R.id.best_score);
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

        //数字
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

        //retreat = (Button) findViewById(R.id.retreat);
        //reset = (Button) findViewById(R.id.reset);
    }

    public void setBox() {
        //score.setText(String.valueOf(scores));
        //把每个数字快的背景改为对应颜色
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
        //text显示对应的数字(String)
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

    /*
修正数字的大小，超过512变为25dp
 */
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
    给每个字块附上对应的颜色
     */
    public int getBackground(int n) {
        if (n == 0) return R.drawable.box0;
        switch (n % 2048) {
            case 2:
                return R.drawable.box2;
            case 4:
                return R.drawable.box4;
            case 8:
                return R.drawable.box8;
//            case 16:
//                return R.drawable.box16;
//            case 32:
//                return R.drawable.box2;
//            case 64:
//                return R.drawable.box4;
//            case 128:
//                return R.drawable.box8;
//            case 256:
//                return R.drawable.box16;
//            case 512:
//                return R.drawable.box2;
//            case 1024:
//                return R.drawable.box4;
//            case 0:
//                return R.drawable.box2048;
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

    /*
generate a random number
 */
    public void randomNumber() {
        Random random = new Random();
        while (true) {
            int i = random.nextInt(4) + 1, j = random.nextInt(4) + 1;    //i,j为[1,5)内任意的值
            if (matrix[i][j] == 0) {
                if (random.nextInt(11) < 10) //[0,11)内产生随机数，只有1/10的可能为10
                    matrix[i][j] = 2;
                else matrix[i][j] = 4;   //随机数≥10时生成新的随机数为4
                break;
            }
        }
    }

    public void setSaveMatrix() {
        for (int i = 1; i < 25; i++)
            saveMatrix[i / 5][i % 5] = matrix[i / 5][i % 5];
    }

    public void gameOver() {
        for (int i = 1; i < 5; i++)
            for (int j = 1; j < 5; j++)
                if (matrix[i][j] == 0)
                    return;
        boolean check = false;
        for (int i = 1; i < 5; i++) {
            for (int j = 1; j < 4; j++) {
                if (matrix[i][j] == matrix[i][j + 1]) check = true;
                if (matrix[j][i] == matrix[j + 1][i]) check = true;
            }
        }
        if (check == false) {
//            final Dialog dialog = new Dialog(this);
//            dialog.setCancelable(false);
//            dialog.setContentView(R.layout.game_over);
//            dialog.show();
//            final Button menu = (Button) dialog.findViewById(R.id.menu);
//            Button Again = (Button) dialog.findViewById(R.id.Again) ;
//            TextView diemso = (TextView) dialog.findViewById(R.id.diemso);
//            TextView diemcao = (TextView) dialog.findViewById(R.id.diemcao);
//            diemso.setText("New "+String.valueOf(scores));

//            Database  database;
//            database = new Database(this);
//            if(Integer.valueOf(database.getBestScore2048(this)) < scores){
//                database.setBestScore2048(scores);
//            }
//            database.close();

//            database = new Database(this);
//            diemcao.setText("Best "+database.getDiemcao2048(this));
//            database.close();
//            menu.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    startActivity(new Intent(Play2048.this, MainActivity.class));
//                }
//            });
//            Again.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    khoitao();
//                    dialog.cancel();
//                }
//            });
            //Toast.makeText(TiltActivity.this, "Game Over", Toast.LENGTH_LONG).show();

            Toast toast = Toast.makeText(getApplicationContext(), "Game Over", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER,0,0);
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
            //新添的
            boolean check = false;
            setSaveMatrix();

            float a = 0.0f;
            float b = 0.0f;
            int[] compareImg = new int[3200];
            String str = null;
            framecount = framecount + 1;
            mframestart = System.currentTimeMillis(); //起始时间
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
                normalize(leftEyeBitmap, compareImg);
                str = compare(compareImg);
            }
//            if(detected > 0 && saved && saveidx < 5){
//                leftEyeBitmap = rotateBimap(RunActivity.this, -90, leftEyeBitmap);
//                int width = normalize(leftEyeBitmap);
//                //statusView.setText(saveidx);
//                saveidx++;
//                saved = false;
//            }

//            if(detected > 0 && saved && saveidx < 5){
//                leftEyeBitmap = rotateBimap(RunActivity.this, -90, leftEyeBitmap);
//                String fileName = "//data/data/com.example.uitest/leftEye/m_" + saveidx + ".jpg";
//                savebitmap(fileName, leftEyeBitmap);
//
//                rightEyeBitmap = rotateBimap(RunActivity.this, -90, rightEyeBitmap);
//                fileName = "//data/data/com.example.uitest/rightEye/n_" + saveidx + ".jpg";
//                savebitmap(fileName, rightEyeBitmap);
//                saveidx++;
//                saved = false;
//            }

            if(saveidx >= 100){
                timer.cancel();
            }
            mImageView.setImageBitmap(bmp);
            //mEyeView.setImageBitmap(eyesshow);
            //mEyeView.setImageBitmap(leftEyeBitmap);
            mframeend = System.currentTimeMillis(); //起始时间
            mframetime = mframeend - mframestart;
            //String show = "average fps:" + 1000 / mframetime;
//            mTextView.setText(show);
            //mTextView.setText("centre: "+ mse_centre +"  left: "+ mse_left + "  right: " + mse_right + "\n"
//                              + "up: "+ mse_up + "  down: "+ mse_down);

            //statusView.setText("result: " + str);
            //根据判断结果改变方块

            //str可能是null，look left等
            if(str == "look left") {
                //左滑
                left_n++;
                if (left_n == 4) {
                    Toast toast = Toast.makeText(RunActivity.this, "left", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    left_n++;
                    //right_n = up_n = down_n = 0;
                    centre_n = 0;
                    leftFunc(check);
                    playSound(1);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("left " + left_n);
            }
            else if(str == "look right"){
                //右滑
                right_n++;
                if (right_n == 4) {
                    Toast toast = Toast.makeText(RunActivity.this, "right", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    right_n++;
                    //left_n = up_n = down_n = 0;
                    centre_n = 0;
                    rightFunc(check);
                    playSound(2);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("right " + right_n);

            }
            else if(str == "look up"){
                //上滑
                up_n++;
                if (up_n == 4){
                    Toast toast = Toast.makeText(RunActivity.this, "up", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    up_n++;
                    //left_n = right_n = down_n = 0;
                    centre_n = 0;
                    upFunc(check);
                    playSound(3);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("up "+ up_n);
            }
            else if(str == "look down"){
                //下滑
                down_n++;
                if (down_n == 5){ //超过眨眼时间
                    Toast toast = Toast.makeText(RunActivity.this, "down", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    down_n++;
                    //left_n = right_n = up_n =0;
                    centre_n = 0;
                    downFunc(check);
                    playSound(4);
                }
                //statusView.setText("result: " + str);
                //infoView.setText("down "+ down_n);
            }
            else if(str == "look centre"){
                centre_n++;
                if(centre_n >= 4){
                    left_n = right_n = up_n = down_n = 0;
                }
                //statusView.setText("result: " + str);
            }else if(str == "close") {
                down_n = 0;
                //statusView.setText("result: " + str);
            }else{  //null
                //statusView.setText("result: " + str);
            }
        }
    }

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
                            scores += matrix[i][j];
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
                            scores += matrix[i][j];
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
                            scores += matrix[j][i];
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
                            scores += matrix[j][i];
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
//        int[] pixels = new int[width * height];
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

    public String compare(int[] compareImg){
        String str;
        float[] mse = new float[6];

        mse[0] = mse(temCen, compareImg);
        mse[1] = mse(temClose, compareImg);
        mse[2] = mse(temLeft, compareImg);
        mse[3] = mse(temRight, compareImg);
        mse[4] = mse(temUp, compareImg);
        mse[5] = mse(temDown, compareImg);

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
}
