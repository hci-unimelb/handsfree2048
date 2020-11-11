package com.example.uitest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ShowTemplates extends AppCompatActivity {

    private ImageView L_centreView, L_leftView, L_rightView, L_upView, L_downView, L_closeView;
    private ImageView R_centreView, R_leftView, R_rightView, R_upView, R_downView, R_closeView;

    private Bitmap L_centre, L_left, L_right, L_up, L_down, L_close;
    private Bitmap R_centre, R_left, R_right, R_up, R_down, R_close;

    private FileInputStream inputStream;
    private String prefix =  "//data/data/com.example.uitest/";

    private Button back, next;
    private Orientation orientation;
    private float AZ;
    private float AX;
    private float AY;
    private int count = 0;
    private int left_n = 0;
    private int right_n = 0;

    //private TextView debug;
    private TextView prompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_templates);

        initView();

        prompt = (TextView) findViewById(R.id.prompt);
        //debug = (TextView) findViewById(R.id.debug);

        back = (Button) findViewById(R.id.back);
        next = (Button) findViewById(R.id.next);
        back.setClickable(false);
        next.setClickable(false);

        orientation = new Orientation(this);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                direction();
            }
        }, 1500);

        prompt.setText("You can check the templates here" + "\n"
                + "If they are good, tilt right to play!" + "\n"
                + "otherwise, you can tilt left to reinitialize.");
        prompt.setTextSize(20);

        try {
            L_centre = load("leftEye/L_centre.jpg");
            L_left = load("leftEye/L_left.jpg");
            L_right = load("leftEye/L_right.jpg");
            L_up = load("leftEye/L_up.jpg");
            L_down = load("leftEye/L_down.jpg");
            L_close = load("leftEye/L_close.jpg");

            R_centre = load("rightEye/R_centre.jpg");
            R_left = load("rightEye/R_left.jpg");
            R_right = load("rightEye/R_right.jpg");
            R_up = load("rightEye/R_up.jpg");
            R_down = load("rightEye/R_down.jpg");
            R_close = load("rightEye/R_close.jpg");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        showTemplates();

//        final Toast toast = Toast.makeText(getApplicationContext(), "Now you can tilt!", Toast.LENGTH_SHORT);
//        toast.setGravity(Gravity.CENTER, 0, 0);
//        LinearLayout layout = (LinearLayout) toast.getView();
//        TextView tv = (TextView) layout.getChildAt(0);
//        tv.setTextSize(20);
    }

    private void initView(){

        L_centreView = (ImageView) findViewById(R.id.L_centre);
        L_leftView = (ImageView) findViewById(R.id.L_left);
        L_rightView = (ImageView) findViewById(R.id.L_right);
        L_upView = (ImageView) findViewById(R.id.L_up);
        L_downView = (ImageView) findViewById(R.id.L_down);
        L_closeView = (ImageView) findViewById(R.id.L_close);

        R_centreView = (ImageView) findViewById(R.id.R_centre);
        R_leftView = (ImageView) findViewById(R.id.R_left);
        R_rightView = (ImageView) findViewById(R.id.R_right);
        R_upView = (ImageView) findViewById(R.id.R_up);
        R_downView = (ImageView) findViewById(R.id.R_down);
        R_closeView = (ImageView) findViewById(R.id.R_close);
    }

    private Bitmap load(String fileName) throws FileNotFoundException {
        String str = prefix + fileName;
        inputStream = new FileInputStream(str);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return bitmap;
    }

    private void showTemplates(){

        L_centreView.setImageBitmap(bitmapScale(L_centre));
        L_closeView.setImageBitmap(bitmapScale(L_close));
        L_leftView.setImageBitmap(bitmapScale(L_left));
        L_rightView.setImageBitmap(bitmapScale(L_right));
        L_upView.setImageBitmap(bitmapScale(L_up));
        L_downView.setImageBitmap(bitmapScale(L_down));

        R_centreView.setImageBitmap(bitmapScale(R_centre));
        R_closeView.setImageBitmap(bitmapScale(R_close));
        R_leftView.setImageBitmap(bitmapScale(R_left));
        R_rightView.setImageBitmap(bitmapScale(R_right));
        R_upView.setImageBitmap(bitmapScale(R_up));
        R_downView.setImageBitmap(bitmapScale(R_down));

    }

    public static Bitmap bitmapScale(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postScale(3,3);

        Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        return resizeBmp;
    }

    private void direction(){
        orientation.setListener(new Orientation.Listener() {
            @Override
            public void onTranslation(float az, float ax, float ay) {
                count++;
                if(count == 1){
                    AZ = az;
                    AX = ax;
                    AY = ay;
                }
                if(count > 20){
                    count = 8;
                }
                //debug.setText("az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay +"\n"+"left  " + left_n +"\n" + "right  " + right_n);
                if (ay-AY < -25.0f){
                    left_n++;
                    if (left_n == 6){
                        back.setBackgroundColor(0xfff57c00);
                        Intent intent = new Intent(ShowTemplates.this, EyeActivity.class);
                        startActivity(intent);
                    }
                    back.setBackgroundColor(0xfff57c00);
                    //debug.setText("az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay +"\n"+"left  " + left_n +"\n" + "right  " + right_n);
                }else if(ay-AY > 25.0f){
                    right_n++;
                    if (right_n == 6){
                        next.setBackgroundColor(0xfff57c00);
                        Intent intent = new Intent(ShowTemplates.this, RunActivity.class);
                        startActivity(intent);
                    }
                    next.setBackgroundColor(0xfff57c00);
                    //debug.setText("az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay +"\n"+"left  " + left_n +"\n" + "right  " + right_n);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientation.register();
        back.setBackgroundColor(0xffffa726);
        next.setBackgroundColor(0xffffa726);
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientation.unregister();
    }

    @Override
    protected void onStop() {
        super.onStop();
        orientation.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        orientation.unregister();
    }
}