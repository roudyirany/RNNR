LOCAL_PATH := $(call my-dir)
       include $(CLEAR_VARS)
       LOCAL_MODULE := superpowered
       LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/cpp/Superpowered/lib/$(TARGET_ARCH_ABI)/Superpowered.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/Superpowered/include
       include $(PREBUILT_STATIC_LIBRARY)
       include $(CLEAR_VARS)
       LOCAL_MODULE    := bpm_analyzer
       LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/jni/bpm-analyzer.cpp
       LOCAL_STATIC_LIBRARIES := superpowered
       LOCAL_LDLIBS := -llog
       include $(BUILD_SHARED_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libavdevice
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libavdevice.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libavcodec
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libavcodec.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libavformat
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libavformat.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libswscale
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libswscale.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libavutil
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libavutil.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libavfilter
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libavfilter.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE:= libswresample
       LOCAL_SRC_FILES:= $(LOCAL_PATH)/src/main/cpp/FFmpeg/lib/$(TARGET_ARCH_ABI)/libswresample.a
       LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/FFmpeg/include
       include $(PREBUILT_STATIC_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE := kissfft
       LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/cpp/libmusly/kissfft/kiss_fft.c
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/kissfft/kiss_fftr.c
       LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/libmusly/kissfft
       LOCAL_LDLIBS := -llog
       include $(BUILD_SHARED_LIBRARY)

        include $(CLEAR_VARS)
        LOCAL_MODULE := resample
        LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/cpp/libmusly/libresample/filterkit.c
        LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/libresample/resamplesubs.c
        LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/libresample/resample.c
        LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/libmusly/libresample
        LOCAL_LDLIBS := -llog
        include $(BUILD_SHARED_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE    := musly
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/methods/mandelellis.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/methods/timbre.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/decoders/libav.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/resampler.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/plugins.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/method.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/decoder.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/windowfunction.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/powerspectrum.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/melspectrum.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/discretecosinetransform.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/mfcc.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/gaussianstatistics.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/mutualproximity.cpp
       LOCAL_SRC_FILES += $(LOCAL_PATH)/src/main/cpp/libmusly/lib.cpp
       LOCAL_STATIC_LIBRARIES := libavcodec libavformat libavutil kissfft resample lswresample
       LOCAL_C_INCLUDES := $(LOCAL_PATH)/src/main/cpp/
       LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/include
       LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/libmusly
       LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/libmusly/methods
       LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/libmusly/decoders
       LOCAL_C_INCLUDES += $(LOCAL_PATH)/src/main/cpp/include/musly
       LOCAL_LDLIBS := -llog -lm -lz
       LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
       include $(BUILD_SHARED_LIBRARY)

       include $(CLEAR_VARS)
       LOCAL_MODULE := rnnrsim
       LOCAL_SRC_FILES := $(LOCAL_PATH)/src/main/jni/rnnrsim.cpp
       LOCAL_STATIC_LIBRARIES := musly
       LOCAL_LDLIBS := -llog
       include $(BUILD_SHARED_LIBRARY)