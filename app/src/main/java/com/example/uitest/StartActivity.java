package com.example.uitest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class StartActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        TextView prompt = (TextView) findViewById(R.id.prompt);

        Button eye_btn = (Button) findViewById(R.id.eye_tracking);
        final Button tilt_btn = (Button) findViewById(R.id.tilt);
        //eye_btn.setClickable(false);

        eye_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartActivity.this, EyeActivity.class);
                startActivity(intent);
            }
        });

        tilt_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartActivity.this, TiltActivity.class);
                intent.putExtra("playgame",2);

                startActivity(intent);
            }
        });

    }
}