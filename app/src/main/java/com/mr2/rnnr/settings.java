package com.mr2.rnnr;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

public class settings extends Fragment{

    private TextView calibrate;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;

    public settings() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        mFirebaseAuth = FirebaseAuth.getInstance();

        calibrate = (TextView) getView().findViewById(R.id.calibrate);
        calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().stopService(new Intent(getActivity(),MusicService.class));
                Intent intent = new Intent(getActivity(), Calibration.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }
}
