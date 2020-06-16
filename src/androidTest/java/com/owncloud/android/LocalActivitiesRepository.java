/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

package com.owncloud.android;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.activities.model.RichElement;
import com.owncloud.android.lib.resources.activities.model.RichElementTypeAdapter;
import com.owncloud.android.lib.resources.activities.models.PreviewObject;
import com.owncloud.android.lib.resources.activities.models.PreviewObjectAdapter;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.TAG;

public class LocalActivitiesRepository implements ActivitiesRepository {
    String json = "{\n" +
        "  \"ocs\": {\n" +
        "    \"meta\": {\n" +
        "      \"status\": \"ok\",\n" +
        "      \"statuscode\": 200,\n" +
        "      \"message\": \"OK\"\n" +
        "    },\n" +
        "    \"data\": [\n" +
        "      {\n" +
        "        \"activity_id\": 1114442,\n" +
        "        \"app\": \"files\",\n" +
        "        \"type\": \"file_created\",\n" +
        "        \"user\": \"test\",\n" +
        "        \"subject\": \"You created IMG_0255.JPG\",\n" +
        "        \"subject_rich\": [\n" +
        "          \"You created {file}\",\n" +
        "          {\n" +
        "            \"file\": {\n" +
        "              \"type\": \"file\",\n" +
        "              \"id\": \"3523820\",\n" +
        "              \"name\": \"IMG_0255.JPG\",\n" +
        "              \"path\": \"IMG_0255.JPG\",\n" +
        "              \"link\": \"https:\\/\\/cloud.server.com\\/f\\/3523820\"\n" +
        "            }\n" +
        "          }\n" +
        "        ],\n" +
        "        \"message\": \"\",\n" +
        "        \"message_rich\": [\n" +
        "          \"\",\n" +
        "          []\n" +
        "        ],\n" +
        "        \"object_type\": \"files\",\n" +
        "        \"object_id\": 3523820,\n" +
        "        \"object_name\": \"\\/IMG_0255.JPG\",\n" +
        "        \"objects\": {\n" +
        "          \"3523820\": \"\\/IMG_0255.JPG\"\n" +
        "        },\n" +
        "        \"link\": \"https:\\/\\/cloud.server.com\\/apps\\/files\\/?dir=\\/\",\n" +
        "        \"icon\": \"https:\\/\\/cloud.server.com\\/apps\\/files\\/img\\/add-color.svg\",\n" +
        "        \"datetime\": \"2020-03-19T12:18:25+00:00\",\n" +
        "        \"previews\": [\n" +
        "          {\n" +
        "            \"link\": \"https:\\/\\/cloud.server.com\\/apps\\/files\\/?dir=\\/&scrollto=IMG_0255.JPG\",\n" +
        "            \"source\": \"https:\\/\\/cloud.server.com\\/core\\/preview.png?file=\\/IMG_0255.JPG&c=b68e0f2668cc89eec0d17e6125ff8ae7&x=150&y=150\",\n" +
        "            \"mimeType\": \"image\\/jpeg\",\n" +
        "            \"isMimeTypeIcon\": false,\n" +
        "            \"fileId\": 3523820,\n" +
        "            \"view\": \"files\"\n" +
        "          }\n" +
        "        ]\n" +
        "      }]}}";

    @Override
    public void getActivities(int lastGiven, @NonNull LoadActivitiesCallback callback) {

        ArrayList activities = parseResult(json);

        callback.onActivitiesLoaded(activities, 1);
    }

    protected ArrayList<Activity> parseResult(String response) {
        if (response == null || response.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JsonParser jsonParser = new JsonParser();
            JsonObject jo = (JsonObject) jsonParser.parse(response);
            JsonArray jsonDataArray = jo.getAsJsonObject("ocs").getAsJsonArray("data");

            Gson gson = new GsonBuilder()
                .registerTypeAdapter(RichElement.class, new RichElementTypeAdapter())
                .registerTypeAdapter(PreviewObject.class, new PreviewObjectAdapter())
                .create();
            Type listType = new TypeToken<List<Activity>>() {
            }.getType();

            return gson.fromJson(jsonDataArray, listType);

        } catch (JsonSyntaxException e) {
            Log_OC.e(TAG, "Not a valid json: " + response, e);
            return new ArrayList<>();
        }
    }
}
