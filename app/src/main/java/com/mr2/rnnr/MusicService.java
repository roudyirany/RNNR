package com.mr2.rnnr;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

/**
 * Created by roudyirany on 3/13/17.
 */

public class MusicService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    public GoogleApiClient mApiClient;
    private SensorManager senSensorManager;
    private Sensor StepCounter;
    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;
    private double stepCounter = 0;

    PowerManager powerManager;
    PowerManager.WakeLock wakelock;
    String activityString = "Walking";
    boolean activityStarted = false;

    ArrayList<Integer> BestTrajectory;
    Context context;

    LocalBroadcastManager bManager;
    public static final String RECEIVE_PATH = "Path received.";

    MediaPlayer mediaPlayer;
    NotificationCompat.Builder builder;
    NotificationManager nManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        BestTrajectory = new ArrayList<Integer>();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.STRING_ACTION));


        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "wake lock");
        wakelock.acquire();

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        StepCounter = senSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        senSensorManager.registerListener(this, StepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_PATH);
        bManager.registerReceiver(bReceiver, intentFilter);

        //Notification
        builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("RNNR")
                        .setContentText("Monitoring on.")
                        .setOngoing(true);
        int NOTIFICATION_ID = 12345;

        Intent targetIntent = new Intent(this, MainMenu.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(NOTIFICATION_ID, builder.build());


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        context = this;

        //Activity detection
        new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                if (!activityStarted) {
                    requestActivityUpdates();
                    activityStarted = true;
                }

                double distance = stepCounter;
                double speed = (distance / 1000.0) / (30.0 / 3600.0);
                DecimalFormat df = new DecimalFormat("####0.0");
                speed = Double.valueOf(df.format(speed));

                String dataString = Double.toString(speed) + " Km/h";
                Intent RTReturn = new Intent(MainMenu.RECEIVE_DATA);
                RTReturn.putExtra("data", dataString);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                stepCounter = 0;
                this.start();
            }
        }.start();

        startService(new Intent(this, PlanningIntentService.class));

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(12345);
        bManager.unregisterReceiver(bReceiver);
        stopSelf();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (activityString.equals("Walking"))
                stepCounter = stepCounter + 0.726;
            else if (activityString.equals("Running"))
                stepCounter = stepCounter + 0.826;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //Activity Recognition
    public String getDetectedActivity(int detectedActivityType) {
        Resources resources = this.getResources();
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DetectedActivity detectedActivity = intent.getParcelableExtra(Constants.STRING_EXTRA);
            activityString = getDetectedActivity(detectedActivity.getType());
        }
    }

    public void requestActivityUpdates() {
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 0, getActivityDetectionPendingIntent()).setResultCallback(this);
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, ActivityRecognizedService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.e("status: ", "Successfully added activity detection.");

        } else {
            Log.e("status: ", "Error: " + status.getStatusMessage());
        }
    }

    //Receive current song path
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(RECEIVE_PATH)) {
                String path = intent.getStringExtra("path");

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(path);

                byte[] art = retriever.getEmbeddedPicture();
                String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

                Intent RTReturn = new Intent(MainMenu.RECEIVE_SONG);
                RTReturn.putExtra("art", art);
                RTReturn.putExtra("artist",artist);
                RTReturn.putExtra("title", title);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                builder.setContentText(title+" - "+artist);
                nManager.notify(12345,builder.build());

                mediaPlayer = MediaPlayer.create(MusicService.this, Uri.parse(path));
                mediaPlayer.start();
            }
        }
    };

}
