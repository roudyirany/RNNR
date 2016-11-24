#include <src/main/cpp/include/musly/musly_types.h>
#include <src/main/cpp/include/musly/musly.h>
#include "jni.h"
#include "stdlib.h"

extern "C" jobjectArray Java_com_mr2_rnnr_Initialization_Musly(JNIEnv *env, jobject thiz, jobjectArray songPaths){
    int stringCount = env->GetArrayLength(songPaths);
    const char *paths[stringCount];

    //Extract paths into array of strings
    for (int i=0; i<stringCount; i++) {
        jstring string = (jstring) (env->GetObjectArrayElement(songPaths, i));
        paths[i] = env->GetStringUTFChars(string, 0);
    }

    musly_jukebox *mj = 0;
    mj = musly_jukebox_poweron(NULL, NULL);
    musly_track *tracks[stringCount];
    musly_trackid trackids[stringCount];

    int ref;

    //Analyze each audio file
    for (int i=0; i<stringCount; i++){
        tracks[i] = musly_track_alloc(mj);
        musly_track_analyze_audiofile(mj, paths[i],15, -48, tracks[i]);
    }

   //Set Musly jukebox music style
    if(stringCount <=1000)
        musly_jukebox_setmusicstyle(mj,tracks,stringCount);
    else{
        musly_track *tracks1000[1000];
        for(int i=0; i<1000; i++) {
            tracks1000[i] = tracks[i];
        }
        musly_jukebox_setmusicstyle(mj,tracks1000,1000);
    }

    //Add tracks to Musly jukebox
    musly_jukebox_addtracks(mj,tracks,trackids,stringCount,true);


    jclass floatClass = env->FindClass("[F"); //
    jsize height = stringCount;

    // Create the returnable 2D array
    jobjectArray jObjarray = env->NewObjectArray(height, floatClass, NULL);

    //Calculate similarity of tracks
    for(int i=0; i<stringCount; i++){
        float similarities[stringCount];
        musly_jukebox_similarity(mj,tracks[i],trackids[i],tracks,trackids,stringCount, similarities);
        jfloatArray floatArray = env->NewFloatArray(stringCount);
        env->SetFloatArrayRegion(floatArray, (jsize) 0, (jsize) stringCount, (jfloat*) similarities);
        env->SetObjectArrayElement(jObjarray, (jsize) i, floatArray);
        env->DeleteLocalRef(floatArray);
    }

    musly_jukebox_poweroff(mj);

    return jObjarray;
}