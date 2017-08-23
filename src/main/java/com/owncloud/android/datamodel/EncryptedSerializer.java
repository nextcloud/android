package com.owncloud.android.datamodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by tobi on 03.08.17.
 */

public class EncryptedSerializer implements JsonSerializer<DecryptedFolderMetadata.Encrypted> {
    @Override
    public JsonElement serialize(DecryptedFolderMetadata.Encrypted src, Type typeOfSrc,
                                 JsonSerializationContext context) {

//        DecryptedFolderMetadata.Encrypted encrypted = new Gson().fromJson(src, DecryptedFolderMetadata.Encrypted.class);

        return null;
    }
}
