package com.example.uitest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Orientation orientation;
    private TextView translationView;
    private TextView rotationView;
    private TextView statusView;
    private int count = 0;
    private float AZ;
    private float AX;
    private float AY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye);

        orientation = new Orientation(this);
        translationView = (TextView) this.findViewById(R.id.translationView);
        rotationView = (TextView) this.findViewById(R.id.rotationView);
        statusView = (TextView) this.findViewById(R.id.statusView);

        orientation.setListener(new Orientation.Listener() {
            @Override
            public void onTranslation(float az, float ax, float ay) {
                count++;
                if(count == 4) {
                    AZ = az;
                    AX = ax;
                    AY = ay;
                }

                if(count > 20){
                    count = 8;
                }
                translationView.setText("Translation: " + "\n" + "Around_z= " + az + "\n" + "Around_x= " + ax +"\n" + "Around_y= " + ay);

                rotationView.setText("AZ  " + AZ + "\n" + "AX  " + AX + "\n" + "AY  " + AY);
                statusView.setText("Original");

                if( ay-AY > 15.0f ){
                    statusView.setText("tilt left");
                    Toast.makeText(MainActivity.this,"tilt left", Toast.LENGTH_SHORT).show();
                }else if( ay-AY < -13.0f ) {
                    statusView.setText("tilt right");
                    Toast.makeText(MainActivity.this,"tilt right", Toast.LENGTH_SHORT).show();
                }else if( ax-AX > 13.0f ){
                    statusView.setText("tilt up");
                    Toast.makeText(MainActivity.this,"tilt up", Toast.LENGTH_SHORT).show();
                }else if((ax-AX > -30.0f) && (ax-AX < -6.0f) ){
                    statusView.setText("tilt down");
                    Toast.makeText(MainActivity.this,"tilt down", Toast.LENGTH_SHORT).show();
                }else{
                    statusView.setText("Original");
                }

            }
        });

//        Button button = (Button) findViewById(R.id.back);
//        //button1.setClickable(false);
//
//        button.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MainActivity.this, TiltActivity.class);
//                startActivity(intent);
//            }
//        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        orientation.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientation.unregister();
    }
}