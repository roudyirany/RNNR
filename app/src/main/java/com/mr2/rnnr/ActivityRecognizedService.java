package com.mr2.rnnr;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by roudyirany on 2/12/17.
 */

public class ActivityRecognizedService extends IntentService {

    DetectedActivity previousActivity = new DetectedActivity(DetectedActivity.STILL, 100);

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent i = new Intent(Constants.STRING_ACTION);
        int confidence = -1;

        List<DetectedActivity> detectedActivities = result.getProbableActivities();

        for(DetectedActivity activity: detectedActivities){
            if(activity.getType() == DetectedActivity.RUNNING || activity.getType() == DetectedActivity.WALKING || activity.getType() == DetectedActivity.STILL){
                if(activity.getConfidence() > confidence){
                    confidence = activity.getConfidence();
                    i.putExtra(Constants.STRING_EXTRA, activity);
                    previousActivity = activity;
                }
            }

            else
                i.putExtra(Constants.STRING_EXTRA, previousActivity);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
}
