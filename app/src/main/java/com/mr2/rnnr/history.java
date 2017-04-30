package com.mr2.rnnr;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class history extends Fragment{

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;

    private LinearLayout historyLayout;

    public history() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(mFirebaseUser.getUid());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.history, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        historyLayout = (LinearLayout) getView().findViewById(R.id.history);

        mFirebaseDatabaseReference.child("history").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                historyLayout.removeAllViews();

                if(dataSnapshot.getValue() != null) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        TextView date = new TextView(getActivity());
                        date.setText(postSnapshot.child("date").getValue().toString());
                        date.setTextColor(Color.BLACK);
                        date.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                        date.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        historyLayout.addView(date);

                        TextView summary = new TextView(getActivity());
                        String distance = postSnapshot.child("distance").getValue().toString();
                        String time = postSnapshot.child("time").getValue().toString();
                        String speed = postSnapshot.child("speed").getValue().toString();

                        summary.setText("Distance: " + distance + " km\nTime: " + time + " hours\nSpeed: " + speed + " km/h");

                        if (Build.VERSION.SDK_INT < 23)
                            summary.setTextAppearance(getContext(), R.style.divStyle);
                        else
                            summary.setTextAppearance(R.style.divStyle);

                        summary.setBackground(getResources().getDrawable(R.drawable.shape1));
                        summary.setPadding(20, 0, 0, 0);
                        summary.setGravity(Gravity.CENTER_VERTICAL);

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 250);
                        lp.setMargins(0, 0, 0, 30);
                        summary.setLayoutParams(lp);

                        historyLayout.addView(summary);
                    }
                }

                else{
                    TextView date = new TextView(getActivity());
                    date.setText("No workout history saved yet. Get running!");
                    date.setTextColor(Color.BLACK);
                    date.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
                    date.setPadding(0,20,0,0);
                    date.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    historyLayout.addView(date);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
