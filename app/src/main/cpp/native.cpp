#include <jni.h>

extern "C" const char* lab_c_status();

extern "C"
JNIEXPORT jstring JNICALL
Java_com_security_lab_MainActivity_nativeStatus(
        JNIEnv* env, jobject) {
    return env->NewStringUTF(lab_c_status());
}
