package com.mr2.rnnr;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends AppCompatActivity {

    public static final String RECEIVE_DATA = "Data received.";
    public static final String RECEIVE_PROGRESS = "Progress received.";
    public static final String RECEIVE_TRACK_PROGRESS = "Track progress received.";
    public static final String RECEIVE_SONG = "Song received. ";
    public static final String FORCED_PAUSE = "Audiofocus loss.";
    public static final String TARGET_SPEED = "Target speed. ";
    public static final String DISABLE_SKIP = "Disable skip. ";
    public static final String RESTART = "Disable";
    public static final String ENABLE_SKIP = "Enable skip. ";
    public static final String SET_LIKE = "Set like. ";
    private LocalBroadcastManager bManager;
    private TextView speed;
    private TextView titleT;
    private TextView artistT;
    private ImageView artwork;
    private ImageView like;
    private ImageView skip;
    private ImageView playpause;
    private Button startWorkout;
    private ProgressBar progressBar;
    private ProgressBar trackProgressBar;
    private RelativeLayout workoutBar;
    private Context context;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RECEIVE_DATA)) {
                String serviceDataString = intent.getStringExtra("data");
                speed.setText(serviceDataString);
            } else if (intent.getAction().equals(RECEIVE_PROGRESS)) {
                int Progress = intent.getIntExtra("progress", 0);
                progressBar.setProgress(Progress);
            } else if (intent.getAction().equals(RECEIVE_SONG)) {
                byte[] art = intent.getByteArrayExtra("art");
                String artist = intent.getStringExtra("artist");
                String title = intent.getStringExtra("title");

                if (art != null) {
                    artwork.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length));
                } else {
                    artwork.setImageResource(R.drawable.unknown_track);
                }

                if (artist == null)
                    artist = "Unknown Artist";
                if (title == null)
                    title = "Unknown Track";

                if (artist.length() <= 30)
                    artistT.setText(artist);
                else
                    artistT.setText(artist.substring(0, 30) + "...");

                if (title.length() <= 30)
                    titleT.setText(title);
                else
                    titleT.setText(title.substring(0, 30) + "...");
            } else if (intent.getAction().equals(RECEIVE_TRACK_PROGRESS)) {
                int progress = intent.getIntExtra("progress", 0);
                trackProgressBar.setProgress(progress);

                if (progress == 100) {
                    Intent RTReturn = new Intent(MusicService.UPDATE_MODEL);
                    RTReturn.putExtra("likeStatus", String.valueOf(like.getTag()));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);
                    like.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                    like.setTag("like");

                    if(startWorkout.getText().toString().equals("Cooling down...")) {
                        startWorkout.setEnabled(true);
                        startWorkout.setText("Start Workout");
                    }
                }
            } else if (intent.getAction().equals(FORCED_PAUSE)) {
                playpause.setImageResource(android.R.drawable.ic_media_play);
                playpause.setTag("play");
            } else if (intent.getAction().equals(TARGET_SPEED)) {
                TextView targetSpeed = (TextView) findViewById(R.id.textView7);
                Double target = intent.getDoubleExtra("targetSpeed", 0.0);
                targetSpeed.setText(target + " Km/h");
            } else if (intent.getAction().equals(DISABLE_SKIP)) {
                skip.setEnabled(false);
            } else if (intent.getAction().equals(ENABLE_SKIP)) {
                skip.setEnabled(true);
            } else if (intent.getAction().equals(SET_LIKE)) {
                like.setColorFilter(Color.parseColor("#ffff8800"), PorterDuff.Mode.SRC_IN);
                like.setTag("unlike");
            }
            else if(intent.getAction().equals(RESTART)){
                Intent intent1 = new Intent(MainMenu.this,MainMenu.class);
                startActivity(intent1);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        context = this;

        //Hide workout bar
        workoutBar = (RelativeLayout) findViewById(R.id.activity_workout);

        titleT = (TextView) findViewById(R.id.title);
        artistT = (TextView) findViewById(R.id.artist);
        artwork = (ImageView) findViewById(R.id.artwork);

        speed = (TextView) findViewById(R.id.textView9);

        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_DATA);
        intentFilter.addAction(RECEIVE_PROGRESS);
        intentFilter.addAction(RECEIVE_SONG);
        intentFilter.addAction(RECEIVE_TRACK_PROGRESS);
        intentFilter.addAction(FORCED_PAUSE);
        intentFilter.addAction(TARGET_SPEED);
        intentFilter.addAction(DISABLE_SKIP);
        intentFilter.addAction(ENABLE_SKIP);
        intentFilter.addAction(SET_LIKE);
        bManager.registerReceiver(bReceiver, intentFilter);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        startService(new Intent(this, MusicService.class));

        progressBar = (ProgressBar) findViewById(R.id.workoutProgress);
        progressBar.setMax(100);
        progressBar.setProgress(100);

        trackProgressBar = (ProgressBar) findViewById(R.id.songProgress);
        trackProgressBar.setMax(100);
        trackProgressBar.setProgress(0);

        //If song is playing, pause button should be displayed and vice versa.
        playpause = (ImageView) findViewById(R.id.playpause);
        playpause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (String.valueOf(playpause.getTag()).equals("play")) {
                    Intent RTReturn = new Intent(MusicService.PLAY_SONG);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                    playpause.setImageResource(android.R.drawable.ic_media_pause);
                    playpause.setTag("pause");

                } else {
                    Intent RTReturn = new Intent(MusicService.PAUSE_SONG);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                    playpause.setImageResource(android.R.drawable.ic_media_play);
                    playpause.setTag("play");
                }
            }
        });

        like = (ImageView) findViewById(R.id.likeMain);
        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (String.valueOf(like.getTag()).equals("like")) {
                    like.setColorFilter(Color.parseColor("#ffff8800"), PorterDuff.Mode.SRC_IN);
                    like.setTag("unlike");
                } else {
                    like.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                    like.setTag("like");
                }
            }
        });

        skip = (ImageView) findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent RTReturn = new Intent(MusicService.UPDATE_MODEL);
                Log.d("likeStatus", String.valueOf(like.getTag()));
                RTReturn.putExtra("likeStatus", String.valueOf(like.getTag()));
                RTReturn.putExtra("skipped", true);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);
                like.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                like.setTag("like");
            }
        });

        startWorkout = (Button) findViewById(R.id.startWorkout);
        startWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (String.valueOf(startWorkout.getTag()).equals("start")) {

                    if(!isMyServiceRunning(MusicService.class)){
                        startService(new Intent(context,MusicService.class));
                        Toast.makeText(context, "Please wait while RNNR initializes.",
                                Toast.LENGTH_LONG).show();

                        while(!isMyServiceRunning(MusicService.class)){}

                        Intent RTReturn = new Intent(MusicService.PLAY_SONG);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                        playpause.setImageResource(android.R.drawable.ic_media_pause);
                        playpause.setTag("pause");

                        startWorkout.setTag("stop");
                        startWorkout.setText("Cooldown & Stop Workout");

                        skip.setClickable(true);
                        like.setClickable(true);
                        playpause.setClickable(true);
                    }

                    Intent RTReturn = new Intent(MusicService.PLAY_SONG);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                    playpause.setImageResource(android.R.drawable.ic_media_pause);
                    playpause.setTag("pause");

                    startWorkout.setTag("stop");
                    startWorkout.setText("Cooldown & Stop Workout");

                    skip.setClickable(true);
                    like.setClickable(true);
                    playpause.setClickable(true);
                } else {
                    Intent RTReturn = new Intent(MusicService.COOLDOWN);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);

                    playpause.setImageResource(android.R.drawable.ic_media_pause);
                    playpause.setTag("pause");

                    startWorkout.setTag("start");
                    startWorkout.setText("Cooling down...");

                    startWorkout.setEnabled(false);
                    skip.setClickable(false);
                }
            }
        });

        //Disable buttons before starting workout
        skip.setClickable(false);
        like.setClickable(false);
        playpause.setClickable(false);

        Toast.makeText(MainMenu.this, "Please wait while RNNR initializes...",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bManager.unregisterReceiver(bReceiver);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new homescreen(), "HOME");
        adapter.addFragment(new history(), "HISTORY");
        adapter.addFragment(new settings(), "SETTINGS");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

