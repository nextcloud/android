/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.lib.resources.status.E2EVersion

class EncryptionTestUtils {
    val t1PrivateKey =
        "MIIEugIBADANBgkqhkiG9w0BAQEFAASCBKQwggSgAgEAAoIBAQC1p8eYMFwGoi7geYzEwNbePRLL5LRhorAecFG3zkpLBwSi/QHkU4" +
            "u4uSegEbHgOfe73eKVOFdfFpw8wd5cvtY+4CzbX8bu+yrC+tFGcJ25/4VQ78Bl4MI0SvOmxDwuZNrg9SWgs9RwialKOsfCEyz0" +
            "SS8RstGNt5KZKn1e8z7V9X/eORPmOQ5KcIXHlMbAY3m4erBSKvhRZdqy+Dbnc0rZeZaKkoIMJH1OYfVto/ek12iIKF2YStPVzo" +
            "TgNsFelPDxeA/lltgf6qDVRD+ELydEncPIJwcv52D8ZitoEyEOfjDZW+rvvE02g1ZD1xPkDLpwltAsFCglCKvKBAWuhthFAgMB" +
            "AAECgf8BN1MLcq+6m8C1tzNvN/UDd9c0rUpexM6D5eC4O+6B7YGidEqIhHVIzUj0e2HUgpRBbURxsvF1FWdIT2gu7dnULtOGWQ" +
            "xNujJ0kGwXfAnqxh/rACDFb5TS3sJawEExC5yJw14bCEbE/0uBF5uiTU/U9AV7PKHlqAKsS2RtcwPNceB8zDu0hh/Mb/uS7274" +
            "TsxUllx0WzGZrozO1K6AlOete9rXmmpghpFTNVhxgf0pxe3hrK+tZGSL9di+Wft9eCvSbdG/FzeXgwVqmGtWU7kSB7FqstEEJO" +
            "4VpOSyEfcXGHTHwdZjrhBUuAcjWE8E0mCKa8htRE52czb3C0f7ZYkCgYEA5eH3vmHEgQjXzSSEtbmDLRq9X9SB7pIAIXHj2UuE" +
            "OTkLUJ/7xLTHqt82jqZaZzns1RZIJXKZjH85CswQp/py2/qD240KvA/N+ELZaciaV+Wg+m4+iHdi0DyPkaKaBtFG1nsR2GbVWO" +
            "1OsaTUZTG4D7RCUErU6XVmNPQKSk5uRA0CgYEAykskpX3KKuWq5nxV4vwgPmxz+uAfCtaGhcPEUg764SR+n0ODAvGiEJU7B0Q2" +
            "oX621pDOQeRfFufiMWfD8ByhErs1HFCmW69YPlR8qamfc8tHG5UM+r3bb49sDEYU4qr1Ji5Zzs4XgfmToKLbWdzzhaW6YxqO7N" +
            "ntIIh2169PPxkCgYBF2TAWl8xGTLKNcYAlW1XBObO6z24fWBtUDi/mEWz+mheXCtVMAoX8pFAGbgNgBBiy8k8/mZ+QMgPaBQE2" +
            "mQGXV3oDFsrhM4go298Fpl9HP8126lJz0pqinRQecyKL2cDFYKWedDh1Cb30ehnTGZVMqD/R97rTqMlCY7hQtZ4JbQKBgEXpLD" +
            "QJQeoLT0GybJgyXA5WuspT1EaRlxH5cwqM5MUUMLJnyYol6cVjXXAIcfzj5tpGVxHMk9Q9tR0v6DY+HqhzjEpJ0QRUl+GKnz6f" +
            "QVzqPpvYqhCptoFahpPDUIp5XJmiYSUoclVX5F4aikYHJx3kBYMkdYqDUgDxSGkHzBJZAoGAHV44xgTW02dgeB5GfDJVWCJKAU" +
            "GsYOFuUehKUBXSJ0929hdP0sjOQDJN3DEDISzmgdWX5NyLJxEYgFWNivpePjWCWzOzyua3nPSpvxPIUB7xh27gjT91glj1hEmy" +
            "sCd7+9yoMPiCXR7iigRycxegI/Krd39QzISSk9O0finfytU="

    val t1PublicKey = """-----BEGIN CERTIFICATE-----
MIIC6DCCAdCgAwIBAgIBADANBgkqhkiG9w0BAQUFADANMQswCQYDVQQDDAJ0MTAe
Fw0yMzA3MjUwNzU3MTJaFw00MzA3MjAwNzU3MTJaMA0xCzAJBgNVBAMMAnQxMIIB
IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtafHmDBcBqIu4HmMxMDW3j0S
y+S0YaKwHnBRt85KSwcEov0B5FOLuLknoBGx4Dn3u93ilThXXxacPMHeXL7WPuAs
21/G7vsqwvrRRnCduf+FUO/AZeDCNErzpsQ8LmTa4PUloLPUcImpSjrHwhMs9Ekv
EbLRjbeSmSp9XvM+1fV/3jkT5jkOSnCFx5TGwGN5uHqwUir4UWXasvg253NK2XmW
ipKCDCR9TmH1baP3pNdoiChdmErT1c6E4DbBXpTw8XgP5ZbYH+qg1UQ/hC8nRJ3D
yCcHL+dg/GYraBMhDn4w2Vvq77xNNoNWQ9cT5Ay6cJbQLBQoJQirygQFrobYRQID
AQABo1MwUTAdBgNVHQ4EFgQUE9zCeA9/QMAtVgLxD23X6ZcodhMwHwYDVR0jBBgw
FoAUE9zCeA9/QMAtVgLxD23X6ZcodhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG
9w0BAQUFAAOCAQEAZdy/YjJlvnz3FQwxp6oVtMJccpdxveEPfLzgaverhtd/vP8O
AvDzOLgQJHmrDS91SG503eU4cYGyuNKwd77OyTnqMg+GUEmJhGfPpSVrEIdh65jv
q61T4oqBdehevVmBq54rGiwL0DGv1DlXQlwiJZP4qni2KnOEFcnvL3gVtRnQjXQ+
kHvlMshkK6w021EMV5NfjG2zg67wC65rLaej5f6Ssp2S7g2VtmE4aXq1bjAuEbqk
4TiyZHLDdsJuqzyGyyOpMV7i9ucXDoaZt9cGS9hT2vRxTrSH63vKR8Xeig9+stLw
t9ONcUqCKP7hd8rajtxM4JIIRExwD8OkgARWGg==
-----END CERTIFICATE-----"""

    val johnPrivateKey =
        """MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDuPcSvlhqElPQsCdJEuGmptGj4TUBWe33yu+ncOYR8Ec3M0H4NL0gE
            |ORJJcz9i18ByLpNzDy6NUGOtlf9YSat/zKdAfFiZJolKc/y4BPfTr8xx5ml2mu4Rz39LXRru+nnhluV3g1h2Z9LvWhUVUqAztz9W2H
            |H6uC7jx+7HNtYC9VgsVzHjuHPQMlOePPZlr9Hry5enF/Psn24RdiKqwCz8WhsOwtmW5PdHLLBVHAoF53URnFR4sgmLLGlS2GEZ8hvx
            |vdV/2NmhRWLebmCZziyklAe9gCR9lgfN32tqzyMG7VptBHFy7YJidWjpjSZPGEqFBL+fmCO/cTGJAXfCn9djAgMBAAECggEAV2QBCg
            |edopShHKZdoyeiWsX621o7B341LR0RI99VYc2GGGNCWcPGPwZQVvEXh0JtLXU4UTR4dw3OApbLG6+qYS7JCzaRqVwhcFYrlbT804Hh
            |FMbYWNFsEsxyfUqh3peyrbWUZsqfYI+lKHd61F+CtHW7nje3V6jISnXEeP78cgioKOX8gsCG8DEWsmaLrQz0PyMwdhucRfa8Bm6qeX
            |NY+wCMg8lyH/+OLlyCZTdkaWbTBBD5UXGbZly8iX17McmsYhdjFyx1l0NQnVMAYjOpXXEkeEixZpSfm3GYxmdaQqZFkpbI/FbQF0yD
            |7hLrGwiRTDcyPUz+QypUv8CZxpXbgQKBgQD3btuYmb+BpPZjryfa3worv/3XQCTs08V0TX3mDxHVQL95TgP+L8/Z/brxIMBNpwG1wk
            |iCWLYLer68+qioMTohuzeUx7hRKcoHa9ezW8m7m9AcPmAnzNticPYv835BQjEu/avU98rwIDihsYgxcjU3L7/P2ajVgUDigQxmE3gO
            |OwKBgQD2fXBLwch0P5g2GCyOPYvSgyF/umS7mcyUVTE4WOoJNDf8Q+Bx1dA2yAKQaoVghqW4uCfOAo/rERvGAYZ7fm7nFwx1jZ8ToT
            |dKZwybIFPjF/zkfuZLajYxVOPnzuQrsXnjcGg/ltMKZg3NqnGQGnD1S3eOhZ+dIOBmb7+jSO4A+QKBgASqnpGeNLJpPgxbPVEva62v
            |jUYF+6xLwimTXJB+MEPpWLMc+Y5NsInX8zKg/393atzWsS9kJOrKgdZmk8+4PfRs53ty2NMPCrRhIExNqtxS7/XYZ0/Y2TpeDwaQfQ
            |0WBn9wYVE+6yDkOq0x//OOx9ommGN/I2QDcAnVjTpPm7AJAoGAYT8cDsdlTnfIlY70BSpC/8q8bKgdFeaXz+3MfW6W5wqzC9O7uS2h
            |9/rxCAj+lhaJS1dcXOql3Rfi3Tu80vwOxR1SzQ4StKvmJHSDhLA8aFwOahemxBojR1M2lz4IxzQ94n12o5/dozygNYQJSdEkv6IGiT
            |QuxM8zuTZdZQ5g2AECgYAujetfkwgVW7/gumpMKytoY0VuTzF4Y/XZfqBMVIiPIuUl57JbDzrcx6YVXX3PavxNWmBLBmMq3SHMbdva
            |H7LnU/8rvkT8xRVLg/w/bRJc3Lb3oUjrdhkUQUYDoOfMoFA+ceZ2L6bnSXwm86KKV+xoXWpxAoL4AvdNrMhoWw3+yg=="""
            .trimMargin()

    val johnPublicKey = """-----BEGIN CERTIFICATE-----
MIIDkDCCAnigAwIBAgIBADANBgkqhkiG9w0BAQUFADBhMQswCQYDVQQGEwJERTEb
MBkGA1UECAwSQmFkZW4tV3VlcnR0ZW1iZXJnMRIwEAYDVQQHDAlTdHV0dGdhcnQx
EjAQBgNVBAoMCU5leHRjbG91ZDENMAsGA1UEAwwEam9objAeFw0yMzA3MTQwNzM0
NTZaFw00MzA3MDkwNzM0NTZaMGExCzAJBgNVBAYTAkRFMRswGQYDVQQIDBJCYWRl
bi1XdWVydHRlbWJlcmcxEjAQBgNVBAcMCVN0dXR0Z2FydDESMBAGA1UECgwJTmV4
dGNsb3VkMQ0wCwYDVQQDDARqb2huMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB
CgKCAQEA7j3Er5YahJT0LAnSRLhpqbRo+E1AVnt98rvp3DmEfBHNzNB+DS9IBDkS
SXM/YtfAci6Tcw8ujVBjrZX/WEmrf8ynQHxYmSaJSnP8uAT306/MceZpdpruEc9/
S10a7vp54Zbld4NYdmfS71oVFVKgM7c/Vthx+rgu48fuxzbWAvVYLFcx47hz0DJT
njz2Za/R68uXpxfz7J9uEXYiqsAs/FobDsLZluT3RyywVRwKBed1EZxUeLIJiyxp
UthhGfIb8b3Vf9jZoUVi3m5gmc4spJQHvYAkfZYHzd9ras8jBu1abQRxcu2CYnVo
6Y0mTxhKhQS/n5gjv3ExiQF3wp/XYwIDAQABo1MwUTAdBgNVHQ4EFgQUmTeILVuB
tv70fTGkXWGAueDp5kAwHwYDVR0jBBgwFoAUmTeILVuBtv70fTGkXWGAueDp5kAw
DwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQUFAAOCAQEAyVtq9XAvW7nxSW/8
hp30z6xbzGiuviXhy/Jo91VEa8IRsWCCn3OmDFiVduTEowx76tf8clJP0gk7Pozi
6dg/7Fin+FqQGXfCk8bLAh9gXKAikQ2GK8yRN3slRFwYC2mm23HrLdKXZHUqJcpB
Mz2zsSrOGPj1YsYOl/U8FU6KA7Yj7U3q7kDMYTAgzUPZAH+d1DISGWpZsMa0RYid
vigCCLByiccmS/Co4Sb1esF58H+YtV5+nFBRwx881U2g2TgDKF1lPMK/y3d8B8mh
UtW+lFxRpvyNUDpsMjOErOrtNFEYbgoUJLtqwBMmyGR+nmmh6xna331QWcRAmw0P
nDO4ew==
-----END CERTIFICATE-----"""

    @Throws(java.lang.Exception::class)
    fun generateFolderMetadataV2(userId: String, cert: String): DecryptedFolderMetadataFile {
        val metadata = DecryptedMetadata().apply {
            metadataKey = EncryptionUtils.generateKey()
            keyChecksums.add(EncryptionUtilsV2().hashMetadataKey(metadataKey))
        }

        val file1 = DecryptedFile(
            "image1.png",
            "image/png",
            "gKm3n+mJzeY26q4OfuZEqg==",
            "PboI9tqHHX3QeAA22PIu4w==",
            "WANM0gRv+DhaexIsI0T3Lg=="
        )

        val file2 = DecryptedFile(
            "image2.png",
            "image/png",
            "hnJLF8uhDvDoFK4ajuvwrg==",
            "qOQZdu5soFO77Y7y4rAOVA==",
            "9dfzbIYDt28zTyZfbcll+g=="
        )

        val users = mutableListOf(
            DecryptedUser(userId, cert)
        )

        // val filedrop = mutableMapOf(
        //     Pair(
        //         "eie8iaeiaes8e87td6",
        //         DecryptedFile(
        //             "test2.txt",
        //             "txt/plain",
        //             "hnJLF8uhDvDoFK4ajuvwrg==",
        //             "qOQZdu5soFO77Y7y4rAOVA==",
        //             "9dfzbIYDt28zTyZfbcll+g=="
        //         )
        //     )
        // )

        metadata.files["ia7OEEEyXMoRa1QWQk8r"] = file1
        metadata.files["n9WXAIXO2wRY4R8nXwmo"] = file2

        return DecryptedFolderMetadataFile(metadata, users, mutableMapOf(), E2EVersion.V2_0.value)
    }
}
