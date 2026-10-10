#include <jni.h>
#include <string>
extern "C" int lab_walk_files(const char* root, void (*cb)(const char*));
static void cb(const char*) {}
extern "C" JNIEXPORT jint JNICALL
Java_com_security_lab_MainActivity_nativeWalkFiles(JNIEnv* env, jobject, jstring j) {
    const char* r = env->GetStringUTFChars(j, nullptr);
    int n = lab_walk_files(r, cb);
    env->ReleaseStringUTFChars(j, r);
    return n;
}
