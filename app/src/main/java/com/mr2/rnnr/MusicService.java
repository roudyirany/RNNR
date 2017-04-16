package com.mr2.rnnr;

import android.app.IntentService;
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
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
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

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
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
    public static final String PLAY_SONG = "Play song.";
    public static final String PAUSE_SONG = "Pause song.";
    public static final String UPDATE_MODEL = "Update model";

    MediaPlayer mediaPlayer;
    CountDownTimer mediaCountDown;
    NotificationCompat.Builder builder;
    NotificationManager nManager;
    AudioManager am;
    AudioManager.OnAudioFocusChangeListener afChangeListener;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    Integer previousCluster = null;
    Integer currentCluster;
    Integer tempC;
    Integer tempB;
    Integer previousBPM = null;
    Integer currentBPM;

    ArrayList<Double> speeds;
    ArrayList<Integer> rewards;
    double targetSpeed;
    int songsPlayed = 0;
    int songsLoaded = 0;
    String nextPath;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        speeds = new ArrayList<Double>();
        rewards = new ArrayList<Integer>();

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid());

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
        intentFilter.addAction(PLAY_SONG);
        intentFilter.addAction(PAUSE_SONG);
        intentFilter.addAction(UPDATE_MODEL);
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

                speeds.add(speed);
                String dataString = Double.toString(speed) + " Km/h";
                Intent RTReturn = new Intent(MainMenu.RECEIVE_DATA);
                RTReturn.putExtra("data", dataString);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                stepCounter = 0;
                this.start();
            }
        }.start();

        new planAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR);

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
        public void onReceive(final Context context, Intent intent) {
            if (intent.getAction().equals(RECEIVE_PATH)) {
                String path = intent.getStringExtra("path");

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(path);

                byte[] art = retriever.getEmbeddedPicture();
                String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

                Intent RTReturn = new Intent(MainMenu.RECEIVE_SONG);
                RTReturn.putExtra("art", art);
                RTReturn.putExtra("artist", artist);
                RTReturn.putExtra("title", title);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                builder.setContentText(artist).setContentTitle(title);
                nManager.notify(12345, builder.build());

                if(mediaPlayer != null && mediaPlayer.isPlaying())
                    mediaPlayer.stop();

                am.abandonAudioFocus(afChangeListener);
                mediaPlayer = MediaPlayer.create(MusicService.this, Uri.parse(path));
                RTReturn = new Intent(MusicService.PLAY_SONG);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                new planAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR);
            } else if (intent.getAction().equals(PLAY_SONG)) {
                am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                afChangeListener =  new AudioManager.OnAudioFocusChangeListener() {
                    public void onAudioFocusChange(int focusChange) {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            // Permanent loss of audio focus
                            // Pause playback immediately
                            mediaPlayer.pause();
                            Intent RTReturn = new Intent(MainMenu.FORCED_PAUSE);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                        }
                    }
                };;

                int result = am.requestAudioFocus(afChangeListener,
                        // Use the music stream.
                        AudioManager.STREAM_MUSIC,
                        // Request permanent focus.
                        AudioManager.AUDIOFOCUS_GAIN);

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mediaPlayer.start();
                    final Context context1 = context;
                    mediaCountDown = new CountDownTimer(500, 500) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            int position = mediaPlayer.getCurrentPosition();
                            int duration = mediaPlayer.getDuration();
                            int progress = ((100 * position) / duration+1);

                            Intent RTReturn = new Intent(MainMenu.RECEIVE_TRACK_PROGRESS);
                            RTReturn.putExtra("progress", progress);
                            LocalBroadcastManager.getInstance(context1).sendBroadcast(RTReturn);

                            mediaCountDown.start();
                        }
                    };

                    mediaCountDown.start();
                }
            } else if (intent.getAction().equals(PAUSE_SONG)) {
                mediaPlayer.pause();
                am.abandonAudioFocus(afChangeListener);
                mediaCountDown.cancel();
            }
            //Update model
            else if(intent.getAction().equals(UPDATE_MODEL)){

                String likeStatus = intent.getStringExtra("likeStatus");
                int reward;

                if(likeStatus.equals("unlike"))
                    reward = 10;
                else
                    reward = 0;

                double averageSpeed = 0;
                for(int i=0; i<speeds.size();i++){
                    averageSpeed = averageSpeed + speeds.get(i);
                }
                averageSpeed = averageSpeed / ((double)speeds.size());
                speeds = new ArrayList<Double>();

                double currentTarget;
                if(songsPlayed < 4)
                    currentTarget = (targetSpeed/(double)4)*(double)(songsPlayed+1);
                else
                    currentTarget = targetSpeed;

                reward = reward + ((int)(averageSpeed - currentTarget));
                rewards.add(reward);

                double rewardAverage = 0;
                for(int i=0; i<rewards.size();i++){
                    rewardAverage = rewardAverage + rewards.get(i);
                }
                rewardAverage = rewardAverage / ((double)rewards.size());
                final double rewardIncr = Math.log((double)reward/rewardAverage);

                mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        double WsC,WsB,WtC, WtB;
                        double RsC,RsB, RtC, RtB, weightTC, weightTB;

                        RsC = (double) dataSnapshot.child("clusters").child(Integer.toString(currentCluster)).child("weight").getValue();
                        RsB = (double) dataSnapshot.child("bpms").child(Integer.toString(currentBPM)).child("weight").getValue();


                        if(previousCluster == null){
                            WsC = WsB = 1;
                            WtC = WtB = 0;
                        }

                        else{
                            RtC = (double) dataSnapshot.child("transitions").child("clusters").child(previousCluster+"-"+currentCluster).child("weight").getValue();
                            RtB = (double) dataSnapshot.child("transitions").child("bpms").child(previousBPM+"-"+currentBPM).child("weight").getValue();
                            WsC = RsC/(RsC + RtC);
                            WsB = RsB/(RsB + RtB);
                            WtC = RsC/(RsC + RtC);
                            WtB = RtB/(RsB + RtB);
                            weightTC = ((double)(songsPlayed/(songsPlayed+1))*RtC + ((double)(1/(songsPlayed+1))*WtC*rewardIncr));
                            weightTB = ((double)(songsPlayed/(songsPlayed+1))*RtB + ((double)(1/(songsPlayed+1))*WtB*rewardIncr));
                            mFirebaseDatabaseReference.child("transitions").child("clusters").child(previousCluster+"-"+currentCluster).child("weight").setValue(weightTC);
                            mFirebaseDatabaseReference.child("transitions").child("bpms").child(previousBPM+"-"+currentBPM).child("weight").setValue(weightTB);
                        }

                        double weightC = (((double)songsPlayed+1.0)/((double)songsPlayed+2.0))*RsC + ((1.0/((double)songsPlayed+2.0))*WsC*rewardIncr);
                        double weightB = (((double)songsPlayed+1.0)/((double)songsPlayed+2.0))*RsB + ((1.0/((double)songsPlayed+2.0))*WsB*rewardIncr);
                        Log.d("new","weightC: "+weightC);

                        mFirebaseDatabaseReference.child("clusters").child(Integer.toString(currentCluster)).child("weight").setValue(weightC);
                        mFirebaseDatabaseReference.child("bpms").child(Integer.toString(currentBPM)).child("weight").setValue(weightB);

                        previousCluster = currentCluster;
                        previousBPM = currentBPM;
                        currentCluster = tempC;
                        currentBPM = tempB;

                        songsPlayed++;
                        Intent RTReturn = new Intent(MusicService.RECEIVE_PATH);
                        RTReturn.putExtra("path", nextPath);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        }
    };

    //Playlist planning in the background
    class planAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            songsLoaded++;

            mFirebaseDatabaseReference.child("targetSpeed").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    targetSpeed = (double) dataSnapshot.getValue();

                    double currentTarget;
                    if(songsPlayed < 4)
                        currentTarget = (targetSpeed/(double)4)*(double)(songsPlayed+1);
                    else
                        currentTarget = targetSpeed;

                    Intent RTReturn = new Intent(MainMenu.TARGET_SPEED);
                    RTReturn.putExtra("targetSpeed",currentTarget);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<Integer> BestTrajectory = new ArrayList<Integer>();
                    int upperMedian = ((int) (long) dataSnapshot.child("clusters").getChildrenCount()) / 2;
                    ArrayList<Double> RsValues = new ArrayList<Double>();
                    ArrayList<Integer> upperMedianClusters = new ArrayList<Integer>();
                    double maxPayoff = 0.0;

                    for (DataSnapshot postSnapshot : dataSnapshot.child("clusters").getChildren()) {
                        double weightC = (double) postSnapshot.child("weight").getValue();
                        int bpm = quantizeBPM((int) (long) dataSnapshot.child("library").child(postSnapshot.child("parent").getValue().toString()).child("bpm").getValue());
                        double weightB = (double) dataSnapshot.child("bpms").child(Integer.toString(bpm)).child("weight").getValue();
                        double Rs = weightC + weightB;

                        int position = RsValues.size() - 1;
                        if (position == -1)
                            position = 0;


                        for (int i = 0; i < RsValues.size(); i++) {
                            if (RsValues.get(i) < Rs)
                                position = i;
                        }

                        if (position < upperMedian && ((RsValues.size() + 1) < upperMedian)) {
                            RsValues.add(position, Rs);
                            upperMedianClusters.add(position, Integer.parseInt(postSnapshot.getKey().toString()));
                        }
                    }


                    for (int i = 0; i < (upperMedian * upperMedian * upperMedian); i++) {
                        ArrayList<Integer> Trajectory = new ArrayList<Integer>();
                        ArrayList<Integer> BPMs = new ArrayList<Integer>();
                        double expectedPayoff = 0.0;
                        for (int j = 0; j < 5; j++) {

                            Random rand = new Random();
                            int n = rand.nextInt(upperMedianClusters.size() - 1);
                            int cluster = upperMedianClusters.get(n);

                            while (Trajectory.contains(cluster)) {
                                n = rand.nextInt(upperMedianClusters.size() - 1);
                                cluster = upperMedianClusters.get(n);
                            }

                            Trajectory.add(cluster);

                            int bpm = ((int) (long) dataSnapshot.child("library").child(dataSnapshot.child("clusters").child(Integer.toString(cluster)).child("parent").getValue().toString()).child("bpm").getValue());

                            BPMs.add(quantizeBPM(bpm));
                        }

                        for (int k = 0; k < 3; k++) {
                            expectedPayoff = expectedPayoff + RsValues.get(upperMedianClusters.indexOf(Trajectory.get(k)));
                        }

                        double RtC, RtB;
                        if (previousCluster != null) {
                            RtC = ((double) dataSnapshot.child("transitions").child("clusters").child(previousCluster + "-" + Trajectory.get(0)).getValue()) + ((double) dataSnapshot.child("transitions").child("clusters").child(Trajectory.get(0) + "-" + Trajectory.get(1)).getValue()) + ((double) dataSnapshot.child("transitions").child("clusters").child(Trajectory.get(1) + "-" + Trajectory.get(2)).getValue());
                            RtB = ((double) dataSnapshot.child("transitions").child("bpms").child(previousBPM + "-" + BPMs.get(0)).getValue()) + ((double) dataSnapshot.child("transitions").child("bpms").child(BPMs.get(0) + "-" + BPMs.get(1)).getValue()) + ((double) dataSnapshot.child("transitions").child("bpms").child(BPMs.get(1) + "-" + BPMs.get(2)).getValue());
                        } else {
                            RtC = ((double) dataSnapshot.child("transitions").child("clusters").child(Trajectory.get(0) + "-" + Trajectory.get(1)).getValue()) + ((double) dataSnapshot.child("transitions").child("clusters").child(Trajectory.get(1) + "-" + Trajectory.get(2)).getValue());
                            RtB = ((double) dataSnapshot.child("transitions").child("bpms").child(BPMs.get(0) + "-" + BPMs.get(1)).getValue()) + ((double) dataSnapshot.child("transitions").child("bpms").child(BPMs.get(1) + "-" + BPMs.get(2)).getValue());
                        }

                        expectedPayoff = expectedPayoff + RtC + RtB;

                        if (expectedPayoff > maxPayoff) {
                            maxPayoff = expectedPayoff;
                            BestTrajectory = new ArrayList<Integer>(Trajectory);
                        }

                    }

                    randomSong(BestTrajectory.get(0));

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

                //Select random 3 songs
                public void randomSong(final int cluster) {
                    mFirebaseDatabaseReference.child("library").orderByChild("cluster").equalTo(cluster).addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            int count = (int) (long) dataSnapshot.getChildrenCount();
                            int n;
                            if (count > 1) {
                                Random rand = new Random();
                                n = rand.nextInt(count - 1);
                            } else n = 0;

                            int i = 0;
                            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                if (i == n) {
                                    tempC = cluster;
                                    tempB = quantizeBPM((int) (long) postSnapshot.child("bpm").getValue());
                                    nextPath = postSnapshot.child("path").getValue().toString();
                                } else i++;
                            }

                            if(songsLoaded<2){
                                currentCluster = tempC;
                                currentBPM = tempB;

                                Intent RTReturn = new Intent(MusicService.RECEIVE_PATH);
                                RTReturn.putExtra("path", nextPath);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }

                //Quantize bpm
                public int quantizeBPM(int i) {
                    if (i < 50)
                        return 0;
                    else if (i < 55)
                        return 1;
                    else if (i < 60)
                        return 2;
                    else if (i < 70)
                        return 3;
                    else if (i < 85)
                        return 4;
                    else if (i < 100)
                        return 5;
                    else if (i < 115)
                        return 6;
                    else if (i < 140)
                        return 7;
                    else if (i < 150)
                        return 8;
                    else if (i < 170)
                        return 9;
                    else
                        return 10;
                }

            });

            return null;
        }
    }

}
