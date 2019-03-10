package com.nextcloud.client.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestPreferenceManager {

    @Mock
    private Context testContext;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private SharedPreferences.Editor editor;

    private PreferenceManager appPreferences;

    @Before
    public void setUp() {
        when(editor.remove(anyString())).thenReturn(editor);
        when(sharedPreferences.edit()).thenReturn(editor);
        appPreferences = new PreferenceManager(testContext, sharedPreferences);
    }

    @Test
    public void removeLegacyPreferences() {
        appPreferences.removeLegacyPreferences();
        InOrder inOrder = inOrder(editor);
        inOrder.verify(editor).remove("instant_uploading");
        inOrder.verify(editor).remove("instant_video_uploading");
        inOrder.verify(editor).remove("instant_upload_path");
        inOrder.verify(editor).remove("instant_upload_path_use_subfolders");
        inOrder.verify(editor).remove("instant_upload_on_wifi");
        inOrder.verify(editor).remove("instant_upload_on_charging");
        inOrder.verify(editor).remove("instant_video_upload_path");
        inOrder.verify(editor).remove("instant_video_upload_path_use_subfolders");
        inOrder.verify(editor).remove("instant_video_upload_on_wifi");
        inOrder.verify(editor).remove("instant_video_uploading");
        inOrder.verify(editor).remove("instant_video_upload_on_charging");
        inOrder.verify(editor).remove("prefs_instant_behaviour");
        inOrder.verify(editor).apply();
    }
}
