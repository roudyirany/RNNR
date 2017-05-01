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
import android.widget.Toast;


import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

public class Initialization extends AppCompatActivity {

    FrameLayout wind;
    boolean firstTime = true;
    PowerManager powerManager;
    PowerManager.WakeLock wakelock;
    TextView textView;
    private ArrayList<Song> songList;
    private ArrayList<String> localLibrary;
    private ArrayList<String> addedSongs;
    private int size;
    private int processed = 0;
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    double weightT;
    double weightTra;

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

        songList = new ArrayList<Song>();
        getSongList();

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null || songList.size() == 0) {
            if (songList.size() == 0)
                Toast.makeText(Initialization.this, "Your music library is empty. Please add songs to your library before using this app.",
                        Toast.LENGTH_LONG).show();
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, Sign_In.class));
            finish();
            return;
        }

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid());
        mFirebaseDatabaseReference.keepSynced(true);

        checkLibrary();

        localLibrary = new ArrayList<String>();

        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });


        for (int i = 0; i < songList.size(); i++)
            localLibrary.add(songList.get(i).getTitle());

        //Power on Musly similarity component
        MuslyPowerOn();

        //Checks if user exists and updates library accordingly
        mFirebaseDatabaseReference.child("library").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    mFirebaseDatabaseReference.child("walking").setValue(0.726);
                    mFirebaseDatabaseReference.child("running").setValue(0.750);

                    size = songList.size();
                    if (size < 128) {
                        for (int i = 0; i < size; i++)
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(i));
                    } else {
                        for (int i = 0; i < 128; i++)
                            new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(i));
                    }
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

                    if (intersection.isEmpty()) {
                        mFirebaseDatabaseReference.setValue(null);
                        Intent intent = new Intent(Initialization.this, Initialization.class);
                        startActivity(intent);
                        finish();
                    }


                    //additions to local library
                    added.removeAll(intersection);
                    addedSongs = new ArrayList<String>(added);

                    //deletions from local library
                    removed.removeAll(intersection);

                    size = added.size() + removed.size();

                    if (size > 0) {
                        for (int i = 0; i < removed.size(); i++) {
                            mFirebaseDatabaseReference.child("library/" + removed.get(i)).setValue(null);

                            //Handle clusters
                            mFirebaseDatabaseReference.child("clusters").orderByChild("parent").equalTo(removed.get(i)).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {
                                        int c = 0;
                                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren())
                                            c = Integer.parseInt(postSnapshot.getKey());
                                        final int cluster = c;
                                        mFirebaseDatabaseReference.child("library").orderByChild("cluster").equalTo(cluster).limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {

                                                //If no other tracks from the cluster exist delete that cluster since it is non existent
                                                if (dataSnapshot.getValue() == null) {

                                                    mFirebaseDatabaseReference.child("clusters/").child(Integer.toString(cluster)).child("parent").setValue(null);
                                                    mFirebaseDatabaseReference.child("clusters/").child(Integer.toString(cluster)).child("weight").setValue(null);

                                                    //Update weights
                                                    updateWeightsSubtract();

                                                    mFirebaseDatabaseReference.child("clusters").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            int clusters = (int) (long) dataSnapshot.getChildrenCount();
                                                            for (int i = 0; i < clusters; i++) {
                                                                mFirebaseDatabaseReference.child("transitions").child("clusters").child(cluster + "-" + i).setValue(null);
                                                                mFirebaseDatabaseReference.child("transitions").child("clusters").child(i + "-" + cluster).setValue(null);
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {

                                                        }
                                                    });

                                                }
                                                // If another track from the same cluster exists set it as that cluster's parent
                                                else {
                                                    String key = null;
                                                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren())
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

                            if (processed == size)
                                openIntent();
                        }

                        if (added.size() > 0) {
                            firstTime = false;
                            //add missing tracks
                            if (size < 128) {
                                for (int i = 0; i < size; i++) {
                                    int index = localLibrary.indexOf(added.get(i));
                                    new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));
                                }
                            } else {
                                for (int i = 0; i < 128; i++) {
                                    int index = localLibrary.indexOf(added.get(i));
                                    new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));
                                }
                            }
                        }
                    } else {
                        wakelock.release();
                        openIntent();
                    }
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
                String[] split = thisPath.split("/");
                String thisPath1 = split[split.length - 2] + "-" + split[split.length - 1];
                String thisTitle = (thisPath1.substring(0, thisPath1.length() - 4)).replace(".", "").replace(" ", "").replace("#", "").replace("[", "").replace("]", "").replace("$", "s");
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

                DecimalFormat df = new DecimalFormat("####0.0");
                mFirebaseDatabaseReference.child("targetSpeed").setValue(Double.parseDouble(df.format(y)));
                popup.dismiss();
                wind.getForeground().setAlpha(0);


                //if first time logging in, music tester opens. if not, user is redirected to main menu.
                mFirebaseDatabaseReference.child("testing").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            Intent intent = new Intent(Initialization.this, MainMenu.class); //mainmenu
                            startActivity(intent);

                            finish();
                        } else {

                            Intent intent = new Intent(Initialization.this, MusicTester.class); //music tester
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
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

            //implement background tasks
            Song song = songs[0];
            int bpm = (int) AnalyzeBPM(song.getPath());
            mFirebaseDatabaseReference.child("library").child(song.getTitle()).child("path").setValue(song.getPath());
            mFirebaseDatabaseReference.child("library").child(song.getTitle()).child("bpm").setValue(bpm);
            Analyze(song.getPath());

            processed++;
            publishProgress((100 * processed) / (size));

            return null;
        }

        protected void onProgressUpdate(Integer... values) {
            // Executes whenever publishProgress is called from doInBackground
            if (values[0] == 100) {
                //Updates cluster information
                mFirebaseDatabaseReference.child("clusters").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null) {
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    float[][] similarity = new float[size][size];
                                    similarity = calculateSimilarity();
                                    ArrayList<Integer> parents = new ArrayList<Integer>();

                                    mFirebaseDatabaseReference.child("clusters/0/parent/").setValue(localLibrary.get(0));
                                    mFirebaseDatabaseReference.child("library/" + localLibrary.get(0) + "/cluster").setValue(0);
                                    parents.add(0);

                                    int cluster = 1;

                                    for (int i = 1; i < size; i++) {
                                        float max = 1;
                                        int parent = 0;

                                        for (int k = 0; k < parents.size(); k++) {
                                            if (similarity[i][parents.get(k)] < max && similarity[i][parents.get(k)] < 0.5) {
                                                max = similarity[i][parents.get(k)];
                                                parent = k;
                                            }
                                        }

                                        if (max == 1) {
                                            mFirebaseDatabaseReference.child("clusters/" + cluster + "/parent/").setValue(localLibrary.get(i));
                                            mFirebaseDatabaseReference.child("library/" + localLibrary.get(i) + "/cluster").setValue(cluster);
                                            parents.add(i);
                                            cluster++;
                                        } else
                                            mFirebaseDatabaseReference.child("library/" + localLibrary.get(i) + "/cluster").setValue(parent);


                                    }
                                    checkLibrary();
                                }
                            });

                            thread.start();

                        } else {
                            final ArrayList<String> parents = new ArrayList<String>();
                            final ArrayList<Integer> parentsIndex = new ArrayList<Integer>();
                            final ArrayList<Integer> clusterIndex = new ArrayList<Integer>();
                            int cluster = 0;

                            //Get cluster parents
                            int x = addedSongs.size();
                            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                parents.add(postSnapshot.child("parent").getValue().toString());
                                parentsIndex.add(x);
                                clusterIndex.add(Integer.parseInt(postSnapshot.getKey()));

                                if (cluster < Integer.parseInt(postSnapshot.getKey()) + 1)
                                    cluster = Integer.parseInt(postSnapshot.getKey()) + 1;

                                x++;
                            }

                            if (addedSongs.size() > 0) {
                                final int size1 = addedSongs.size() + parents.size();
                                final int cluster1 = cluster;

                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (int i = 0; i < parents.size(); i++)
                                            Analyze(songList.get(localLibrary.indexOf(parents.get(i))).getPath());

                                        int cluster = cluster1;
                                        float[][] similarity = new float[size1][size1];
                                        similarity = calculateSimilarity();

                                        for (int i = 0; i < addedSongs.size(); i++) {
                                            float max = 1;
                                            int parent = 0;

                                            for (int k = 0; k < parentsIndex.size(); k++) {
                                                if (similarity[i][parentsIndex.get(k)] < max && similarity[i][parentsIndex.get(k)] < 0.65) {
                                                    max = similarity[i][parentsIndex.get(k)];
                                                    parent = k;
                                                }
                                                Log.d("similarity: ", "" + similarity[i][parentsIndex.get(k)]);
                                            }

                                            if (max == 1) {
                                                mFirebaseDatabaseReference.child("clusters/" + cluster + "/parent").setValue(addedSongs.get(i));
                                                mFirebaseDatabaseReference.child("clusters/" + cluster + "/weight").setValue(weightT);
                                                mFirebaseDatabaseReference.child("library/" + addedSongs.get(i) + "/cluster").setValue(cluster);
                                                updateWeightsAdd();
                                                parents.add(addedSongs.get(i));
                                                parentsIndex.add(i);
                                                clusterIndex.add(cluster);
                                                cluster++;
                                            } else {
                                                mFirebaseDatabaseReference.child("library/" + addedSongs.get(i) + "/cluster").setValue(clusterIndex.get(parent));
                                            }

                                        }

                                        checkLibrary();
                                    }
                                });

                                thread.start();

                            }

                        }

                        wakelock.release();
                        openIntent();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            } else {
                if (processed + 127 < size) {
                    if (firstTime)
                        new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(processed + 127));
                    else {
                        int index = localLibrary.indexOf(addedSongs.get(processed + 128));
                        new MyAsyncTask().executeOnExecutor(THREAD_POOL_EXECUTOR, songList.get(index));
                    }
                }
            }

            if (values[0] > 0)
                textView.setText(Integer.toString(values[0]) + "%");

        }


    }

    //Open next activity: Main menu or Music tester
    public void openIntent() {
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
                                Intent intent = new Intent(Initialization.this, MainMenu.class); //mainmenu
                                startActivity(intent);

                                finish();
                            } else {

                                Intent intent = new Intent(Initialization.this, MusicTester.class); //musictester
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

    //Remove errors from library
    public void checkLibrary() {
        mFirebaseDatabaseReference.child("library").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    if (postSnapshot.child("bpm").getValue() == null || postSnapshot.child("path").getValue() == null || postSnapshot.child("cluster").getValue() == null)
                        mFirebaseDatabaseReference.child("library").child(postSnapshot.getKey()).setValue(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Update weights
    public void updateWeightsAdd() {
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final int liked = (int) (long) dataSnapshot.child("likedSongs").getValue();
                final int likedTra = (int) (long) dataSnapshot.child("likedTransitions").getValue();

                final int clusters = (int) dataSnapshot.child("clusters").getChildrenCount();
                weightT = 1.0 / ((double) (liked + clusters + 11 + 1));
                weightTra = 1.0 / ((double) (likedTra + (clusters + 1) * (clusters + 1) + 11 * 11));

                int i = 0;
                for (DataSnapshot postSnapshot : dataSnapshot.child("clusters").getChildren()) {
                    Double weight = (Double) postSnapshot.child("weight").getValue();
                    if (weight == null)
                        weight = 0.0;
                    weight = weight - 1.0 / ((double) (liked + clusters + 11));
                    weight = weight + weightT;
                    mFirebaseDatabaseReference.child("clusters").child(Integer.toString(i)).child("weight").setValue(weight);

                    for (int j = 0; j < dataSnapshot.child("clusters").getChildrenCount(); j++) {
                        Double weightTr = (Double) dataSnapshot.child("transitions").child("clusters").child(i + "-" + j).getValue();
                        if (weightTr == null)
                            weightTr = 0.0;
                        weightTr = weightTr - 1.0 / ((double) (likedTra + (clusters) * (clusters) + 11 * 11));
                        weightTr = weightTr + weightTra;
                        mFirebaseDatabaseReference.child("transitions").child("clusters").child(i + "-" + j).setValue(weightTr);
                    }

                    i++;
                }


                for (int j = 0; j < 11; j++) {
                    Double weight = (Double) dataSnapshot.child("bpms").child(Integer.toString(j)).child("weight").getValue();
                    weight = weight - 1.0 / ((double) (liked + clusters + 11));
                    weight = weight + weightT;
                    mFirebaseDatabaseReference.child("bpms").child(Integer.toString(j)).child("weight").setValue(weight);

                    for (int k = 0; k < dataSnapshot.child("clusters").getChildrenCount(); k++) {
                        Double weightTr = (Double) dataSnapshot.child("transitions").child("bpms").child(j + "-" + k).getValue();
                        weightTr = weightTr - 1.0 / ((double) (likedTra + (clusters) * (clusters) + 11 * 11));
                        weightTr = weightTr + weightTra;
                        mFirebaseDatabaseReference.child("transitions").child("bpms").child(j + "-" + k).setValue(weightTr);
                    }

                    i++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    //Update weights
    public void updateWeightsSubtract() {
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final int liked = (int) (long) dataSnapshot.child("likedSongs").getValue();
                final int likedTra = (int) (long) dataSnapshot.child("likedTransitions").getValue();

                final int clusters = (int) dataSnapshot.child("clusters").getChildrenCount();
                weightT = 1.0 / ((double) (liked + clusters + 11 - 1));
                weightTra = 1.0 / ((double) (likedTra + (clusters - 1) * (clusters - 1) + 11 * 11));

                int i = 0;
                for (DataSnapshot postSnapshot : dataSnapshot.child("clusters").getChildren()) {
                    Double weight = (Double) postSnapshot.child("weight").getValue();
                    if (weight == null)
                        weight = 0.0;
                    weight = weight - 1.0 / ((double) (liked + clusters + 11));
                    weight = weight + weightT;
                    mFirebaseDatabaseReference.child("clusters").child(Integer.toString(i)).child("weight").setValue(weight);

                    for (int j = 0; j < dataSnapshot.child("clusters").getChildrenCount(); j++) {
                        Double weightTr = (Double) dataSnapshot.child("transitions").child("clusters").child(i + "-" + j).getValue();
                        if (weightTr == null)
                            weightTr = 0.0;
                        weightTr = weightTr - 1.0 / ((double) (likedTra + (clusters) * (clusters) + 11 * 11));
                        weightTr = weightTr + weightTra;
                        mFirebaseDatabaseReference.child("transitions").child("clusters").child(i + "-" + j).setValue(weightTr);
                    }

                    i++;
                }


                for (int j = 0; j < 11; j++) {
                    Double weight = (Double) dataSnapshot.child("bpms").child(Integer.toString(j)).child("weight").getValue();
                    weight = weight - 1.0 / ((double) (liked + clusters + 11));
                    weight = weight + weightT;
                    mFirebaseDatabaseReference.child("bpms").child(Integer.toString(j)).child("weight").setValue(weight);

                    for (int k = 0; k < dataSnapshot.child("clusters").getChildrenCount(); k++) {
                        Double weightTr = (Double) dataSnapshot.child("transitions").child("bpms").child(j + "-" + k).getValue();
                        weightTr = weightTr - 1.0 / ((double) (likedTra + (clusters) * (clusters) + 11 * 11));
                        weightTr = weightTr + weightTra;
                        mFirebaseDatabaseReference.child("transitions").child("bpms").child(j + "-" + k).setValue(weightTr);
                    }

                    i++;
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

    public native void MuslyPowerOn();

    public native void Analyze(String path);

    public native float[][] calculateSimilarity();

}



