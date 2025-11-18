/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.library;

import android.content.Context;
import android.content.SharedPreferences;

import com.aedinex.music.model.AudioMetadata;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Manages user playlists with persistence.
 */
public class PlaylistManager {

    private static final String PREFS_NAME = "aedinex_playlists";
    private static final String KEY_PLAYLISTS = "playlists";

    private final Context context;
    private final Map<String, Playlist> playlists = new HashMap<>();
    private final SharedPreferences prefs;

    public PlaylistManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadPlaylists();
    }

    /**
     * Create a new playlist
     */
    public Playlist createPlaylist(String name) {
        String id = UUID.randomUUID().toString();
        Playlist playlist = new Playlist(id, name);
        playlists.put(id, playlist);
        savePlaylists();
        return playlist;
    }

    /**
     * Delete a playlist
     */
    public boolean deletePlaylist(String playlistId) {
        if (playlists.remove(playlistId) != null) {
            savePlaylists();
            return true;
        }
        return false;
    }

    /**
     * Rename a playlist
     */
    public boolean renamePlaylist(String playlistId, String newName) {
        Playlist playlist = playlists.get(playlistId);
        if (playlist != null) {
            playlist.setName(newName);
            savePlaylists();
            return true;
        }
        return false;
    }

    /**
     * Get all playlists
     */
    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists.values());
    }

    /**
     * Get playlist by ID
     */
    public Playlist getPlaylist(String playlistId) {
        return playlists.get(playlistId);
    }

    /**
     * Add track to playlist
     */
    public boolean addTrackToPlaylist(String playlistId, AudioMetadata track) {
        Playlist playlist = playlists.get(playlistId);
        if (playlist != null) {
            playlist.addTrack(track);
            savePlaylists();
            return true;
        }
        return false;
    }

    /**
     * Remove track from playlist
     */
    public boolean removeTrackFromPlaylist(String playlistId, int trackIndex) {
        Playlist playlist = playlists.get(playlistId);
        if (playlist != null && playlist.removeTrack(trackIndex)) {
            savePlaylists();
            return true;
        }
        return false;
    }

    /**
     * Move track within playlist
     */
    public boolean moveTrackInPlaylist(String playlistId, int fromIndex, int toIndex) {
        Playlist playlist = playlists.get(playlistId);
        if (playlist != null && playlist.moveTrack(fromIndex, toIndex)) {
            savePlaylists();
            return true;
        }
        return false;
    }

    /**
     * Get playlist count
     */
    public int getPlaylistCount() {
        return playlists.size();
    }

    /**
     * Save playlists to SharedPreferences
     */
    private void savePlaylists() {
        try {
            JSONArray playlistsArray = new JSONArray();

            for (Playlist playlist : playlists.values()) {
                JSONObject playlistObj = new JSONObject();
                playlistObj.put("id", playlist.getId());
                playlistObj.put("name", playlist.getName());
                playlistObj.put("created", playlist.getCreatedTimestamp());

                JSONArray tracksArray = new JSONArray();
                for (AudioMetadata track : playlist.getTracks()) {
                    JSONObject trackObj = new JSONObject();
                    trackObj.put("path", track.getFilePath());
                    trackObj.put("title", track.getTitle());
                    trackObj.put("artist", track.getArtist());
                    trackObj.put("album", track.getAlbum());
                    trackObj.put("duration", track.getDuration());
                    tracksArray.put(trackObj);
                }
                playlistObj.put("tracks", tracksArray);

                playlistsArray.put(playlistObj);
            }

            prefs.edit().putString(KEY_PLAYLISTS, playlistsArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load playlists from SharedPreferences
     */
    private void loadPlaylists() {
        String playlistsJson = prefs.getString(KEY_PLAYLISTS, null);
        if (playlistsJson == null) {
            return;
        }

        try {
            JSONArray playlistsArray = new JSONArray(playlistsJson);

            for (int i = 0; i < playlistsArray.length(); i++) {
                JSONObject playlistObj = playlistsArray.getJSONObject(i);

                String id = playlistObj.getString("id");
                String name = playlistObj.getString("name");
                long created = playlistObj.getLong("created");

                Playlist playlist = new Playlist(id, name, created);

                JSONArray tracksArray = playlistObj.getJSONArray("tracks");
                for (int j = 0; j < tracksArray.length(); j++) {
                    JSONObject trackObj = tracksArray.getJSONObject(j);

                    AudioMetadata track = new AudioMetadata(trackObj.getString("path"));
                    track.setTitle(trackObj.getString("title"));
                    track.setArtist(trackObj.getString("artist"));
                    track.setAlbum(trackObj.getString("album"));
                    track.setDuration(trackObj.getLong("duration"));

                    playlist.addTrack(track);
                }

                playlists.put(id, playlist);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Playlist model class
     */
    public static class Playlist {
        private final String id;
        private String name;
        private final long createdTimestamp;
        private final List<AudioMetadata> tracks = new ArrayList<>();

        public Playlist(String id, String name) {
            this(id, name, System.currentTimeMillis());
        }

        public Playlist(String id, String name, long createdTimestamp) {
            this.id = id;
            this.name = name;
            this.createdTimestamp = createdTimestamp;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getCreatedTimestamp() {
            return createdTimestamp;
        }

        public List<AudioMetadata> getTracks() {
            return new ArrayList<>(tracks);
        }

        public int getTrackCount() {
            return tracks.size();
        }

        public void addTrack(AudioMetadata track) {
            tracks.add(track);
        }

        public boolean removeTrack(int index) {
            if (index >= 0 && index < tracks.size()) {
                tracks.remove(index);
                return true;
            }
            return false;
        }

        public boolean moveTrack(int fromIndex, int toIndex) {
            if (fromIndex >= 0 && fromIndex < tracks.size() &&
                toIndex >= 0 && toIndex < tracks.size()) {
                AudioMetadata track = tracks.remove(fromIndex);
                tracks.add(toIndex, track);
                return true;
            }
            return false;
        }

        public long getTotalDuration() {
            long total = 0;
            for (AudioMetadata track : tracks) {
                total += track.getDuration();
            }
            return total;
        }

        public String getFormattedDuration() {
            long totalSeconds = getTotalDuration() / 1000;
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;

            if (hours > 0) {
                return String.format("%d h %d min", hours, minutes);
            } else {
                return String.format("%d min", minutes);
            }
        }
    }
}
