package com.mr2.rnnr;

import android.content.Intent;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.GenericArrayType;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

public class Initialization extends AppCompatActivity {
    ProgressBar Progress;
    private ArrayList<Song> songList;
    private ArrayList<String> localLibrary;
    private int size;
    private int processed=0;
    boolean fire=true;

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

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseAuth.getCurrentUser().getUid()+"/library");

        songList = new ArrayList<Song>();
        localLibrary = new ArrayList<String>();
        getSongList();

        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });


        for(int i=0; i<songList.size();i++)
            localLibrary.add(songList.get(i).getTitle());

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
        Progress.setProgress(0);
        Progress.setMax(100);

        //Checks if user exists and updates library accordingly
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null)
                {

                    size = songList.size();
                    int j=0;
                    while((j+4)<size)
                    {
                        if(fire) {
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 1));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 2));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 3));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 4));
                            j = j + 5;
                        }
                    }

                    for(int i=j ; i<size; i++)
                        new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(i));

                }

                else {

                    //iterate over online library and store keys in an array
                    ArrayList<String> cloudLibrary = new ArrayList<String>();
                    for(DataSnapshot postSnapshot: snapshot.getChildren()) {
                        cloudLibrary.add(postSnapshot.getKey());
                    }

                    ArrayList<String> intersection = new ArrayList<String>(cloudLibrary);
                    ArrayList<String> added = new ArrayList<String>(localLibrary);
                    ArrayList<String> removed = new ArrayList<String>(cloudLibrary);

                    //intersection between cloud and local library
                    intersection.retainAll(localLibrary);

                    //additions to local library
                    added.removeAll(intersection);
                    size = added.size();

                    if(size>0) {
                        //add missing tracks
                        int j = 0;
                        while ((j + 4) < size) {
                            if (fire) {
                                int index = localLibrary.indexOf(added.get(j));
                                new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));

                                index = localLibrary.indexOf(added.get(j + 1));
                                new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));

                                index = localLibrary.indexOf(added.get(j + 2));
                                new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));

                                index = localLibrary.indexOf(added.get(j + 3));
                                new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));

                                index = localLibrary.indexOf(added.get(j + 4));
                                new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));

                                j = j + 5;
                            }
                        }

                        for (int i = j; i < size; i++) {
                            int index = localLibrary.indexOf(added.get(i));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));
                        }
                    }

                    //deletions from local library
                    removed.removeAll(intersection);
                    size = removed.size();

                    for(int i=0; i<size; i++)
                    {
                        mFirebaseDatabaseReference.child(removed.get(0)).setValue(null);
                        Progress.setProgress(((i+1)/size)*100);
                    }

                    //Progress.setProgress(100);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });


    }


    //Background song processing thread
    class MyAsyncTask extends AsyncTask<Song, Integer, Void> {

        protected Void doInBackground(Song... songs)
        {
            //implement background tasks
            Song song = songs[0];

            mFirebaseDatabaseReference.child(song.getTitle()).child("path").setValue(song.getPath());
            mFirebaseDatabaseReference.child(song.getTitle()).child("bpm").setValue((int)AnalyzeBPM(song.getPath()));
            mFirebaseDatabaseReference.child(song.getTitle()).child("cluster").setValue(0);

            processed++;
            if (processed % 5 == 0)
                fire = true;
            else
                fire = false;

            publishProgress((100*(processed))/(size));

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

    //Get song list from local library
    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns

            int dataColumn= musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);

            //add songs to list

            do {
                String thisPath = musicCursor.getString(dataColumn);
                String thisTitle = (thisPath.substring(thisPath.lastIndexOf("/")+1,thisPath.length()-4)).replace(".","").replace(" ","");
                songList.add(new Song(thisTitle, thisPath));
            }
            while (musicCursor.moveToNext());
        }
    }
}



