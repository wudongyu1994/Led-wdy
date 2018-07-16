LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := Led-wdy
# no this, apk will not install successfully
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := 	android-support-v4 \
								GetuiSDK2.10.2.0 \
								commons-lang-2.5 \
								Core \
								LibFunSDK \
								stickygridheaders \
								sun.misc.BASE64Decoder \
								BASE64 \
								dewarp \
								hisdk \
								HiPhotoView \
								bugly_crash_release \
								glide-3.6.1 \
								jg_filter_sdk_1.1 \
								mid-core-sdk-3.7.2 \
								nineoldandroids-2.4.0 \
								org.apache.http.legacy \
								wup-1.0.0.E-SNAPSHOT \
								zxing3.0

LOCAL_JNI_SHARED_LIBRARIES := 	libgetuiext2 \
								libFunSDK \
								libeznat \
								libh264tomp4 \
								libHiChipAndroid \
								libHiChipP2P \
								libHiPushLib \
								libhisdkqos \
								libBugly \
								libEncMp4 \
								libavcodec-56 \
								libavdevice-56 \
								libavfilter-5 \
								libavformat-56 \
								libavutil-54 \
								libh264decoder \
								libsinvoice_no_sign \
								libswresample-1 \
								libswscale-3 \
								libtpnsSecurity \
								libxguardian

#can't use this, or Imycanservice can't be compiled
#LOCAL_SDK_VERSION := current


include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := android-support-v4:libs/android-support-v4.jar \
										GetuiSDK2.10.2.0:libs/GetuiSDK2.10.2.0.jar \
										commons-lang-2.5:libs/commons-lang-2.5.jar \
										Core:libs/Core.jar \
										LibFunSDK:libs/LibFunSDK.jar \
										stickygridheaders:libs/stickygridheaders.jar \
										sun.misc.BASE64Decoder:libs/sun.misc.BASE64Decoder.jar \
										BASE64:libs/BASE64.jar \
										dewarp:libs/dewarp.jar \
										hisdk:hisdk.jar \
										HiPhotoView:libs/HiPhotoView.jar \
										bugly_crash_release:libs/bugly_crash_release.jar \
										glide-3.6.1:libs/glide-3.6.1.jar \
										jg_filter_sdk_1.1:libs/jg_filter_sdk_1.1.jar \
										mid-core-sdk-3.7.2:libs/mid-core-sdk-3.7.2.jar \
										nineoldandroids-2.4.0:libs/nineoldandroids-2.4.0.jar \
										org.apache.http.legacy:libs/org.apache.http.legacy.jar \
										wup-1.0.0.E-SNAPSHOT:libs/wup-1.0.0.E-SNAPSHOT.jar \
										zxing3.0:libs/zxing3.0.jar

#include $(BUILD_MULTI_PREBUILT)

#include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := 	libgetuiext2:libs/armeabi-v7a/libgetuiext2.so \
						libFunSDK:libs/armeabi/libFunSDK.so \
						libeznat:libs/armeabi/libeznat.so \
						libh264tomp4:libs/armeabi/libh264tomp4.so \
						libHiChipAndroid:libs/armeabi/libHiChipAndroid.so \
						libHiChipP2P:libs/armeabi/libHiChipP2P.so \
						libHiPushLib:libs/armeabi/libHiPushLib.so \
						libhisdkqos:libs/armeabi/libhisdkqos.so \
						libBugly:libs/armeabi/libBugly.so \
						libEncMp4:libs/armeabi/libEncMp4.so \
						libavcodec-56:libs/armeabi/libavcodec-56.so \
						libavdevice-56:libs/armeabi/libavdevice-56.so \
						libavfilter-5:libs/armeabi/libavfilter-5.so \
						libavformat-56:libs/armeabi/libavformat-56.so \
						libavutil-54:libs/armeabi/libavutil-54.so \
						libh264decoder:libs/armeabi/libh264decoder.so \
						libsinvoice_no_sign:libs/armeabi/libsinvoice_no_sign.so \
						libswresample-1:libs/armeabi/libswresample-1.so \
						libswscale-3:libs/armeabi/libswscale-3.so \
						libtpnsSecurity:libs/armeabi/libtpnsSecurity.so \
						libxguardian:libs/armeabi/libxguardian.so

LOCAL_MODULE_TAGS := optional 
include $(BUILD_MULTI_PREBUILT)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
