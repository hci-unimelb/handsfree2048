package com.example.uitest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class StartActivity extends AppCompatActivity {

    private Orientation orientation;
    private float AZ;
    private float AX;
    private float AY;
    private int count = 0;
    private int left_n = 0;
    private int right_n = 0;

    private TextView debug;
    private Button eye_btn;
    private Button tilt_btn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        debug = (TextView) findViewById(R.id.debug);


        eye_btn = (Button) findViewById(R.id.eye_tracking);
        tilt_btn = (Button) findViewById(R.id.tilt);
        eye_btn.setClickable(false);
        tilt_btn.setClickable(false);

//        eye_btn.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(StartActivity.this, EyeActivity.class);
//                startActivity(intent);
//            }
//        });
//
//        tilt_btn.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(StartActivity.this, TiltActivity.class);
//                //intent.putExtra("playgame",2);
//                startActivity(intent);
//            }
//        });

        final Toast toast = Toast.makeText(getApplicationContext(),"Now you can tilt the phone to choose a mode!", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);

        orientation = new Orientation(this);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.show();
                direction();
            }
        }, 2000);

    }

    private void direction(){
        orientation.setListener(new Orientation.Listener() {
            @Override
            public void onTranslation(float az, float ax, float ay) {
                count++;
                if(count == 4){
                    AZ = az;
                    AX = ax;
                    AY = ay;
                }
                if(count > 20){
                    count = 8;
                }
                //debug.setText("AZ  " + AZ + "   AY " + AY + "   AX " + AX);
                //debug.setText("az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay +"\n"+"left  " + left_n +"\n" + "right  " + right_n);
                if (ay-AY < -20.0f){
                    left_n++;
                    if (left_n == 6){
                        tilt_btn.setBackgroundColor(0xfff57c00);
                        Intent intent = new Intent(StartActivity.this, TiltActivity.class);
                        startActivity(intent);
                    }
                    if (left_n > 6)
                        tilt_btn.setBackgroundColor(0xfff57c00);
                    //debug.setText("az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay +"\n"+"left  " + left_n +"\n" + "right  " + right_n);
                }else if(ay-AY > 20.0f){
                    right_n++;
                    if (right_n == 6){
                        eye_btn.setBackgroundColor(0xfff57c00);
                        //left_n = 0;
                        Intent intent = new Intent(StartActivity.this, EyeActivity.class);
                        startActivity(intent);
                    }
                    if (right_n > 6)
                        eye_btn.setBackgroundColor(0xfff57c00);
                    //debug.setText("az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay +"\n"+"left  " + left_n +"\n" + "right  " + right_n);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientation.register();
        tilt_btn.setBackgroundColor(0xffffa726);
        eye_btn.setBackgroundColor(0xffffa726);
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