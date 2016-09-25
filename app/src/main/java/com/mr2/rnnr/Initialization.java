package com.mr2.rnnr;

import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Initialization extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialization);


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
        Progress.setMax(100);

        CountDownTimer mCountDownTimer=new CountDownTimer(12000,100) {
            int i =0;
            @Override
            public void onTick(long millisUntilFinished) {
                i++;
                Progress.setProgress(i);
            }

            @Override
            public void onFinish() {
                //Do what you want
                i++;
                Progress.setProgress(i);
            }
        };
        mCountDownTimer.start();

    }
}
