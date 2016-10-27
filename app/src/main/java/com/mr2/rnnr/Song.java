package com.mr2.rnnr;

/**
 * Created by roudyirany on 10/10/16.
 */

public class Song {
    private String title;
    private String path;
    private int cluster;

    public Song(){
        title=path=null;
    }

    public Song( String songTitle, String songPath) {
        title=songTitle;
        path=songPath;
    }

    public String getTitle(){return title;}
    public String getPath(){return path;}
}
