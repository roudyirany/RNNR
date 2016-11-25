package com.mr2.rnnr;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

public class Initialization extends AppCompatActivity {

    FrameLayout wind;
    boolean fire = true;
    PowerManager powerManager;
    PowerManager.WakeLock wakelock;
    TextView textView;
    private ArrayList<Song> songList;
    private ArrayList<String> localLibrary;
    private ArrayList<String> addedSongs;
    private String[] songPaths;
    private int size;
    private int processed = 0;
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialization);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakelock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "wake lock");
        wakelock.acquire();

        textView = (TextView) findViewById(R.id.textView2);

        wind = (FrameLayout) findViewById(R.id.window);
        wind.getForeground().setAlpha(0);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, Sign_In.class));
            finish();
            return;
        }

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseAuth.getCurrentUser().getUid());
        mFirebaseDatabaseReference.keepSynced(true);

        songList = new ArrayList<Song>();
        localLibrary = new ArrayList<String>();
        getSongList();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });


        for (int i = 0; i < songList.size(); i++)
            localLibrary.add(songList.get(i).getTitle());

    }

    @Override
    public void onStart() {
        super.onStart();


        //Checks if user exists and updates library accordingly
        mFirebaseDatabaseReference.child("library").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    size = songList.size();

                    songPaths = new String[size];
                    for(int i=0; i<size; i++)
                        songPaths[i] = songList.get(i).getPath();

                    int j = 0;
                    while ((j + 4) < size) {
                        if (fire) {
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 1));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 2));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 3));
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(j + 4));
                            j = j + 5;
                        }
                    }

                    for (int i = j; i < size; i++)
                        new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(i));


                } else {

                    //iterate over online library and store keys in an array
                    ArrayList<String> cloudLibrary = new ArrayList<String>();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        cloudLibrary.add(postSnapshot.getKey());
                    }

                    ArrayList<String> intersection = new ArrayList<String>(cloudLibrary);
                    ArrayList<String> added = new ArrayList<String>(localLibrary);
                    final ArrayList<String> removed = new ArrayList<String>(cloudLibrary);

                    //intersection between cloud and local library
                    intersection.retainAll(localLibrary);

                    //additions to local library
                    added.removeAll(intersection);
                    addedSongs = new ArrayList<String>(added);

                    //deletions from local library
                    removed.removeAll(intersection);

                    size = added.size() + removed.size();

                    if (size > 0) {
                        if (added.size() > 0) {
                            //add missing tracks
                            int j = 0;
                            while ((j + 4) < added.size()) {
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

                            for (int i = j; i < added.size(); i++) {
                                int index = localLibrary.indexOf(added.get(i));
                                new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));
                            }
                        }


                        for (int i = 0; i < removed.size(); i++) {
                            mFirebaseDatabaseReference.child("library/"+removed.get(i)).setValue(null);

                            //Handle clusters
                            mFirebaseDatabaseReference.child("clusters").orderByChild("parent").equalTo(removed.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.getValue() != null){
                                            int c=0;
                                            for(DataSnapshot postSnapshot: dataSnapshot.getChildren())
                                                c = Integer.parseInt(postSnapshot.getKey());
                                            final int cluster = c;
                                            mFirebaseDatabaseReference.child("library").orderByChild("cluster").equalTo(cluster).limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {

                                                    //If no other tracks from the cluster exist delete that cluster since it is non existen
                                                    if(dataSnapshot.getValue() == null)
                                                        mFirebaseDatabaseReference.child("clusters/"+cluster).setValue(null);
                                                        // If another track from the same cluster exists set it as that cluster's parent
                                                    else {
                                                        String key=null;
                                                        for (DataSnapshot postSnapshot: dataSnapshot.getChildren())
                                                            key = postSnapshot.getKey();
                                                        mFirebaseDatabaseReference.child("clusters/" + cluster + "/parent").setValue(key);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                            processed++;

                            if(processed == size)
                                openIntent();
                        }
                    }

                    else
                        openIntent();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });


    }

    //Get song list from local library
    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns

            int dataColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);

            //add songs to list

            do {
                String thisPath = musicCursor.getString(dataColumn);
                String thisTitle = (thisPath.substring(thisPath.lastIndexOf("/") + 1, thisPath.length() - 4)).replace(".", "").replace(" ", "").replace("#", "").replace("[", "").replace("]", "");
                songList.add(new Song(thisTitle, thisPath));
            }
            while (musicCursor.moveToNext());
        }
        musicCursor.close();
    }

    //Stats Popup Window
    public void showPopup() {

        // Inflate the popup_layout.xml
        LayoutInflater layoutInflater = (LayoutInflater) getBaseContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View popupView = layoutInflater.inflate(R.layout.statspopup, null);

        // Creating the PopupWindow
        final PopupWindow popup = new PopupWindow(
                popupView,
                1000,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        NumberPicker np = (NumberPicker) popupView.findViewById(R.id.numberpicker);  // get the widget
        np.setMaxValue(100);                                                 // set the max value
        np.setMinValue(5);                                                 // set the min value
        np.setValue(20);                                                    // set initial display value
        np.setWrapSelectorWheel(true);

        //Display PopupWindow at center
        popup.showAtLocation(popupView, Gravity.CENTER, 0, 0);

        wind.getForeground().setAlpha(220);

        // Getting a reference to Close button, and close the popup when clicked.
        Button submit = (Button) popupView.findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                RadioButton male = (RadioButton) popupView.findViewById(R.id.radioButton1);
                NumberPicker age = (NumberPicker) popupView.findViewById(R.id.numberpicker);
                int x = age.getValue();
                double y = 0;

                //Calculate target speed
                if (male.isChecked())
                    y = -0.0718 * x + 12.427;

                else
                    y = -0.0681 * x + 11.14;

                DecimalFormat df = new DecimalFormat("####0.00");
                mFirebaseDatabaseReference.child("targetSpeed").setValue(df.format(y));
                popup.dismiss();
                wind.getForeground().setAlpha(0);


                //if first time logging in, music tester opens. if not, user is redirected to main menu.
                mFirebaseDatabaseReference.child("testing").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            Intent intent = new Intent(Initialization.this, MainMenu.class);
                            startActivity(intent);

                            finish();
                        } else {

                            Intent intent = new Intent(Initialization.this, MusicTester.class);
                            startActivity(intent);

                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }
        });
    }

    //Background bpm processing thread
    class MyAsyncTask extends AsyncTask<Song, Integer, Void> {
        protected Void doInBackground(Song... songs) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
            //implement background tasks
            Song song = songs[0];
            int bpm = (int) AnalyzeBPM(song.getPath());

            mFirebaseDatabaseReference.child("library").child(song.getTitle()).child("path").setValue(song.getPath());
            mFirebaseDatabaseReference.child("library").child(song.getTitle()).child("bpm").setValue(bpm);

            processed++;
            if (processed % 5 == 0) {
                fire = true;
                fire = false;
            }

            publishProgress((100*processed)/size);

            return null;
        }

        protected void onProgressUpdate(Integer... values) {
            // Executes whenever publishProgress is called from doInBackground
            if(values[0]==100)
                new SimAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR);
            else if(values[0] > 0)
                textView.setText(Integer.toString(values[0]-1)+"%");

        }


    }

    //Background similarity processing thread
    class SimAsyncTask extends AsyncTask<Void, Void, Void>{
        protected Void doInBackground(Void... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            //Updates cluster information
            mFirebaseDatabaseReference.child("clusters").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue() == null)
                    {
                        float[][] similarity = new float[size][size];
                        similarity = Musly(songPaths);
                        ArrayList<Integer> parents = new ArrayList<Integer>();

                        mFirebaseDatabaseReference.child("clusters/0/parent/").setValue(localLibrary.get(0));
                        mFirebaseDatabaseReference.child("library/"+localLibrary.get(0)+"/cluster").setValue(0);
                        parents.add(0);

                        int cluster = 1;

                        for(int i=1; i<size; i++){
                            float max = 1;
                            int parent=0;

                            for(int j=0; j<parents.size();j++){
                                if(similarity[i][parents.get(j)] < max && similarity[i][parents.get(j)] < 0.4) {
                                    max = similarity[i][parents.get(j)];
                                    parent = j;
                                }
                            }

                            if(max==1){
                                mFirebaseDatabaseReference.child("clusters/"+cluster+"/parent/").setValue(localLibrary.get(i));
                                mFirebaseDatabaseReference.child("library/"+localLibrary.get(i)+"/cluster").setValue(cluster);
                                parents.add(i);
                                cluster++;
                            }

                            else
                                mFirebaseDatabaseReference.child("library/"+localLibrary.get(i)+"/cluster").setValue(parent);


                        }
                    }

                    else{
                        ArrayList <String> parents = new ArrayList<String>();
                        ArrayList <Integer> parentsIndex = new ArrayList<Integer>();
                        ArrayList <Integer> clusterIndex = new ArrayList<Integer>();
                        String[] songPaths;
                        int cluster=0;

                        //Get cluster parents
                        int x=addedSongs.size();
                        for(DataSnapshot postSnapshot: dataSnapshot.getChildren()){
                            parents.add(postSnapshot.child("parent").getValue().toString());
                            parentsIndex.add(x);
                            clusterIndex.add(Integer.parseInt(postSnapshot.getKey()));

                            if (cluster < Integer.parseInt(postSnapshot.getKey())+1)
                                cluster = Integer.parseInt(postSnapshot.getKey())+1;

                            x++;
                        }

                        if(addedSongs.size()>0) {
                            int size1 = addedSongs.size() + parents.size();
                            float[][] similarity = new float[size1][size1];
                            songPaths = new String[size1];
                            int index=0;

                            for (int i = 0; i < addedSongs.size(); i++) {
                                index=i;
                                songPaths[i] = songList.get(localLibrary.indexOf(addedSongs.get(i))).getPath();
                            }

                            index++;
                            for (int i = 0; i < parents.size(); i++) {
                                songPaths[index] = songList.get(localLibrary.indexOf(parents.get(i))).getPath();
                                index++;
                            }

                            similarity = Musly(songPaths);

                            for(int i=0; i<addedSongs.size();i++){
                                float max=1;
                                int parent=0;

                                for(int j=0; j<parentsIndex.size();j++){
                                    if(similarity[i][parentsIndex.get(j)] < max && similarity[i][parentsIndex.get(j)] < 0.4) {
                                        max = similarity[i][parentsIndex.get(j)];
                                        parent = j;
                                    }
                                }

                                if(max==1){
                                    mFirebaseDatabaseReference.child("clusters/"+cluster+"/parent/").setValue(addedSongs.get(i));
                                    mFirebaseDatabaseReference.child("library/"+addedSongs.get(i)+"/cluster").setValue(cluster);
                                    parents.add(addedSongs.get(i));
                                    parentsIndex.add(i);
                                    clusterIndex.add(cluster);
                                    cluster++;
                                }

                                else{
                                    mFirebaseDatabaseReference.child("library/"+addedSongs.get(i)+"/cluster").setValue(clusterIndex.get(parent));
                                }

                            }

                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            return null;
        }

        protected void onPostExecute(Void Result){
            wakelock.release();
            openIntent();
        }
    }

    //Open next activity: Main menu or Music tester
    public void openIntent(){
        //Checks if target speed value has been calculated before
        mFirebaseDatabaseReference.child("targetSpeed").addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    showPopup();
                } else {
                    mFirebaseDatabaseReference.child("testing").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                Intent intent = new Intent(Initialization.this, MainMenu.class);
                                startActivity(intent);

                                finish();
                            } else {

                                Intent intent = new Intent(Initialization.this, MusicTester.class);
                                startActivity(intent);

                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Native C Libraries & Functions
    static {
        System.loadLibrary("bpm_analyzer");
        System.loadLibrary("rnnrsim");
    }

    public native float AnalyzeBPM(String songPath);
    public native float[][] Musly(String[] songPaths);

}



