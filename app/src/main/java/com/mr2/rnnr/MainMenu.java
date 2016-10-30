package com.mr2.rnnr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.Toast;

public class MainMenu extends AppCompatActivity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableList);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataHeader.add("Beginner");
        listDataHeader.add("Walk-Jog");
        listDataHeader.add("Sprints");
        listDataHeader.add("Pyramid Intervals");
        listDataHeader.add("Quickie");
        listDataHeader.add("Fat Burner");
        listDataHeader.add("Custom");

        // Adding child data
        List<String> Beginner = new ArrayList<String>();
        Beginner.add("30 minutes");
        Beginner.add("40 minutes");

        List<String> walkJog = new ArrayList<String>();
        walkJog.add("42 minutes");
        walkJog.add("60 minutes");

        List<String> Sprints = new ArrayList<String>();
        Sprints.add("30 minutes");
        Sprints.add("60 minutes");

        List<String> Pyramids = new ArrayList<String>();
        Pyramids.add("25 minutes");
        Pyramids.add("30 minutes");
        Pyramids.add("60 minutes");

        List<String> Quickie = new ArrayList<String>();
        Quickie.add("20 minutes");

        List<String> fatBurner = new ArrayList<String>();
        fatBurner.add("45 minutes");

        List<String> custom = new ArrayList<String>();
        custom.add("Tailor your own workout");

        listDataChild.put(listDataHeader.get(0), Beginner); // Header, Child data
        listDataChild.put(listDataHeader.get(1), walkJog);
        listDataChild.put(listDataHeader.get(2), Sprints);
        listDataChild.put(listDataHeader.get(3), Pyramids);
        listDataChild.put(listDataHeader.get(4), Quickie);
        listDataChild.put(listDataHeader.get(5), fatBurner);
        listDataChild.put(listDataHeader.get(6), custom);
    }
}