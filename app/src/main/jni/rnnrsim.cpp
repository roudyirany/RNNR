#include <src/main/cpp/include/musly/musly_types.h>
#include <src/main/cpp/include/musly/musly.h>
#include "jni.h"
#include "stdlib.h"

musly_jukebox *mj=0;

extern "C" void Java_com_mr2_rnnr_Initialization_PowerOn(JNIEnv *env, jobject thiz){
    mj = musly_jukebox_poweron(NULL, NULL);
}

