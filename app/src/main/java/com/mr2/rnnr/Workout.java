package com.mr2.rnnr;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class Workout extends AppCompatActivity implements SensorEventListener {
    private SensorManager senSensorManager;
    private Sensor StepCounter;
    private int stepCounter = 0;
    private int counterSteps = 0;
    private long lastUpdate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        StepCounter = senSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        senSensorManager.registerListener(this, StepCounter , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if(mySensor.getType() == Sensor.TYPE_STEP_COUNTER){

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                if (counterSteps < 1)
                    counterSteps = (int)event.values[0];
                else
                    stepCounter = (int)event.values[0] - counterSteps;

                /*double distanceM = Math.round(stepCounter*0.762);
                double velocityMS = distanceM/(diffTime/1000);
                double velocityKMH = velocityMS*3.6;*/

                TextView velocity = (TextView) findViewById(R.id.velocity);
                velocity.setText(Integer.toString(counterSteps) + "km/h");
            }

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
