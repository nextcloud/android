/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

#include <jni.h>
#include <android/log.h>
#include <openssl/bio.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/x509.h>

#define LOG_TAG "CertificateValidator"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static constexpr size_t OPENSSL_ERROR_BUFFER_SIZE = 256;

static void logOpenSslError(const char* message) {
    char reason[OPENSSL_ERROR_BUFFER_SIZE];
    ERR_error_string_n(ERR_get_error(), reason, sizeof(reason));
    LOGE("%s: %s", message, reason);
    ERR_clear_error();
}

static EVP_PKEY* readPublicKey(JNIEnv* env, jstring publicKeyPem) {
    const char* chars = env->GetStringUTFChars(publicKeyPem, nullptr);
    BIO* bio = BIO_new_mem_buf(chars, -1);
    EVP_PKEY* publicKey = bio == nullptr ? nullptr : PEM_read_bio_PUBKEY(bio, nullptr, nullptr, nullptr);
    BIO_free(bio);
    env->ReleaseStringUTFChars(publicKeyPem, chars);
    return publicKey;
}

static X509* readCertificate(JNIEnv* env, jstring certificatePem) {
    const char* chars = env->GetStringUTFChars(certificatePem, nullptr);
    BIO* bio = BIO_new_mem_buf(chars, -1);
    X509* certificate = bio == nullptr ? nullptr : PEM_read_bio_X509(bio, nullptr, nullptr, nullptr);
    BIO_free(bio);
    env->ReleaseStringUTFChars(certificatePem, chars);
    return certificate;
}

// X509_cmp_current_time returns -1 when the time is in the past, 1 when it is in the future and 0 on error.
static bool isWithinValidityPeriod(const X509* certificate) {
    return X509_cmp_current_time(X509_get0_notBefore(certificate)) < 0 &&
           X509_cmp_current_time(X509_get0_notAfter(certificate)) > 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_owncloud_android_ui_dialog_setupEncryption_CertificateValidator_verifyCertificate(
    JNIEnv* env,
    jobject /* thiz */,
    jstring serverPublicKeyPem,
    jstring certificatePem
) {
    if (serverPublicKeyPem == nullptr || certificatePem == nullptr) {
        LOGE("Server public key or certificate is missing");
        return JNI_FALSE;
    }

    EVP_PKEY* serverPublicKey = readPublicKey(env, serverPublicKeyPem);
    if (serverPublicKey == nullptr) {
        logOpenSslError("Failed to parse server public key");
        return JNI_FALSE;
    }

    X509* certificate = readCertificate(env, certificatePem);
    if (certificate == nullptr) {
        logOpenSslError("Failed to parse certificate");
        EVP_PKEY_free(serverPublicKey);
        return JNI_FALSE;
    }

    jboolean isValid = JNI_FALSE;
    if (!isWithinValidityPeriod(certificate)) {
        LOGE("Certificate is expired or not yet valid");
    } else if (X509_verify(certificate, serverPublicKey) != 1) {
        logOpenSslError("Certificate is not signed by the server public key");
    } else {
        LOGD("Client certificate is valid against server public key");
        isValid = JNI_TRUE;
    }

    X509_free(certificate);
    EVP_PKEY_free(serverPublicKey);
    return isValid;
}
