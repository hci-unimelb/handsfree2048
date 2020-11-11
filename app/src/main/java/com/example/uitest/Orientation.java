package com.example.uitest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Orientation {
    public interface Listener{
        void onTranslation(float tz, float tx, float ty);
    }

    private Listener listener;

    public void setListener(Listener l){
        listener = l;
    }

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magneticField;
    private SensorEventListener sensorEventListener;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];


    Orientation(Context context){
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
                }else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0 , magnetometerReading, 0, magnetometerReading.length);
                }


                SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                if(listener != null)
                    listener.onTranslation((float) Math.toDegrees(orientationAngles[0]), (float) Math.toDegrees(orientationAngles[1]),
                            (float) Math.toDegrees(orientationAngles[2]));          // orientationAngles[0] around z azimuth [1] around x pitch，[2]around y roll

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    public void register(){
        sensorManager.registerListener(sensorEventListener, accelerometer,sensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magneticField,sensorManager.SENSOR_DELAY_UI);
    }

    public void unregister(){
        sensorManager.unregisterListener(sensorEventListener);
    }
}
