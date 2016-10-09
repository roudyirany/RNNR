package com.mr2.rnnr;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class Initialization extends AppCompatActivity {
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialization);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, Sign_In.class));
            finish();
            return;
        }

        //Fade in, fade out animation
        final TextView TextView3 = (TextView)findViewById(R.id.textView3);
        final Animation fadein = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade);
        final Animation fadeout = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fadeout);
        TextView3.startAnimation(fadein);

        fadein.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                TextView3.startAnimation(fadeout);
            }
        });

        fadeout.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                TextView3.startAnimation(fadein);
            }
        });

    }

    @Override
    public void onStart(){
        super.onStart();


        //Progress bar
        final ProgressBar Progress = (ProgressBar) findViewById (R.id.progressBar);
        Progress.setProgress(0);
        Progress.setMax(200);

        int bpm = (int)AnalyzeBPM("src/main/assets/photomaton.mp3");
        Progress.setProgress(bpm);

    }

    static{
        System.loadLibrary("bpm_analyzer");
    }

    public native float AnalyzeBPM(String songPath);
}
