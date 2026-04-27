/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedUser
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptionUtilsMetadataVerificationTests {

    private val sut = EncryptionUtilsV2()

    @Test
    fun testVerifyMetadataWhenGivenValidInputsShouldReturnTrue() {
        val metadata = EncryptedFolderMetadataFile(
            metadata =
                EncryptedMetadata(
                    authenticationTag = "xkVxj0NbQEXIEMlulYZJgg==",
                    nonce = "HzRiseUfoFJ5lqUi",
                    ciphertext = "EOnzuyVn9R8qDUBY4yeuJbhQdkOHBMy3nyRGwY0y/+oWctV17XvE0RIbOhH7+smKV3orJKatu5fG6iIZN+HZUQASTCdQ0mdFVPJmdk20UH5nFZ/ilQIyyXAFhLHdYwWA/M7wKYoh5W9fDXNX9cZvHgjWPdT9Pq99PUv37atYxj7Je25GenbtxkVxj0NbQEXIEMlulYZJgg=="
                ),
            users = listOf(
                EncryptedUser(
                    userId = "admin",
                    certificate = "-----BEGIN CERTIFICATE-----\nMIIC7jCCAdagAwIBAgIBADANBgkqhkiG9w0BAQUFADAQMQ4wDAYDVQQDEwVhZG1p\nbjAeFw0yNjA0MjcwNzI5NDdaFw00NjA0MjIwNzI5NDdaMBAxDjAMBgNVBAMTBWFk\nbWluMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwE513/kdkIp+Z5pI\n7rq1UKV5LBiB6dl4Wh46nI3mhVacOA1dJJWIUxRkkrUNWJewe8eJ7QWmhSpeBauA\n06PrAOTd1ZA4gSUWKpsYJqKm5Nxjp+BUMK1nHGQCkNQWjRllhyKTJeG/9PPc0ZrJ\nh4V27bCXC9iX5l/ve35fp99VR4tQ947HWObe07EdPIEFNYfT/IurPwMySZ1WH1gy\nDTw7IxMXcPVg+GUlfBoSVgQ1UdCkvgHc9pE1LuFmyBguAzGLbXDfspUuTs85RLGX\nGYdv2vZU/R2kJEs3ePMtaGXw6DVSx82RkPFVaLDCdShX24yk6gLNEv9oTUXY40i3\nn8njRQIDAQABo1MwUTAdBgNVHQ4EFgQUDE381mprCEvSLaFeOwZRliBSJnwwHwYD\nVR0jBBgwFoAUDE381mprCEvSLaFeOwZRliBSJnwwDwYDVR0TAQH/BAUwAwEB/zAN\nBgkqhkiG9w0BAQUFAAOCAQEAgU0o8Qp5wn3vkcQLYao2heWKsbYYl8wqkztRVVKb\n+qMe2m/FOOMK1Rxv/anEVHHN+SnTc481fHd8z3w6II28LxJ5M+IxzoAFTj6gCv8+\nrL1R9kE91401d1+ulAiJR92ykOcB1h8bk5yoCZSRLIXwViCGUrbC3iu2NLWQDYk4\nvjxwqCSJOWUQh+qaYGCjB6mgkBMAnXGJCN2fV7sAR7N8Hy7Yh5jvuQOgY574FSoS\nuKCMGJZ6ecJlw+rB5pqanlLS9+HNnQ655/gTYgVBJClFClh4nwdPHtpyTySwgx1V\nr3VDvglfnZM+gD/D2d9nTLIlT3MZqhGOIkxKvpVVkdJKzg==\n-----END CERTIFICATE-----",
                    encryptedMetadataKey = "coawvmhMoAl3iL5okD7K4a4au0Jt0SqUXp6pHP8WD1YTOemFVPsz+ts7TD5kB7ha6Ja3tLdGMq76LP/d2/pbHUiKBd6rytUo6ioHsNmmlTGHAlk9VTDY9fcvtVgkNzy7qyXvsdsUn0gBQ18l526J/bt1uRlClYNKvaEnIh2l3B8X58pzNZqhAKNI7z7WRDbXOVskr4rnqWr2ExBeaZgFwo5nNi9yiqpckICb1S2qwuZJbItqZ8VR2bOG+WpCMwrgcE5UJ6ZvaKLREfmR+qoYYB1oyUuy78eA+sDa3rO5bSgs/9I/cli1b3lZ8JFfgHXRiUYUmBcxZOmUE2IfRSHFTA=="
                )
            ),
            version = "2.0",
            filedrop = mutableMapOf()
        )
        val message = EncryptionUtils.serializeJSON(metadata, true)


        val cert = """
            -----BEGIN CERTIFICATE-----
            MIIC7jCCAdagAwIBAgIBADANBgkqhkiG9w0BAQUFADAQMQ4wDAYDVQQDEwVhZG1p
            bjAeFw0yNjA0MjcwNzI5NDdaFw00NjA0MjIwNzI5NDdaMBAxDjAMBgNVBAMTBWFk
            bWluMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwE513/kdkIp+Z5pI
            7rq1UKV5LBiB6dl4Wh46nI3mhVacOA1dJJWIUxRkkrUNWJewe8eJ7QWmhSpeBauA
            06PrAOTd1ZA4gSUWKpsYJqKm5Nxjp+BUMK1nHGQCkNQWjRllhyKTJeG/9PPc0ZrJ
            h4V27bCXC9iX5l/ve35fp99VR4tQ947HWObe07EdPIEFNYfT/IurPwMySZ1WH1gy
            DTw7IxMXcPVg+GUlfBoSVgQ1UdCkvgHc9pE1LuFmyBguAzGLbXDfspUuTs85RLGX
            GYdv2vZU/R2kJEs3ePMtaGXw6DVSx82RkPFVaLDCdShX24yk6gLNEv9oTUXY40i3
            n8njRQIDAQABo1MwUTAdBgNVHQ4EFgQUDE381mprCEvSLaFeOwZRliBSJnwwHwYD
            VR0jBBgwFoAUDE381mprCEvSLaFeOwZRliBSJnwwDwYDVR0TAQH/BAUwAwEB/zAN
            BgkqhkiG9w0BAQUFAAOCAQEAgU0o8Qp5wn3vkcQLYao2heWKsbYYl8wqkztRVVKb
            +qMe2m/FOOMK1Rxv/anEVHHN+SnTc481fHd8z3w6II28LxJ5M+IxzoAFTj6gCv8+
            rL1R9kE91401d1+ulAiJR92ykOcB1h8bk5yoCZSRLIXwViCGUrbC3iu2NLWQDYk4
            vjxwqCSJOWUQh+qaYGCjB6mgkBMAnXGJCN2fV7sAR7N8Hy7Yh5jvuQOgY574FSoS
            uKCMGJZ6ecJlw+rB5pqanlLS9+HNnQ655/gTYgVBJClFClh4nwdPHtpyTySwgx1V
            r3VDvglfnZM+gD/D2d9nTLIlT3MZqhGOIkxKvpVVkdJKzg==
            -----END CERTIFICATE-----
        """.trimIndent()
        val certs = listOf(EncryptionUtils.convertCertFromString(cert))
        val signature = """
            MIIE1wYJKoZIhvcNAQcCoIIEyDCCBMQCAQExDzANBglghkgBZQMEAgEFADALBgkqhkiG9w0BBwGgggLyMIIC7jCCAdagAwIBAgIBADANBgkqhkiG9w0BAQUFADAQMQ4wDAYDVQQDEwVhZG1pbjAeFw0yNjA0MjcwNzI5NDdaFw00NjA0MjIwNzI5NDdaMBAxDjAMBgNVBAMTBWFkbWluMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwE513/kdkIp+Z5pI7rq1UKV5LBiB6dl4Wh46nI3mhVacOA1dJJWIUxRkkrUNWJewe8eJ7QWmhSpeBauA06PrAOTd1ZA4gSUWKpsYJqKm5Nxjp+BUMK1nHGQCkNQWjRllhyKTJeG/9PPc0ZrJh4V27bCXC9iX5l/ve35fp99VR4tQ947HWObe07EdPIEFNYfT/IurPwMySZ1WH1gyDTw7IxMXcPVg+GUlfBoSVgQ1UdCkvgHc9pE1LuFmyBguAzGLbXDfspUuTs85RLGXGYdv2vZU/R2kJEs3ePMtaGXw6DVSx82RkPFVaLDCdShX24yk6gLNEv9oTUXY40i3n8njRQIDAQABo1MwUTAdBgNVHQ4EFgQUDE381mprCEvSLaFeOwZRliBSJnwwHwYDVR0jBBgwFoAUDE381mprCEvSLaFeOwZRliBSJnwwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQUFAAOCAQEAgU0o8Qp5wn3vkcQLYao2heWKsbYYl8wqkztRVVKb+qMe2m/FOOMK1Rxv/anEVHHN+SnTc481fHd8z3w6II28LxJ5M+IxzoAFTj6gCv8+rL1R9kE91401d1+ulAiJR92ykOcB1h8bk5yoCZSRLIXwViCGUrbC3iu2NLWQDYk4vjxwqCSJOWUQh+qaYGCjB6mgkBMAnXGJCN2fV7sAR7N8Hy7Yh5jvuQOgY574FSoSuKCMGJZ6ecJlw+rB5pqanlLS9+HNnQ655/gTYgVBJClFClh4nwdPHtpyTySwgx1Vr3VDvglfnZM+gD/D2d9nTLIlT3MZqhGOIkxKvpVVkdJKzjGCAakwggGlAgEAMBUwEDEOMAwGA1UEAxMFYWRtaW4CAQAwDQYJYIZIAWUDBAIBBQCgaTAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMC8GCSqGSIb3DQEJBDEiBCDJDYSA3+VA0KfGQbP7BQsnL/s24W/WIb99zb+4uQ8KLjAcBgkqhkiG9w0BCQUxDxcNMjYwNDI3MDczMDAyWjALBgkqhkiG9w0BAQsEggEAYgDB02/z+KaLvieL1hMMA9IZN8KKc4igvilBoS5W7isiArP8D/GIxghMZkrC0Tzqs+/VRlfFREUgf4aBd9GVzd86Qfrhcrzrdd8hoDQvOw/X3UGftqbgJQmOjZUDpI3TiupyQvOU/zqlIjOq5BiZN6RNti2BTcbNyjaTeVh6u1tcqVVSp/Z0keUb+CnJFtIk6WhFepJMWI0vN84OyegNsjzIMSU2WjiN3i0jmYc62MpxUN0ZzmNgdZ7y6exe1Sb8EYUYL83BehQUPKO5EwEjEwX+ScYziWK0atXZioZYI2XLejVbQm1/czPTlA3frywKyM1dnkiufzmRpB49QN4o3g==
        """.trimIndent()
        val signedData = sut.getSignedData(signature, message)
        val result = sut.verifySignedData(signedData, certs)
        assertTrue(result)
    }
}
