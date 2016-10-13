package com.mr2.rnnr;

/**
 * Created by roudyirany on 10/10/16.
 */

public class Song {
    private String title;
    private String artist;
    private String path;
    private int bpm;
    private int cluster;
    private float rating;

    public Song(){
        bpm=cluster=0;
        rating = 0;
        title=artist=path=null;
    }

    public Song( String songTitle, String songArtist, String songPath) {
        title=songTitle;
        artist=songArtist;
        path=songPath;
        bpm=0;
        cluster=0;
        rating=0;
    }

    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getPath(){return path;}
    public int getBpm(){return bpm;}
    public void setBpm(int a){bpm = a;}
    public int getCluster(){return cluster;}
    public void setCluster(int a){cluster = a;}
    public float getRating(){return rating;}
}
