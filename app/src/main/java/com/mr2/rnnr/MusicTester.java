package com.mr2.rnnr;

import android.graphics.BitmapFactory;
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

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private DatabaseReference mFirebaseDatabaseReference;

    ArrayList<String> songList;

    MediaPlayer mediaPlayer;
    CountDownTimer countDownTimer;
    int timer = 20000;
    float currentVol = (float)0.5;
    SeekBar volumeBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_tester);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid()+"/library");

        volumeBar = (SeekBar) findViewById(R.id.seekBar);

        //Disable button at first
        Button button = (Button) findViewById(R.id.done);
        button.setEnabled(false);

        //Get all audio files from library
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for(DataSnapshot postSnapshot: snapshot.getChildren()) {
                    String s = postSnapshot.child("path").getValue().toString();
                    songList.add(s);
                }

                loadSong();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //Enable volume display and control
        volumeBar.setMax(10);
        volumeBar.setProgress(5);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float log1=(float)(Math.log(10-progress)/Math.log(10));
                mediaPlayer.setVolume((float)(1-log1),(float)(1-log1));
                currentVol = 1-log1;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    @Override
    protected void onStart(){
        super.onStart();
        songList = new ArrayList<String>();

        Toast.makeText(MusicTester.this, "Please rate at least 20 tracks from your library before proceeding.",
                Toast.LENGTH_LONG).show();

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(20);
        progressBar.setProgress(0);

        //If song is playing, pause button should be displayed and vice versa.
        final ImageView imageView = (ImageView) findViewById(R.id.playpause);
        imageView.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(String.valueOf(imageView.getTag()).equals("play")){
                    if(timer == 20000)
                        mediaPlayer.seekTo(30000);

                    mediaPlayer.start();
                    imageView.setImageResource(android.R.drawable.ic_media_pause);
                    imageView.setTag("pause");

                    countDownTimer = new CountDownTimer(timer,1000) {

                        public void onTick(long millisUntilFinished) {
                            timer = timer - 1000;
                            if(timer>1000)
                                progressBar.setProgress((20000-timer)/1000);
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

                else{
                    mediaPlayer.pause();
                    countDownTimer.cancel();
                    imageView.setImageResource(android.R.drawable.ic_media_play);
                    imageView.setTag("play");
                }
            }
        });

    }

    //Loads the next song randomly
    public void loadSong(){
        int n = (int) (Math.random() * (songList.size()-1) + 0);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(songList.get(n));

        //Displays album cover art
        byte[] art = retriever.getEmbeddedPicture();
        ImageView imgAlbum = (ImageView) findViewById(R.id.imgAlbum);
        if( art != null ){
            imgAlbum.setImageBitmap( BitmapFactory.decodeByteArray(art, 0, art.length));
        }
        else{
            imgAlbum.setImageResource(R.drawable.unknown_track);
        }

        //Displays artist name and title
        String artist =  retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        TextView artistTitle = (TextView) findViewById(R.id.artistTitle);

        String result = artist + " - " + title;
        if(result.length()<=50)
            artistTitle.setText(artist + " - " + title);
        else
            artistTitle.setText(artist + " - " + title.substring(0,48-artist.length())+"...");

        mediaPlayer = MediaPlayer.create(MusicTester.this, Uri.parse(songList.get(n)));
        mediaPlayer.setVolume(currentVol,currentVol);
    }

    //Override volume up & down key behaviors
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if(currentVol-(float)0.1 < 0)
                currentVol=0;
            else
                currentVol = currentVol - (float)0.1;

            mediaPlayer.setVolume(currentVol,currentVol);

            if(volumeBar.getProgress()-1 < 0)
                volumeBar.setProgress(0);
            else
                volumeBar.setProgress(volumeBar.getProgress()-1);
            return true;
        }
        else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            if(currentVol+(float)0.1 > 1)
                currentVol=1;
            else
                currentVol = currentVol + (float)0.1;

            mediaPlayer.setVolume(currentVol,currentVol);

            if(volumeBar.getProgress()+1 > 10)
                volumeBar.setProgress(10);
            else
                volumeBar.setProgress(volumeBar.getProgress()+1);
            return true;
        }
        else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
