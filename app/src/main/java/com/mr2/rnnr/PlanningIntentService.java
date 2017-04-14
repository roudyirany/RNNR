package com.mr2.rnnr;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by roudyirany on 4/14/17.
 */


public class PlanningIntentService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    Context context;

    public PlanningIntentService() {
        super("PlanningIntentService");
    }

    public PlanningIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context = this;

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users/" + mFirebaseUser.getUid());

        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<Integer> BestTrajectory = new ArrayList<Integer>();
                int upperMedian = ((int) (long) dataSnapshot.child("clusters").getChildrenCount())/2;
                ArrayList<Double> RsValues = new ArrayList<Double>();
                ArrayList<Integer> upperMedianClusters = new ArrayList<Integer>();
                double maxPayoff = 0.0;

                for(DataSnapshot postSnapshot: dataSnapshot.child("clusters").getChildren()){
                    double weightC = (double) postSnapshot.child("weight").getValue();
                    int bpm = quantizeBPM((int) (long) dataSnapshot.child("library").child(postSnapshot.child("parent").getValue().toString()).child("bpm").getValue());
                    double weightB = (double) dataSnapshot.child("bpms").child(Integer.toString(bpm)).child("weight").getValue();
                    double Rs = weightC + weightB;

                    int position = RsValues.size() - 1;
                    if(position == -1)
                        position = 0;


                    for(int i=0; i<RsValues.size();i++){
                        if(RsValues.get(i)<Rs)
                            position = i;
                    }

                    if(position < upperMedian && ((RsValues.size() +1) <upperMedian)){
                        RsValues.add(position, Rs);
                        upperMedianClusters.add(position, Integer.parseInt(postSnapshot.getKey().toString()));
                    }
                }


                for(int i=0; i<(upperMedian*upperMedian*upperMedian);i++) {
                    ArrayList<Integer> Trajectory = new ArrayList<Integer>();
                    ArrayList<Integer> BPMs = new ArrayList<Integer>();
                    double expectedPayoff = 0.0;
                    for (int j = 0; j < 3; j++) {

                        Random rand = new Random();
                        int n = rand.nextInt(upperMedianClusters.size() - 1);
                        int cluster = upperMedianClusters.get(n);

                        while(Trajectory.contains(cluster)){
                            n = rand.nextInt(upperMedianClusters.size() - 1);
                            cluster = upperMedianClusters.get(n);
                        }

                        Trajectory.add(cluster);

                        int bpm = ((int) (long) dataSnapshot.child("library").child(dataSnapshot.child("clusters").child(Integer.toString(cluster)).child("parent").getValue().toString()).child("bpm").getValue());

                        BPMs.add(quantizeBPM(bpm));
                    }

                    for(int k=0; k<3; k++){
                        expectedPayoff = expectedPayoff + RsValues.get(upperMedianClusters.indexOf(Trajectory.get(k)));
                    }

                    double RtC = ((double)dataSnapshot.child("transitions").child("clusters").child(Trajectory.get(0)+"-"+Trajectory.get(1)).getValue()) + ((double)dataSnapshot.child("transitions").child("clusters").child(Trajectory.get(1)+"-"+Trajectory.get(2)).getValue());
                    double RtB = ((double)dataSnapshot.child("transitions").child("bpms").child(BPMs.get(0)+"-"+BPMs.get(1)).getValue()) + ((double)dataSnapshot.child("transitions").child("bpms").child(BPMs.get(1)+"-"+BPMs.get(2)).getValue());
                    expectedPayoff = expectedPayoff + RtC + RtB;

                    if(expectedPayoff > maxPayoff){
                        maxPayoff = expectedPayoff;
                        BestTrajectory = new ArrayList<Integer>(Trajectory);
                    }

                }

                randomSong(BestTrajectory.get(0));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Quantize bpm
    public int quantizeBPM(int i){
        if (i < 50)
            return 0;
        else if (i < 55)
            return 1;
        else if (i < 60)
            return 2;
        else if (i < 70)
            return 3;
        else if (i < 85)
            return 4;
        else if (i < 100)
            return 5;
        else if (i < 115)
            return 6;
        else if (i < 140)
            return 7;
        else if (i < 150)
            return 8;
        else if (i < 170)
            return 9;
        else
            return 10;
    }

    //Select random 3 songs
    public void randomSong(int cluster){
        Query query = mFirebaseDatabaseReference.child("library").orderByChild("cluster").equalTo(cluster);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            String nextSong;

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = (int) (long) dataSnapshot.getChildrenCount();
                int n;
                if(count > 1) {
                    Random rand = new Random();
                    n = rand.nextInt(count - 1);
                }

                else n = 0;

                int i=0;
                for(DataSnapshot postSnapshot: dataSnapshot.getChildren()){
                    if(i == n)
                        nextSong = postSnapshot.getKey().toString();
                    else i++;
                }

                Intent RTReturn = new Intent(MainMenu.RECEIVE_SONG);
                RTReturn.putExtra("songTitle", nextSong);
                LocalBroadcastManager.getInstance(context).sendBroadcast(RTReturn);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
