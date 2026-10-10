#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" int lab_walk_files(const char* root, void (*cb)(const char*));

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "SecLab", __VA_ARGS__)

static void cb(const char* path) {
    // Логирование обработанного файла; шифрование выполняется в Java-слое
    LOG("walk: %s", path);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_security_lab_MainActivity_nativeWalkFiles(JNIEnv* env, jobject, jstring jroot) {
    const char* root = env->GetStringUTFChars(jroot, nullptr);
    int n = lab_walk_files(root, cb);
    env->ReleaseStringUTFChars(jroot, root);
    return n;
}
