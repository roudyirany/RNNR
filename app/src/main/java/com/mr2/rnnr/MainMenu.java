package com.mr2.rnnr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    LocalBroadcastManager bManager;
    TextView speed;
    ProgressBar progressBar;
    RelativeLayout workoutBar;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        //Hide workout bar
        workoutBar = (RelativeLayout) findViewById(R.id.activity_workout);
        workoutBar.setVisibility(View.INVISIBLE);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/targetSpeed");

        final TextView targetSpeed = (TextView) findViewById(R.id.textView7);
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                targetSpeed.setText(dataSnapshot.getValue().toString()+" Km/h");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        speed = (TextView) findViewById(R.id.textView9);

        bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECEIVE_DATA);
        intentFilter.addAction(RECEIVE_PROGRESS);
        bManager.registerReceiver(bReceiver, intentFilter);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        startService(new Intent(this, MusicService.class));

        progressBar = (ProgressBar) findViewById(R.id.workoutProgress);
        progressBar.setMax(100);
        progressBar.setProgress(0);
    }

    @Override
    public void onDestroy(){
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

    //Your activity will respond to this action String
    public static final String RECEIVE_DATA = "Data received.";
    public static final String RECEIVE_PROGRESS = "Progress received.";

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(RECEIVE_DATA)) {
                String serviceDataString = intent.getStringExtra("data");
                speed.setText(serviceDataString);
            }

            else if (intent.getAction().equals(RECEIVE_PROGRESS)){
                int Progress = intent.getIntExtra("progress",0);
                progressBar.setProgress(Progress);
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

