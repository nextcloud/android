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