package com.mr2.rnnr;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Calibration extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Button startCalib;
    private int steps = 0;
    private TextView distance;
    private TextView activity;
    protected LocationManager locationManager;

    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; //in milliseconds

    double clat,clon,dis;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATES,
                MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
                new MyLocationListener()
        );

        startCalib = (Button) findViewById(R.id.startCalib);
        startCalib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if(location != null){
                    steps = 0;
                    dis = 0.0;
                    startCalib.setEnabled(false);

                    clat = location.getLatitude();
                    clon = location.getLongitude();
                }

                else{
                    Toast.makeText(Calibration.this, "Please turn on location services and go outside.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        distance = (TextView) findViewById(R.id.distance);
        activity = (TextView) findViewById(R.id.activityType);
    }

    @Override
    protected void onResume(){
        super.onResume();

        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause(){
        super.onPause();

        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_STEP_COUNTER)
            steps++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class MyLocationListener implements LocationListener {

        public void onLocationChanged(Location location) {
            if(!startCalib.isEnabled()) {
                dis = dis + getDistance(clat, clon, location.getLatitude(), location.getLongitude());
                clat = location.getLatitude();
                clon = location.getLongitude();
                distance.setText(Math.round(dis) + "/50m");

                if (dis >= 50.0) {
                    double stepSize = dis / (double) steps;
                    if (startCalib.getText().toString().equals("start walking")) {
                        mFirebaseDatabaseReference.child("walking").setValue(stepSize);
                        startCalib.setText("start running");
                        startCalib.setEnabled(true);
                        activity.setText("Running");
                        distance.setText("0/50m");
                    } else {
                        mFirebaseDatabaseReference.child("running").setValue(stepSize);
                        Intent intent = new Intent(Calibration.this, MainMenu.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        }

        public void onStatusChanged(String s, int i, Bundle b) {

        }

        public void onProviderDisabled(String s) {
            Toast.makeText(Calibration.this,
                    "Please enable location services, close the app and try again.",
                    Toast.LENGTH_LONG).show();
        }

        public void onProviderEnabled(String s) {
        }

    }

    public double getDistance(double lat1, double lon1, double lat2, double lon2) {

        double latA = Math.toRadians(lat1);
        double lonA = Math.toRadians(lon1);
        double latB = Math.toRadians(lat2);
        double lonB = Math.toRadians(lon2);
        double cosAng = (Math.cos(latA) * Math.cos(latB) * Math.cos(lonB-lonA)) +
                (Math.sin(latA) * Math.sin(latB));
        double ang = Math.acos(cosAng);
        double dist = ang *6371;
        return dist;

    }

}
