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
import java.util.Random;

public class MusicTester extends AppCompatActivity {

    ArrayList<String> songList;
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

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_tester);

        songList = new ArrayList<String>();

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
                            mediaPlayer.stop();
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
                nextSong();
            }
        });

    }

    //Loads the next song randomly
    public void loadSong() {

        if (songsLoaded < size) {
            songsLoaded++;
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
            TextView artistTitle = (TextView) findViewById(R.id.artistTitle);

            String result = artist + " - " + title;
            if (result.length() <= 50)
                artistTitle.setText(artist + " - " + title);
            else
                artistTitle.setText(artist + " - " + title.substring(0, 48 - artist.length()) + "...");

            mediaPlayer = MediaPlayer.create(MusicTester.this, Uri.parse(songList.get(n)));
            mediaPlayer.setVolume(currentVol, currentVol);
        } else {
            Intent intent = new Intent(MusicTester.this, MainMenu.class);
            startActivity(intent);
            finish();
        }

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
                    startActivity(intent);
                    finish();
                }
            });
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

        loadSong();
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
                mediaPlayer.stop();
                timer = 20000;
                imageView.setImageResource(android.R.drawable.ic_media_play);
                imageView.setTag("play");
                progressBar.setProgress(0);
            }
        }.start();
    }
}
