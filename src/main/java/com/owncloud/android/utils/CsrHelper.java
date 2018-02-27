package com.owncloud.android.utils;

import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.bc.BcRSAContentSignerBuilder;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.IOException;
import java.security.KeyPair;

/**
 * copied & modified from:
 * https://github.com/awslabs/aws-sdk-android-samples/blob/master/CreateIotCertWithCSR/src/com/amazonaws/demo/csrcert/CsrHelper.java
 * accessed at 31.08.17
 * Original parts are licensed under the Apache License, Version 2.0: http://aws.amazon.com/apache2.0
 * Own parts are licensed unter GPLv3+.
 */

public class CsrHelper {

    /**
     * Generate CSR with PEM encoding
     *
     * @param keyPair the KeyPair with private and public keys
     * @param userId  userId of CSR owner
     * @return PEM encoded CSR string
     * @throws IOException               thrown if key cannot be created
     * @throws OperatorCreationException thrown if contentSigner cannot be build
     */
    public static String generateCsrPemEncodedString(KeyPair keyPair, String userId)
            throws IOException, OperatorCreationException {
        PKCS10CertificationRequest csr = CsrHelper.generateCSR(keyPair, userId);
        byte[] derCSR = csr.getEncoded();
        return "-----BEGIN CERTIFICATE REQUEST-----\n" + android.util.Base64.encodeToString(derCSR,
                android.util.Base64.NO_WRAP) + "\n-----END CERTIFICATE REQUEST-----";
    }
    
    /**
     * Create the certificate signing request (CSR) from private and public keys
     *
     * @param keyPair the KeyPair with private and public keys
     * @param userId userId of CSR owner
     * @return PKCS10CertificationRequest with the certificate signing request (CSR) data
     * @throws IOException thrown if key cannot be created
     * @throws OperatorCreationException thrown if contentSigner cannot be build
     */
    private static PKCS10CertificationRequest generateCSR(KeyPair keyPair, String userId) throws IOException,
    OperatorCreationException {
        String principal = "CN=" + userId;
        AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
        AlgorithmIdentifier signatureAlgorithm = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WITHRSA");
        AlgorithmIdentifier digestAlgorithm = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-1");
        ContentSigner signer = new BcRSAContentSignerBuilder(signatureAlgorithm, digestAlgorithm).build(privateKey);

        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(new X500Name(principal),
                keyPair.getPublic());
        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
        extensionsGenerator.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensionsGenerator.generate());

        return csrBuilder.build(signer);
    }
}
