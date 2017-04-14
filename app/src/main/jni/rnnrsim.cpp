#include <src/main/cpp/include/musly/musly_types.h>
#include <src/main/cpp/include/musly/musly.h>
#include "jni.h"
#include "stdlib.h"
#include <vector>

musly_jukebox *mj=0;
std::vector<musly_track*> tracks;
musly_trackid *trackids;

extern "C" void Java_com_mr2_rnnr_Initialization_MuslyPowerOn(JNIEnv *env, jobject thiz){
    mj = musly_jukebox_poweron(NULL, NULL);
}

extern "C" void Java_com_mr2_rnnr_Initialization_Analyze(JNIEnv *env, jobject thiz, jstring songpath){
    const char *path = env->GetStringUTFChars(songpath , NULL ) ;

    musly_track *track;
    track = musly_track_alloc(mj);

    musly_track_analyze_audiofile(mj, path, 30, 60, track);
    tracks.push_back(track);
}

extern "C" jobjectArray Java_com_mr2_rnnr_Initialization_calculateSimilarity(JNIEnv *env, jobject thiz){
    //Set Musly jukebox music style
    if(tracks.size() <=1000)
        musly_jukebox_setmusicstyle(mj,tracks.data(),tracks.size());
    else{
        musly_track *tracks1000[1000];
        for(int i=0; i<1000; i++) {
            tracks1000[i] = tracks[i];
        }
        musly_jukebox_setmusicstyle(mj,tracks1000,1000);
    }

    trackids = new musly_trackid[tracks.size()];

    //Add tracks to Musly jukebox
    musly_jukebox_addtracks(mj,tracks.data(),trackids,tracks.size(),true);

    jclass floatClass = env->FindClass("[F"); //
    jsize height = tracks.size();

    // Create the returnable 2D array
    jobjectArray jObjarray = env->NewObjectArray(height, floatClass, NULL);

    //Calculate similarity of tracks
    for(int i=0; i<tracks.size(); i++){
        float similarities[tracks.size()];
        musly_jukebox_similarity(mj,tracks[i],trackids[i],tracks.data(),trackids,tracks.size(), similarities);
        jfloatArray floatArray = env->NewFloatArray(tracks.size());
        env->SetFloatArrayRegion(floatArray, (jsize) 0, (jsize) tracks.size(), (jfloat*) similarities);
        env->SetObjectArrayElement(jObjarray, (jsize) i, floatArray);
        env->DeleteLocalRef(floatArray);
    }

    musly_jukebox_poweroff(mj);

    for(int i=0; i<tracks.size(); i++)
        musly_track_free(tracks[i]);

    return jObjarray;
}
