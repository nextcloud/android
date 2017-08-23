/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.google.gson.reflect.TypeToken;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class EncryptionTest {
    private String encryptedString = "{\"metadata\":{\"encrypted\":\"np1sIwoAFCb/vRqV/jWOIe1UtyVO02EJhvPoh3VwcZuiSbDjwQO5QHUWtKXpHLyX6wfbkRX6nr8mSG0+HhLRud1t126UMxQK5BNINu99WlzPMa5PaKhTXlpuRUf3tR6PTQ\\u003d\\u003d\",\"initializationVector\":" +
            "\"kahzfT4u86Knc+e3\",\"sharing\":{\"recipient\":{\"blah@schiessle.org\":\"PUBLIC KEY\"," +
            "\"bjoern@schiessle.org\":\"PUBLIC KEY\"},\"signature\":\"HMACOFRECIPIENTANDNEWESTMETADATAKEY\"}," +
            "\"version\":1},\"files\":{\"ia7OEEEyXMoRa1QWQk8r\":{\"encrypted\":\"yl52TIccvo62LezCaFjQFJs7a1Q281pOuj59oNXMX7ti+7+h1SjK1AAk1HuwT+CI7BT64+R0ZLgyR/vBPjWvAQuxi9JWgsCjFMX91Mv2m2zI/bNQCarczOfnmf4FZ3Nv6yPLSjShmfQzemQ99Z3g7UHyrZ6pKT18m17IueJHF3V5kOhd9vcH\",\"metadataKey\":0," +
            "\"initializationVector\":\"+mHu52HyZq+pAAIN\"}," +
            "\"n9WXAIXO2wRY4R8nXwmo\":{\"encrypted\":\"Z9YTAgY/0YqKQlDwiqENcZRRupjgmJ1f0bTy0hOHP2/mHxFtoHCftT4STvt21OJMx8wF6V3cquQIGJ976WxkhA4SQxaQNpznhm1W9e8y+B5x8QnxSasYnOSFtZ/xVgQq6IZRjvYdPo7rvZk49hnqkwnUyvqtCj14aCE42qoxVZCd9M6XaZEBTA\\u003d\\u003d\",\"metadataKey\":0,\"initializationVector\":" +
            "\"sOFd17hCKWIv0gyB\"}}}";

    private String decryptedString = "{\"metadata\":{\"encrypted\":{\"metadataKeys\":{\"0\":" +
            "\"s4k4LPDpxoO53TKwem3Lo1\",\"2\":\"…\",\"3\":\"NEWESTMETADATAKEY\"}},\"initializationVector\":" +
            "\"kahzfT4u86Knc+e3\",\"sharing\":{\"recipient\":{\"blah@schiessle.org\":\"PUBLIC KEY\"," +
            "\"bjoern@schiessle.org\":\"PUBLIC KEY\"},\"signature\":\"HMACOFRECIPIENTANDNEWESTMETADATAKEY\"}," +
            "\"version\":1},\"files\":{\"ia7OEEEyXMoRa1QWQk8r\":{\"encrypted\":{\"key\":" +
            "\"jtboLmgGR1OQf2uneqCVHpklQLlIwWL5TXAQ0keK\",\"filename\":\"test.txt\",\"authenticationTag\":" +
            "\"HMAC of file\",\"version\":1},\"metadataKey\":0,\"initializationVector\":\"+mHu52HyZq+pAAIN\"}," +
            "\"n9WXAIXO2wRY4R8nXwmo\":{\"encrypted\":{\"key\":\"s4k4LPDpxoO53TKwem3Lo1yJnbNUYH2KLrSFT8Ea\"," +
            "\"filename\":\"test2.txt\",\"authenticationTag\":\"HMAC of file\",\"version\":1}," +
            "\"metadataKey\":0,\"initializationVector\":\"sOFd17hCKWIv0gyB\"}}}";

    private String privateKey = "TUlJRXZRSUJBREFOQmdrcWhraUc5dzBCQVFFRkFBU0NCS2N3Z2dTakFnRUFBb0lCQVFEV0FKMDNuNlBLVDNJWlZTT1paSzgyd3dqRTVWSW4vclZ5L0VLbmsyMVgzQ1dPbkdsb0ZrUXdiSWFsYjlkVWNRcmQzM250NVFoMmhJeXM5TkdsMjdMUmhreU1qemYzaFdzbnljSVhZQURVSExweGJZbWpJRzRjbWlNQnI4ckhjVFEvM0dUTlBKWG00ZzUwQlZEOXZlRThoMkQrTXdIQXlTSkxZcG5FaDBKWExES0pvVnpQYldxdVZVdHZNMmdmWVFHNFgwbS9kamR5VzJ4ZGN2UkpZeUtpWDZ3UmY3U3ViaWJkR1V1MHgxckhRdGhnM1dtaXdLd09NR2pQQ3oxRDAwMC9vWjZnckRHcW03TG5XMVZaN0VNTWtWWjVjVlc4NlpGQ2lrcXNtcDd1MVg1YjdCTzBiaThyV2Z0Zm8zZnJaOUptdmkvZHpmeXo2RFRFOWIrZU9CNzlBZ01CQUFFQ2dnRUJBSURYYzlCR1k5VnRDWFBwQjNyVjNJdXExcis4bFQ4UklldHRweSsvR2dqWXVSL29XYW5hSmduRmZUZGpZNUFxVXZHTUY1dTcxZUdOSWlrTGFLRmo2WUF1VEM0Z0dBRUZLYU9WM0M0NGxhY2UrTDFMeHA4WTZsSjhGbkZ3aGpTWG1tNk1ZWUFUWnVqUDF3WFJJWmJ6V0FVYU9MSXl3VzV4YWgxYTZ0c2cyRGNrZ2h3YVoxMURiRTVkNEpNNXl0Q1J6YmZzWTA3cElrUmU4WlFLcm1sMXU0THlsWXVHVHBDZGMwSnNXdUx3d1NWbm8vK056RURiVVNKZThIRmpnM1k3L2JkajgzNE9INzBtQWdBVUdEQTJqQ1BqY1hzNHJnN25udkhuQm5WcVFaemFvVWhHYzFOak9pZzMwNitUV25OL1JUNWc0TThucDNuMXZETWF2dVBiM0lFQ2dZRUE2NVpQV243UlYxZVVVS20zcVRUN1BSa3NuLzZDVWdFaFQ1WDloc0c3cXJXMUVlalhuODE2S1A0R3VsaDc1S0dhQ3hUVmdGZDhHdHF1QVU3MGdaKzB5MHVGd2hTdGtWN1A0K2xlUXlYSmxtNDNWNU9zT3Jxb25ocTZyOCthZ3lGbnNuOEhGd3lVcG1VM0FqR2tXVUNzYWFNdWM4SEt0ajU4RkdGRlh6K3ZlcDBDZ1lFQTZJdUZVZmljLytpUXpsVjczTDdWSS9kRjJNcFRPeHVKb1c4MmM1OE1tdEhGMFFQSktlYU5YVGJuYzluUXZ0aFBWM3c1eEZWc3FkN3pKOEM4ZkJ5S21Xc2ZLOU1abGQvY3RZTk9EMTNLWlM2WTNvWXpGVU5ZRzFPSEhpamNZT3RSWUsvY3k0cS9SRnExQ0pZVCtPaEVTOXlmeGFqZlZXdzFPdGxNZGNuNlYrRUNnWUFwNlN5bTBjYldQZnRodWorMU4zcTJyT0xXZDhXaFp4Z1ErNE1GMVROWXRFakpMZDRtVEx5OXpDdFFQV3VWQ2ZiSW4rVTNsdGk2UWtzUWFvWnZCUVY1NFM2amoyQXRhMnVhaFNyQzBWY2lqdXNEaG43dVY4U2xrK1hBWHpPQ3ZvK2ZIcUFaUnFDdlZYUkt6S0FMVE1rZlpldGVwb3cwamJzdk9QckpiaC8rdFFLQmdIQUFpd1R4RmtVWGNXOC9zdm1lSERCSGI2ZTd3eHlyNWIwUVFJeXRwVGVJSTV2SkZBR1BYclR2dGNpUnR6M0VGMnJPbFZBZnlNZUViMTdOTUxzaVVBc1draHZjZiswMHRpdmlneDFaa2hycnQ0c3QzYnEzQmQrYmVtK25SSVdWc1VzOVNMM3NKTFU2Yndra3A1ZngzcnNmRndEdmxpbWhoWDNEblZUNkpBNWhBb0dBZUtSZGRBVFAxZmVDL1pIZWE0VWh1MEVIencrY2xraDBrdjFDQkNHNnhjM002Y245SzFUNlIwMHRZYlFjS1JvZHJ1ZnBZNUNOL3V2Zyt0VWRucTUxRFhwTU5tbDVMdGpRYnV3MktuZ1F1ZG9NQ1NFMHMzT0dnaGJqMzZtbEVsRERjUGxLaE0rb1hyaVRYdXUvWmljclZqcGxTcWxuSlN2aDh6STJGNFBLaDlnPQ==";

    private String publicKey = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDpzCCAo+gAwIBAgIBADANBgkqhkiG9w0BAQUFADBuMRowGAYDVQQDDBF3d3cu\n" +
            "bmV4dGNsb3VkLmNvbTESMBAGA1UECgwJTmV4dGNsb3VkMRIwEAYDVQQHDAlTdHV0\n" +
            "dGdhcnQxGzAZBgNVBAgMEkJhZGVuLVd1ZXJ0dGVtYmVyZzELMAkGA1UEBhMCREUw\n" +
            "HhcNMTcwOTA0MTEwNTQyWhcNMzcwODMwMTEwNTQyWjBuMRowGAYDVQQDDBF3d3cu\n" +
            "bmV4dGNsb3VkLmNvbTESMBAGA1UECgwJTmV4dGNsb3VkMRIwEAYDVQQHDAlTdHV0\n" +
            "dGdhcnQxGzAZBgNVBAgMEkJhZGVuLVd1ZXJ0dGVtYmVyZzELMAkGA1UEBhMCREUw\n" +
            "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDWAJ03n6PKT3IZVSOZZK82\n" +
            "wwjE5VIn/rVy/EKnk21X3CWOnGloFkQwbIalb9dUcQrd33nt5Qh2hIys9NGl27LR\n" +
            "hkyMjzf3hWsnycIXYADUHLpxbYmjIG4cmiMBr8rHcTQ/3GTNPJXm4g50BVD9veE8\n" +
            "h2D+MwHAySJLYpnEh0JXLDKJoVzPbWquVUtvM2gfYQG4X0m/djdyW2xdcvRJYyKi\n" +
            "X6wRf7SubibdGUu0x1rHQthg3WmiwKwOMGjPCz1D000/oZ6grDGqm7LnW1VZ7EMM\n" +
            "kVZ5cVW86ZFCikqsmp7u1X5b7BO0bi8rWftfo3frZ9Jmvi/dzfyz6DTE9b+eOB79\n" +
            "AgMBAAGjUDBOMB0GA1UdDgQWBBS3zNF86LEZFT/KDdscr4ZJEisXqDAfBgNVHSME\n" +
            "GDAWgBS3zNF86LEZFT/KDdscr4ZJEisXqDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3\n" +
            "DQEBBQUAA4IBAQCPMu5AKNlh0gTr/k9Vc7RJ01uF07D+lTPGIErfW7+qXO21kXyw\n" +
            "+w8sxw+e1B/gah/bxotfO7ZuOhs49d8QRUzPy/miBFaZXfjfiqs7UXSDQ6oUbX3a\n" +
            "X9eTFMHDcsSUbyqhwn2cghmPJEhE10mtH2DJNPqDYvdpekJ6sEUVaqx63CD3nxcl\n" +
            "7fXh0IfmvDQOrSBszRqPY8pvnZJIEwqaENPk9Vgbzs8oXVstKl6wCqM0B36tmhBl\n" +
            "f6Dz/EhriF3Rq9w5RrWZOpS6XAWwRpyHPN+lKPa321dF6EEsnvhX8G3UbLbr0uEg\n" +
            "dR8lPhuKejU/Ds0ARwQGmFXFzidFNZL5ymos\n" +
            "-----END CERTIFICATE-----";

    @Test
    public void deserializeJSON() {
        String file = "ia7OEEEyXMoRa1QWQk8r";
        DecryptedFolderMetadata metadata = EncryptionUtils.deserializeJSON(decryptedString,
                new TypeToken<DecryptedFolderMetadata>() {});

        assertEquals("jtboLmgGR1OQf2uneqCVHpklQLlIwWL5TXAQ0keK", metadata.files.get(file).encrypted.key);
        assertEquals("+mHu52HyZq+pAAIN", metadata.files.get(file).initializationVector);
    }

    @Test
    public void serializeJSON() {
        try {
            HashMap<Integer, String> metadataKeys = new HashMap<>();
            metadataKeys.put(0, EncryptionUtils.encryptStringAsymmetric("s4k4LPDpxoO53TKwem3Lo1", publicKey));
            metadataKeys.put(1, EncryptionUtils.encryptStringAsymmetric("Q3ZobVJHbTlkK1VHT0g3ME", publicKey));
            metadataKeys.put(2, EncryptionUtils.encryptStringAsymmetric("lkK1VHT0g3ME3TKwem3Lo1", publicKey));
            DecryptedFolderMetadata.Encrypted encrypted = new DecryptedFolderMetadata.Encrypted();
            encrypted.metadataKeys = metadataKeys;

            DecryptedFolderMetadata.Metadata metadata1 = new DecryptedFolderMetadata.Metadata();
            metadata1.metadataKeys = metadataKeys;
            metadata1.version = 1;

            DecryptedFolderMetadata.Sharing sharing = new DecryptedFolderMetadata.Sharing();
            sharing.signature = "HMACOFRECIPIENTANDNEWESTMETADATAKEY";
            HashMap<String, String> recipient = new HashMap<>();
            recipient.put("blah@schiessle.org", "PUBLIC KEY");
            recipient.put("bjoern@schiessle.org", "PUBLIC KEY");
            sharing.recipient = recipient;
            metadata1.sharing = sharing;

            HashMap<String, DecryptedFolderMetadata.DecryptedFile> files = new HashMap<>();

            DecryptedFolderMetadata.Data data1 = new DecryptedFolderMetadata.Data();
            data1.key = "jtboLmgGR1OQf2uneqCVHpklQLlIwWL5TXAQ0keK";
            data1.filename = "test.txt";
            data1.version = 1;

            DecryptedFolderMetadata.DecryptedFile file1 = new DecryptedFolderMetadata.DecryptedFile();
            file1.initializationVector = "+mHu52HyZq+pAAIN";
            file1.encrypted = data1;
            file1.metadataKey = 0;
            file1.authenticationTag = "HMAC of file";

            files.put("ia7OEEEyXMoRa1QWQk8r", file1);

            DecryptedFolderMetadata.Data data2 = new DecryptedFolderMetadata.Data();
            data2.key = "s4k4LPDpxoO53TKwem3Lo1yJnbNUYH2KLrSFT8Ea";
            data2.filename = "test2.txt";
            data2.version = 1;

            DecryptedFolderMetadata.DecryptedFile file2 = new DecryptedFolderMetadata.DecryptedFile();
            file2.initializationVector = "sOFd17hCKWIv0gyB";
            file2.encrypted = data2;
            file2.metadataKey = 0;
            file2.authenticationTag = "HMAC of file";

            files.put("n9WXAIXO2wRY4R8nXwmo", file2);

            DecryptedFolderMetadata metadata = new DecryptedFolderMetadata(metadata1, files);

            // serialize
            assertEquals(decryptedString, EncryptionUtils.serializeJSON(metadata));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}