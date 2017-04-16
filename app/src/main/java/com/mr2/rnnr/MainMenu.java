package com.mr2.rnnr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.os.CountDownTimer;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.android.gms.ads.formats.NativeAd;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    LocalBroadcastManager bManager;
    TextView speed;
    TextView titleT;
    TextView artistT;
    ImageView artwork;
    ImageView like;
    ImageView playpause;
    ProgressBar progressBar;
    ProgressBar trackProgressBar;
    RelativeLayout workoutBar;
    Context context;

    public static final String RECEIVE_DATA = "Data received.";
    public static final String RECEIVE_PROGRESS = "Progress received.";
    public static final String RECEIVE_TRACK_PROGRESS = "Track progress received.";
    public static final String RECEIVE_SONG = "Song received. ";
    public static final String FORCED_PAUSE = "Audiofocus loss.";
    public static final String TARGET_SPEED = "Target speed. ";

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        context = this;

        //Hide workout bar
        workoutBar = (RelativeLayout) findViewById(R.id.activity_workout);
        //workoutBar.setVisibility(View.INVISIBLE);

        titleT = (TextView) findViewById(R.id.title);
        artistT = (TextView) findViewById(R.id.artist);
        artwork = (ImageView) findViewById(R.id.artwork);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/targetSpeed");

        speed = (TextView) findViewById(R.id.textView9);

        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_DATA);
        intentFilter.addAction(RECEIVE_PROGRESS);
        intentFilter.addAction(RECEIVE_SONG);
        intentFilter.addAction(RECEIVE_TRACK_PROGRESS);
        intentFilter.addAction(FORCED_PAUSE);
        intentFilter.addAction(TARGET_SPEED);
        bManager.registerReceiver(bReceiver, intentFilter);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        startService(new Intent(this, MusicService.class));

        progressBar = (ProgressBar) findViewById(R.id.workoutProgress);
        progressBar.setMax(100);
        progressBar.setProgress(0);

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
                    like.setColorFilter(Color.rgb(251, 152, 0), PorterDuff.Mode.SRC_IN);
                    like.setTag("unlike");
                } else {
                    like.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                    like.setTag("like");
                }
            }
        });
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
                }
            } else if (intent.getAction().equals(FORCED_PAUSE)) {
                playpause.setImageResource(android.R.drawable.ic_media_pause);
                playpause.setTag("pause");
            }
            else if(intent.getAction().equals(TARGET_SPEED)){
                TextView targetSpeed = (TextView) findViewById(R.id.textView7);
                Double target = intent.getDoubleExtra("targetSpeed",0.0);
                targetSpeed.setText(target +" Km/h");
            }
        }
    };

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
}

