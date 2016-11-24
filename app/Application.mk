APP_OPTIM := release
APP_PLATFORM := android-21
APP_ABI := armeabi-v7a x86 armeabi arm64-v8a X86_64
APP_STL := gnustl_static
# use this to select gcc instead of clang
NDK_TOOLCHAIN_VERSION := 4.9


# then enable c++11 extentions in source code
APP_CPPFLAGS += -std=c++0x
# or use APP_CPPFLAGS := -std=gnu++11