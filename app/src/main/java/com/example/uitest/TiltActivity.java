package com.example.uitest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.prefs.BackingStoreException;

/**
 * @Description This activity moves the numbered tiles according to the result of tilt gesture recognition
 * @reference Part of the 2048 game code comes from https://www.youtube.com/watch?v=6ojV--thA5c, which will be specified in the code.
 */

public class TiltActivity extends AppCompatActivity {

    private final static String TAG = "TiltActivity";
    private ImageView box11, box12, box13, box14;
    private ImageView box21, box22, box23, box24;
    private ImageView box31, box32, box33, box34;
    private ImageView box41, box42, box43, box44;

    private TextView text11, text12, text13, text14;
    private TextView text21, text22, text23, text24;
    private TextView text31, text32, text33, text34;
    private TextView text41, text42, text43, text44;
    private LinearLayout layout;

    Button back, retreat;
    private int matrix[][], saveMatrix[][];
    private TiltDatabase database;

    private Orientation orientation;
    private int count = 0;
    private float AZ, AX, AY;
    private TextView rotationView;
    private TextView statusView;
    private int left_n, right_n, up_n, down_n, back_n, retreat_n, origin_n = 0;

    private int random_i;
    private int random_j;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_tilt);
        initView();

        //newGame();
        keepPlay();

        Toast toast = Toast.makeText(getApplicationContext(), "In tilt mode, you can control the numbered tiles by tilting your phone.", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView tv = (TextView) layout.getChildAt(0);
        tv.setTextSize(20);
        toast.show();

        final Toast toast1 = Toast.makeText(getApplicationContext(), "Remember to return to the original position after each move.", Toast.LENGTH_LONG);
        toast1.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout layout1 = (LinearLayout) toast1.getView();
        TextView tv1 = (TextView) layout1.getChildAt(0);
        tv1.setTextSize(20);

        showToast(toast1,4500);

        final Toast toast2 = Toast.makeText(getApplicationContext(), "Now please hold your phone, the game will record your initial position", Toast.LENGTH_LONG);
        toast2.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout layout2 = (LinearLayout) toast2.getView();
        TextView tv2 = (TextView) layout2.getChildAt(0);
        tv2.setTextSize(20);

        showToast(toast2,9000);


        final Toast toast3 = Toast.makeText(getApplicationContext(), "Now you can play the game!", Toast.LENGTH_SHORT);
        toast3.setGravity(Gravity.CENTER, 0, 0);
        LinearLayout layout3 = (LinearLayout) toast3.getView();
        TextView tv3 = (TextView) layout3.getChildAt(0);
        tv3.setTextSize(20);

        showToast(toast3,13000);

        orientation = new Orientation(this);
//        rotationView = (TextView) findViewById(R.id.prompt);
        statusView = (TextView) findViewById(R.id.status);
        statusView.setText(" ");

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                direction();
            }
        },13000);
    }
    

    public void showToast(final Toast toast, int milliseconds){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                toast.show();
            }
        },milliseconds);
    }

    public void direction() {

        orientation.setListener(new Orientation.Listener() {

            @Override
            public void onTranslation(float az, float ax, float ay) {
                boolean check = false;
                float az_diff = az-AZ;
                float ax_diff = ax-AX;
                float ay_diff = ay-AY;
                if (az_diff > 300.0f) az_diff = az_diff - 360;
                if (az_diff < -300.0f) az_diff = az_diff + 360;

                count++;
                if (count == 1) {
                    AZ = az;
                    AX = ax;
                    AY = ay;
                }
                if (count > 80) {
                    count = 8;
                }

//                rotationView.setText("origin " + origin_n + "  left "+ left_n+"  retreat"+retreat_n + "\n" +" AZ  " + AZ + "  " + "AX  " + AX + "  " + "AY  " + AY + "\n" +
//                        "az  " + az + "\n" + "ax  " + ax + "\n" + "ay  " + ay + "\n" + "az_diff  "
//                        + az_diff + "\n" +"ax_diff  " + ax_diff + "\n" + "ay_diff  " + ay_diff);
                if ( az_diff < -25.0f ){ //az_diff > -40.0f && b
                    back_n++;
                    if (back_n == 8){
                        origin_n = 0;
                        back.setBackgroundColor(0xfff57c00);
                        statusView.setText("back");
                        Intent intent = new Intent(TiltActivity.this, StartActivity.class);
                        startActivity(intent);
                    }
                    if (back_n > 8)
                        statusView.setText("back");
                }
                else if ( az_diff > 25.0f ){ //&& az_diff < 40.0f && b
                    retreat_n++;
                    if (retreat_n == 8){
                        retreat.setBackgroundColor(0xfff57c00);
                        retreat();
                        right_n = back_n = origin_n = 0;
                        statusView.setText("retreat");
                    }
                    if (retreat_n > 8)
                        statusView.setText("retreat");
                }
                else if ( ay_diff < -20.0f ) { //&& az_diff > -10.0f
                    left_n++;
                    if (left_n == 3){
                        setSaveMatrix();
                        leftFunc(check);
                        //right_n = up_n = down_n = 0;
                        back_n = retreat_n = origin_n = 0;
                        statusView.setText("left");
                    }
                    if (left_n > 3)
                        statusView.setText("left");
                }
                else if ( ay_diff > 20.0f ) {  //&& az_diff < 10.0f
                    right_n++;
                    if (right_n == 3) {
                        setSaveMatrix();
                        rightFunc(check);
                        //left_n = up_n = down_n = 0;
                        back_n = retreat_n = origin_n = 0;
                        statusView.setText("right");
                    }
                    if (right_n > 3)
                        statusView.setText("right");
                }
                else if (ax_diff > 20.0f) {
                    up_n++;
                    if (up_n == 3){
                        setSaveMatrix();
                        //left_n = right_n = down_n = 0;
                        back_n = retreat_n = origin_n = 0;
                        upFunc(check);
                        statusView.setText("up");
                    }
                    if (up_n > 3)
                        statusView.setText("up");
                }
                else if (ax_diff < -16.0f) {  //(ax_diff > -45.0f)
                    down_n++;
                    if (down_n == 3) {
                        setSaveMatrix();
                        //left_n = right_n = up_n = 0;
                        back_n = retreat_n = origin_n = 0;
                        downFunc(check);
                        statusView.setText("down");
                    }
                    if (down_n > 3 )
                        statusView.setText("down");
                }else{
                    origin_n++;
                    if (origin_n > 4)
                        left_n = right_n = up_n = down_n = back_n = retreat_n = 0;

                    if( Math.abs(az_diff) < 10.0f && Math.abs(ax_diff) < 7.0f && Math.abs(ay_diff) < 7.0f) {
                        if (origin_n % 20 == 0) {    //Calibration, there will be an offset in the process of playing
                            AZ = az;
                            AX = ax;
                            AY = ay;
                        }
                    }
                    statusView.setText("origin");
                    back.setBackgroundColor(0xffffa726);
                    retreat.setBackgroundColor(0xffffa726);
                }
            }
        });
    }

    /*
    The leftFunc, rightFunc, upFunc and downFunc come from the url, and they were combined with tilt gesture recognition
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
                            matrix[i][j]+=matrix[i][k];
                            matrix[i][k]=0;j=k;break;
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
                            matrix[i][j]=matrix[i][k];
                            matrix[i][k]=0; break;
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

    @Override
    public void onStop() {
        saveProcess();
        super.onStop();
        orientation.unregister();
    }

    /*
    the start of the copied code from the url
     */
    public void saveProcess() {
        database = new TiltDatabase(this);
        database.setProcess(matrix);
        database.close();
    }

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
            return;
        }

        for (int i = 1; i < 25; i++)
            matrix[i / 5][i % 5] = saveMatrix[i / 5][i % 5];
        setBox();
    }

    public void keepPlay() {
        database = new TiltDatabase(this);
        matrix = database.getProcess(this);
        database.close();
        setSaveMatrix();
        setBox();
    }

    public void newGame() {
        database = new TiltDatabase(this);
        database.close();
        for (int i = 0; i < 25; i++) matrix[i / 5][i % 5] = 0;
        randomNumber();   //initialize two random numbers at begin
        randomNumber();
        setSaveMatrix();
        setBox();
    }

    public void setSaveMatrix() {
        for (int i = 1; i < 25; i++)
            saveMatrix[i / 5][i % 5] = matrix[i / 5][i % 5];
    }

    public void initView() {
        matrix = new int[5][5];
        saveMatrix = new int[5][5];

        layout = (LinearLayout) findViewById(R.id.board);

        //block background
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

        //block number
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

        back = (Button) findViewById(R.id.back);
        retreat = (Button) findViewById(R.id.retreat);
        back.setClickable(false);
        retreat.setClickable(false);

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

    /*
    number exceeds 512 and becomes 25dp
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
    //the end of the copied code

    /*
    The original code obtained from the url was modified to generate animation for each new numbered tile
     */
    public void randomNumber() {
        Random random = new Random();
        while (true) {
            random_i= random.nextInt(4) + 1;
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

        if (check.equals("win")){
            Toast toast = Toast.makeText(getApplicationContext(), "You win!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            LinearLayout layout = (LinearLayout) toast.getView();
            TextView tv = (TextView) layout.getChildAt(0);
            tv.setTextSize(30);
            toast.show();
            newGame();
        }

        if (check.equals("game over")) {
            Toast toast = Toast.makeText(getApplicationContext(), "Game Over", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            LinearLayout layout = (LinearLayout) toast.getView();
            TextView tv = (TextView) layout.getChildAt(0);
            tv.setTextSize(30);
            toast.show();
            newGame();

        }
    }

}



