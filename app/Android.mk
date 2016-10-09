LOCAL_PATH := $(call my-dir)
       include $(CLEAR_VARS)
       LOCAL_MODULE := superpowered
       LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/cpp/libs/$(TARGET_ARCH_ABI)/Superpowered.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/include
       include $(PREBUILT_STATIC_LIBRARY)
       include $(CLEAR_VARS)
       LOCAL_MODULE    := bpm_analyzer
       LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/jni/bpm-analyzer.cpp
       LOCAL_STATIC_LIBRARIES := superpowered
       LOCAL_LDLIBS := -llog
       include $(BUILD_SHARED_LIBRARY)