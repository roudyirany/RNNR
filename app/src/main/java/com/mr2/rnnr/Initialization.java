package com.mr2.rnnr;

import android.content.Intent;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Initialization extends AppCompatActivity {
    ProgressBar Progress;
    private ArrayList<Song> songList;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialization);

        Progress = (ProgressBar) findViewById (R.id.progressBar);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, Sign_In.class));
            finish();
            return;
        }

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseAuth.getCurrentUser().getUid());

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
        songList = new ArrayList<Song>();
        getSongList();

        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        //Progress bar
        Progress.setProgress(0);
        Progress.setMax(100);
        new MyAsyncTask().execute(songList);

    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int dataColumn= musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisPath = musicCursor.getString(dataColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisPath));
            }
            while (musicCursor.moveToNext());
        }
    }

    //Background song processing thread
    class MyAsyncTask extends AsyncTask<ArrayList<Song>, Integer, Void> {

        protected Void doInBackground(ArrayList<Song>... list)
        {
            //implement background tasks
            ArrayList<Song> songList = list[0];
            int size = songList.size();

            for(int i=0; i<size; i++)
            {
                songList.get(i).setBpm((int)AnalyzeBPM(songList.get(i).getPath()));
                mFirebaseDatabaseReference.push().setValue(songList.get(i));
                publishProgress((100*(i+1))/(2*size));
            }

            return null;
        }

        protected void onProgressUpdate(Integer... values) {
            // Executes whenever publishProgress is called from doInBackground
            // Used to update the progress indicator
            Progress.setProgress(values[0]);
        }


    }


    static{
        System.loadLibrary("bpm_analyzer");
    }

    public native float AnalyzeBPM(String songPath);
}



