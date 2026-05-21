/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

#include <jni.h>
#include <android/log.h>
#include <openssl/bio.h>
#include <openssl/cms.h>
#include <openssl/pem.h>
#include <openssl/x509.h>
#include <cstring>

#define LOG_TAG "CmsVerifier"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_nextcloud_utils_CmsSignatureVerifier_verifySignedData(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray cmsDataArray,
    jbyteArray messageDataArray,
    jobjectArray certPemArray
) {
    jsize cmsLen = env->GetArrayLength(cmsDataArray);
    jbyte* cmsBytes = env->GetByteArrayElements(cmsDataArray, nullptr);
    BIO* cmsBio = BIO_new_mem_buf(cmsBytes, static_cast<int>(cmsLen));

    jsize msgLen = env->GetArrayLength(messageDataArray);
    jbyte* msgBytes = env->GetByteArrayElements(messageDataArray, nullptr);
    BIO* dataBio = BIO_new_mem_buf(msgBytes, static_cast<int>(msgLen));

    CMS_ContentInfo* contentInfo = d2i_CMS_bio(cmsBio, nullptr);

    BIO_free(cmsBio);
    env->ReleaseByteArrayElements(cmsDataArray, cmsBytes, JNI_ABORT);

    if (contentInfo == nullptr) {
        LOGE("Failed to parse CMS content info");
        BIO_free(dataBio);
        env->ReleaseByteArrayElements(messageDataArray, msgBytes, JNI_ABORT);
        return JNI_FALSE;
    }

    int verifyResult = CMS_verify(
        contentInfo,
        nullptr,
        nullptr,
        dataBio,
        nullptr,
        CMS_DETACHED | CMS_NO_SIGNER_CERT_VERIFY
    );

    BIO_free(dataBio);
    env->ReleaseByteArrayElements(messageDataArray, msgBytes, JNI_ABORT);

    if (verifyResult != 1) {
        LOGE("CMS_verify failed");
        CMS_ContentInfo_free(contentInfo);
        return JNI_FALSE;
    }

    STACK_OF(CMS_SignerInfo)* signerInfos = CMS_get0_SignerInfos(contentInfo);
    int numSigners = sk_CMS_SignerInfo_num(signerInfos);
    jsize numCerts = env->GetArrayLength(certPemArray);
    jboolean matched = JNI_FALSE;

    for (jsize i = 0; i < numCerts && !matched; ++i) {
        auto certPem = reinterpret_cast<jstring>(env->GetObjectArrayElement(certPemArray, i));
        const char* pemChars = env->GetStringUTFChars(certPem, nullptr);

        BIO* certBio = BIO_new(BIO_s_mem());
        BIO_write(certBio, pemChars, static_cast<int>(strlen(pemChars)));
        X509* certX509 = PEM_read_bio_X509(certBio, nullptr, nullptr, nullptr);

        BIO_free(certBio);
        env->ReleaseStringUTFChars(certPem, pemChars);
        env->DeleteLocalRef(certPem);

        if (certX509 == nullptr) {
            LOGE("Failed to parse PEM certificate at index %d", i);
            continue;
        }

        for (int j = 0; j < numSigners; ++j) {
            CMS_SignerInfo* signerInfo = sk_CMS_SignerInfo_value(signerInfos, j);
            if (CMS_SignerInfo_cert_cmp(signerInfo, certX509) == 0) {
                matched = JNI_TRUE;
                break;
            }
        }

        X509_free(certX509);
    }

    CMS_ContentInfo_free(contentInfo);
    return matched;
}
