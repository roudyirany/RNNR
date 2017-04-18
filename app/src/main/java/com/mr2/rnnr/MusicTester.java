package com.mr2.rnnr;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MusicTester extends AppCompatActivity {

    ArrayList<String> songList;
    ArrayList<String> songNames;
    ArrayList<String> likedSongs;
    ArrayList<ArrayList<String>> likeTransitions;
    ArrayList<Double> weightC;
    ArrayList<Double> weightB;
    HashMap weightTC;
    HashMap weightTB;
    int songsLoaded = 0;
    int size;
    ArrayList<Integer> songListNumbers;
    MediaPlayer mediaPlayer;
    CountDownTimer countDownTimer;
    int timer = 20000;
    int maxVol;
    int currentVol;
    SeekBar volumeBar;
    AudioManager audioManager;
    Button likeT;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_tester);

        songList = new ArrayList<String>();
        likedSongs = new ArrayList<String>();
        likeTransitions = new ArrayList<ArrayList<String>>();
        songNames = new ArrayList<String>();
        weightC = new ArrayList<Double>();
        weightB = new ArrayList<Double>();
        weightTC = new HashMap();
        weightTB = new HashMap();
        likeT = (Button) findViewById(R.id.likeT);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/library");

        volumeBar = (SeekBar) findViewById(R.id.seekBar);

        //Disable button at first
        Button button = (Button) findViewById(R.id.done);
        button.setEnabled(false);

        //Get all audio files from library
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String s = postSnapshot.child("path").getValue().toString();
                    songList.add(s);
                    songNames.add(postSnapshot.getKey());
                }

                size = songList.size();
                loadSong();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Enable volume display and control
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volumeBar.setMax(100);
        volumeBar.setProgress((int) Math.ceil(((double) currentVol / (double) maxVol) * 100));
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentVol = (int) Math.ceil(((double) volumeBar.getProgress() / 100.0) * (double) maxVol);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        songListNumbers = new ArrayList<Integer>();

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(20);
        progressBar.setProgress(0);

        //If song is playing, pause button should be displayed and vice versa.
        final ImageView imageView = (ImageView) findViewById(R.id.playpause);
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (String.valueOf(imageView.getTag()).equals("play")) {
                    if (timer == 20000)
                        mediaPlayer.seekTo(30000);

                    mediaPlayer.start();
                    imageView.setImageResource(android.R.drawable.ic_media_pause);
                    imageView.setTag("pause");

                    countDownTimer = new CountDownTimer(timer, 1000) {

                        public void onTick(long millisUntilFinished) {
                            timer = timer - 1000;
                            if (timer > 1000)
                                progressBar.setProgress((20000 - timer) / 1000);
                            else
                                progressBar.setProgress(20);
                        }

                        public void onFinish() {
                            mediaPlayer.pause();
                            timer = 20000;
                            imageView.setImageResource(android.R.drawable.ic_media_play);
                            imageView.setTag("play");
                            progressBar.setProgress(0);
                        }
                    }.start();
                } else {
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.pause();
                    if (countDownTimer != null)
                        countDownTimer.cancel();
                    imageView.setImageResource(android.R.drawable.ic_media_play);
                    imageView.setTag("play");
                }
            }
        });

        //Skip button functionality
        final ImageView skip = (ImageView) findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                nextSong();
            }
        });

        //Like button functionality
        final ImageView like = (ImageView) findViewById(R.id.like);
        like.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                likedSongs.add(songNames.get(songListNumbers.get(songListNumbers.size()-1)));
                nextSong();
            }
        });

        likeT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> songCombo = new ArrayList<String>();
                songCombo.add(songNames.get(songListNumbers.get(songListNumbers.size()-2)));
                songCombo.add(songNames.get(songListNumbers.get(songListNumbers.size()-1)));
                likeTransitions.add(songCombo);
                likeT.setEnabled(false);
            }
        });


    }

    //Loads the next song randomly
    public boolean loadSong() {

        //Enable done button when enough songs have been rated
        if (songsLoaded > (int) Math.ceil(0.1 * size)) {
            Button button = (Button) findViewById(R.id.done);
            button.setEnabled(true);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    countDownTimer.cancel();
                    mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid());
                    mFirebaseDatabaseReference.child("testing").setValue(true);

                    Intent intent = new Intent(MusicTester.this, MainMenu.class);
                    initializeWeights();
                    initializeTransitions();
                    startActivity(intent);
                    finish();
                }
            });
        }

        if (songsLoaded < size - (int) Math.ceil(0.1 * size)) {
            songsLoaded++;

            if (songsLoaded > 1)
                likeT.setEnabled(true);

            int n = (int) (Math.random() * (songList.size() - 1) + 0);

            if (songListNumbers.isEmpty())
                songListNumbers.add(n);
            else {
                while (songListNumbers.contains(n))
                    n = (int) (Math.random() * (songList.size() - 1) + 0);
                songListNumbers.add(n);
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(songList.get(n));

            //Displays album cover art
            byte[] art = retriever.getEmbeddedPicture();
            ImageView imgAlbum = (ImageView) findViewById(R.id.imgAlbum);
            if (art != null) {
                imgAlbum.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length));
            } else {
                imgAlbum.setImageResource(R.drawable.unknown_track);
            }

            //Displays artist name and title
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            TextView Artist = (TextView) findViewById(R.id.artist);
            TextView Title = (TextView) findViewById(R.id.title);

            if (artist == null)
                artist = "Unknown Artist";
            if (title == null)
                title = "Unknown Track";

            if (artist.length() <= 30)
                Artist.setText(artist);
            else
                Artist.setText(artist.substring(0, 30) + "...");

            if (title.length() <= 30)
                Title.setText(title);
            else
                Title.setText(title.substring(0, 30) + "...");

            mediaPlayer = MediaPlayer.create(MusicTester.this, Uri.parse(songList.get(n)));
            return true;
        } else {
            mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid());
            mFirebaseDatabaseReference.child("testing").setValue(true);
            Intent intent = new Intent(MusicTester.this, MainMenu.class);
            initializeWeights();
            startActivity(intent);
            finish();
            return false;
        }

    }

    //Override volume up & down key behaviors
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeBar.getProgress() - 10 < 0) {
                currentVol = 0;
                volumeBar.setProgress(0);
            } else {
                volumeBar.setProgress(volumeBar.getProgress() - 10);
                currentVol = (int) Math.ceil(((double) volumeBar.getProgress() / 100.0) * (double) maxVol);
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (volumeBar.getProgress() + 10 > 100) {
                currentVol = 100;
                volumeBar.setProgress(100);
            } else {
                volumeBar.setProgress(volumeBar.getProgress() + 10);
                currentVol = (int) Math.ceil(((double) volumeBar.getProgress() / 100.0) * (double) maxVol);
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    //Play next song
    public void nextSong() {
        final ImageView imageView = (ImageView) findViewById(R.id.playpause);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        mediaPlayer.pause();
        mediaPlayer.release();

        if (countDownTimer != null)
            countDownTimer.cancel();
        timer = 20000;

        if (loadSong()) {
            mediaPlayer.seekTo(30000);

            mediaPlayer.start();
            imageView.setImageResource(android.R.drawable.ic_media_pause);
            imageView.setTag("pause");

            countDownTimer = new CountDownTimer(timer, 1000) {

                public void onTick(long millisUntilFinished) {
                    timer = timer - 1000;
                    if (timer > 1000)
                        progressBar.setProgress((20000 - timer) / 1000);
                    else
                        progressBar.setProgress(20);
                }

                public void onFinish() {
                    mediaPlayer.pause();
                    timer = 20000;
                    imageView.setImageResource(android.R.drawable.ic_media_play);
                    imageView.setTag("play");
                    progressBar.setProgress(0);
                }
            }.start();
        }
    }

    //Initialize weights
    public void initializeWeights() {
        final DatabaseReference clusterReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/clusters");
        final DatabaseReference bpmReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/bpms");
        clusterReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final int liked = likedSongs.size();
                FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/likedSongs").setValue(liked);
                int clusterBins = (int) dataSnapshot.getChildrenCount();
                double initialWeight = 1.0 / ((double) (liked + clusterBins + 11));

                for (int i = 0; i < clusterBins; i++)
                    weightC.add(initialWeight);
                for (int i = 0; i < 11; i++)
                    weightB.add(initialWeight);

                mFirebaseDatabaseReference.child("library").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (int i = 0; i < liked; i++) {
                            int bpm = (int) (long) dataSnapshot.child(likedSongs.get(i)).child("bpm").getValue();
                            if (bpm < 50)
                                bpm = 0;
                            else if (bpm < 55)
                                bpm = 1;
                            else if (bpm < 60)
                                bpm = 2;
                            else if (bpm < 70)
                                bpm = 3;
                            else if (bpm < 85)
                                bpm = 4;
                            else if (bpm < 100)
                                bpm = 5;
                            else if (bpm < 115)
                                bpm = 6;
                            else if (bpm < 140)
                                bpm = 7;
                            else if (bpm < 150)
                                bpm = 8;
                            else if (bpm < 170)
                                bpm = 9;
                            else
                                bpm = 10;

                            int cluster = (int) (long) dataSnapshot.child(likedSongs.get(i)).child("cluster").getValue();

                            weightC.set(cluster, weightC.get(cluster) + 1.0 / ((double) (liked + 1)));
                            weightB.set(bpm, weightB.get(bpm) + 1.0 / ((double) (liked + 1.0)));
                        }

                        for (int i = 0; i < weightC.size(); i++)
                            clusterReference.child(Integer.toString(i)).child("weight").setValue(weightC.get(i));
                        for (int i = 0; i < 11; i++)
                            bpmReference.child(Integer.toString(i)).child("weight").setValue(weightB.get(i));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Initialize transition weights
    public void initializeTransitions() {
        final DatabaseReference clusterReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/clusters");
        final DatabaseReference bpmReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/bpms");
        final DatabaseReference transitionReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid() + "/transitions");

        clusterReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int clusterSize = (int) (long) dataSnapshot.getChildrenCount();

                Double initialWeight = 1.0 / ((double) (likeTransitions.size() + (clusterSize * clusterSize + 11 * 11)));

                for (int i = 0; i < clusterSize; i++) {
                    for (int j = 0; j < clusterSize; j++) {
                        weightTC.put(i + "-" + j, initialWeight);
                    }
                }

                for (int i = 0; i < 11; i++) {
                    for (int j = 0; j < 11; j++) {
                        weightTB.put(i + "-" + j, initialWeight);
                    }
                }

                mFirebaseDatabaseReference.child("library").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Set wC = weightTC.entrySet();
                        Set wB = weightTB.entrySet();
                        Iterator C = wC.iterator();
                        Iterator B = wB.iterator();

                        for (int i = 0; i < likeTransitions.size(); i++) {
                            int bpm1 = (int) (long) dataSnapshot.child(likeTransitions.get(i).get(0)).child("bpm").getValue();
                            if (bpm1 < 50)
                                bpm1 = 0;
                            else if (bpm1 < 55)
                                bpm1 = 1;
                            else if (bpm1 < 60)
                                bpm1 = 2;
                            else if (bpm1 < 70)
                                bpm1 = 3;
                            else if (bpm1 < 85)
                                bpm1 = 4;
                            else if (bpm1 < 100)
                                bpm1 = 5;
                            else if (bpm1 < 115)
                                bpm1 = 6;
                            else if (bpm1 < 140)
                                bpm1 = 7;
                            else if (bpm1 < 150)
                                bpm1 = 8;
                            else if (bpm1 < 170)
                                bpm1 = 9;
                            else
                                bpm1 = 10;
                            int cluster1 = (int) (long) dataSnapshot.child(likeTransitions.get(i).get(0)).child("cluster").getValue();

                            int bpm2 = (int) (long) dataSnapshot.child(likeTransitions.get(i).get(1)).child("bpm").getValue();
                            if (bpm2 < 50)
                                bpm2 = 0;
                            else if (bpm2 < 55)
                                bpm2 = 1;
                            else if (bpm2 < 60)
                                bpm2 = 2;
                            else if (bpm2 < 70)
                                bpm2 = 3;
                            else if (bpm2 < 85)
                                bpm2 = 4;
                            else if (bpm2 < 100)
                                bpm2 = 5;
                            else if (bpm2 < 115)
                                bpm2 = 6;
                            else if (bpm2 < 140)
                                bpm2 = 7;
                            else if (bpm2 < 150)
                                bpm2 = 8;
                            else if (bpm2 < 170)
                                bpm2 = 9;
                            else
                                bpm2 = 10;
                            int cluster2 = (int) (long) dataSnapshot.child(likeTransitions.get(i).get(1)).child("cluster").getValue();

                            Double weightC = (Double) weightTC.get(cluster1 + "-" + cluster2);
                            Log.d("weight1",""+weightC);
                            Double weightB = (Double) weightTB.get(bpm1 + "-" + bpm2);

                            weightC = weightC + 1.0 / ((double) (likeTransitions.size() + 1));
                            Log.d("weight2",""+weightC);
                            weightB = weightB + 1.0 / ((double) (likeTransitions.size() + 1));

                            weightTC.put(cluster1 + "-" + cluster2, weightC);
                            weightTB.put(bpm1 + "-" + bpm2, weightB);
                        }

                        while (C.hasNext()) {
                            Map.Entry me = (Map.Entry) C.next();
                            transitionReference.child("clusters").child(me.getKey().toString()).setValue(me.getValue());
                        }

                        while (B.hasNext()) {
                            Map.Entry me = (Map.Entry) B.next();
                            transitionReference.child("bpms").child(me.getKey().toString()).setValue(me.getValue());
                        }

                        mFirebaseDatabaseReference.child("likedTransitions").setValue(likeTransitions.size());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


}
