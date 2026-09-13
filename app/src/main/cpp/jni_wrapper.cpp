// JNI glue exposing the X11 engine to the Android app.
//
// Hashing is performed entirely inside the native loop below so that the
// hot path never crosses the JNI boundary per hash. Kotlin only drives
// the miner in chunks and handles share submission / hashrate bookkeeping.
#include <jni.h>
#include <cstring>

#include "x11hash.h"

namespace {

// Block header is always 80 bytes for X11 coins (Dash/Bitcoin lineage).
constexpr jint kHeaderSize = 80;
// The digest written by the sph chain is 32 bytes, big-endian.
constexpr jint kDigestSize = 32;

} // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_altomedia_mineplus_crypto_X11Engine_x11_1hash_1native(
        JNIEnv *env, jclass, jbyteArray input) {
    const jsize len = env->GetArrayLength(input);
    jbyte *bytes = env->GetByteArrayElements(input, nullptr);
    if (bytes == nullptr) {
        return nullptr;
    }

    unsigned char output[32];
    x11_hash(reinterpret_cast<unsigned char *>(bytes),
             static_cast<size_t>(len), output);

    env->ReleaseByteArrayElements(input, bytes, JNI_ABORT);

    jbyteArray result = env->NewByteArray(32);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, 32, reinterpret_cast<jbyte *>(output));
    }
    return result;
}

/**
 * Mines a contiguous range of nonces in native code.
 *
 * Kotlin hands in the 80-byte header template, the 32-byte target in
 * big-endian order and a range of nonces. The loop runs entirely in C:
 *
 *   header[76..79] = nonce (little-endian)
 *   digest = x11(header)
 *   found  = digest <= target
 *
 * Every share found triggers the Java callback
 * `(nonce: Long, digest: ByteArray)`.
 *
 * @return the number of hashes actually performed (usually equals count,
 *         fewer only if the range ran out).
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_altomedia_mineplus_miner_NativeMiner_mineNative(
        JNIEnv *env, jclass,
        jbyteArray headerArr, jbyteArray targetArr,
        jlong nonceStart, jlong nonceCount,
        jobject shareCallback) {

    jbyte *headerBytes = env->GetByteArrayElements(headerArr, nullptr);
    jbyte *targetBytes = env->GetByteArrayElements(targetArr, nullptr);
    if (headerBytes == nullptr || targetBytes == nullptr) {
        if (headerBytes) env->ReleaseByteArrayElements(headerArr, headerBytes, JNI_ABORT);
        if (targetBytes) env->ReleaseByteArrayElements(targetArr, targetBytes, JNI_ABORT);
        return 0;
    }

    const jsize headerLen = env->GetArrayLength(headerArr);
    unsigned char block[kHeaderSize];
    std::memset(block, 0, sizeof(block));
    std::memcpy(block, headerBytes,
                headerLen < kHeaderSize ? headerLen : kHeaderSize);

    const unsigned char *target =
            reinterpret_cast<const unsigned char *>(targetBytes);

    jclass callbackClass = env->GetObjectClass(shareCallback);
    jmethodID onShare =
            env->GetMethodID(callbackClass, "onShareFound", "(J[B)V");
    if (onShare == nullptr) {
        env->ReleaseByteArrayElements(headerArr, headerBytes, JNI_ABORT);
        env->ReleaseByteArrayElements(targetArr, targetBytes, JNI_ABORT);
        env->DeleteLocalRef(callbackClass);
        return 0;
    }

    jlong limit = nonceStart + nonceCount;
    jlong done = 0;
    unsigned char digest[kDigestSize];

    for (jlong nonce = nonceStart; nonce < limit; ++nonce) {
        uint32_t n = static_cast<uint32_t>(nonce);
        block[76] = static_cast<unsigned char>(n & 0xFF);
        block[77] = static_cast<unsigned char>((n >> 8) & 0xFF);
        block[78] = static_cast<unsigned char>((n >> 16) & 0xFF);
        block[79] = static_cast<unsigned char>((n >> 24) & 0xFF);

        x11_hash(block, kHeaderSize, digest);

        // Share accepted when the digest is numerically <= target.
        if (std::memcmp(digest, target, kDigestSize) <= 0) {
            jbyteArray hashArr = env->NewByteArray(kDigestSize);
            env->SetByteArrayRegion(
                    hashArr, 0, kDigestSize,
                    reinterpret_cast<jbyte *>(digest));
            env->CallVoidMethod(shareCallback, onShare,
                                static_cast<jlong>(nonce), hashArr);
            env->DeleteLocalRef(hashArr);
        }
        ++done;
    }

    env->ReleaseByteArrayElements(headerArr, headerBytes, JNI_ABORT);
    env->ReleaseByteArrayElements(targetArr, targetBytes, JNI_ABORT);
    env->DeleteLocalRef(callbackClass);
    return done;
}