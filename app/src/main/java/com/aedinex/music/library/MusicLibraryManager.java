/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.library;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.aedinex.music.model.AudioMetadata;
import com.aedinex.music.utils.AudioFileFilter;
import com.owncloud.android.datamodel.OCFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages the music library, scanning both local and cloud files.
 * Organizes music by artists, albums, genres, and provides search functionality.
 */
public class MusicLibraryManager {

    private final Context context;
    private final List<AudioMetadata> allTracks = new ArrayList<>();
    private final Map<String, List<AudioMetadata>> artistsMap = new HashMap<>();
    private final Map<String, List<AudioMetadata>> albumsMap = new HashMap<>();
    private final Map<String, List<AudioMetadata>> genresMap = new HashMap<>();

    private LibraryScanListener scanListener;

    public MusicLibraryManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Scan local device music library using MediaStore
     */
    public void scanLocalLibrary() {
        new Thread(() -> {
            notifyScanStarted();

            List<AudioMetadata> tracks = new ArrayList<>();

            String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

            try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                MediaStore.Audio.Media.TITLE + " ASC"
            )) {

                if (cursor != null && cursor.moveToFirst()) {
                    int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    int albumArtistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST);
                    int genreColumn = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE);
                    int yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
                    int trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
                    int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                    int mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);

                    do {
                        String filePath = cursor.getString(pathColumn);

                        AudioMetadata metadata = new AudioMetadata(filePath);
                        metadata.setTitle(cursor.getString(titleColumn));
                        metadata.setArtist(cursor.getString(artistColumn));
                        metadata.setAlbum(cursor.getString(albumColumn));
                        if (albumArtistColumn >= 0) {
                            metadata.setAlbumArtist(cursor.getString(albumArtistColumn));
                        }
                        if (genreColumn >= 0) {
                            metadata.setGenre(cursor.getString(genreColumn));
                        }
                        if (yearColumn >= 0) {
                            metadata.setYear(cursor.getInt(yearColumn));
                        }
                        if (trackColumn >= 0) {
                            metadata.setTrackNumber(cursor.getInt(trackColumn));
                        }
                        metadata.setDuration(cursor.getLong(durationColumn));
                        metadata.setFileSize(cursor.getLong(sizeColumn));
                        metadata.setMimeType(cursor.getString(mimeColumn));

                        tracks.add(metadata);

                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                notifyScanError(e);
                return;
            }

            updateLibrary(tracks);
            notifyScanCompleted(tracks.size());

        }).start();
    }

    /**
     * Scan cloud files for music
     */
    public void scanCloudFiles(List<OCFile> files) {
        new Thread(() -> {
            notifyScanStarted();

            List<AudioMetadata> tracks = new ArrayList<>();

            for (OCFile file : files) {
                if (!file.isFolder() && AudioFileFilter.isAudioFile(file)) {
                    AudioMetadata metadata = new AudioMetadata(file.getRemotePath());

                    // Basic info from OCFile
                    String fileName = file.getFileName();
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        metadata.setTitle(fileName.substring(0, dotIndex));
                    } else {
                        metadata.setTitle(fileName);
                    }

                    metadata.setFileSize(file.getFileLength());
                    metadata.setMimeType(file.getMimeType());

                    // TODO: Extract metadata from actual file when downloaded
                    tracks.add(metadata);
                }
            }

            updateLibrary(tracks);
            notifyScanCompleted(tracks.size());

        }).start();
    }

    /**
     * Update library with scanned tracks
     */
    private void updateLibrary(List<AudioMetadata> tracks) {
        synchronized (allTracks) {
            allTracks.clear();
            allTracks.addAll(tracks);

            artistsMap.clear();
            albumsMap.clear();
            genresMap.clear();

            for (AudioMetadata track : tracks) {
                // Group by artist
                String artist = track.getArtist();
                artistsMap.computeIfAbsent(artist, k -> new ArrayList<>()).add(track);

                // Group by album
                String album = track.getAlbum();
                albumsMap.computeIfAbsent(album, k -> new ArrayList<>()).add(track);

                // Group by genre
                String genre = track.getGenre();
                genresMap.computeIfAbsent(genre, k -> new ArrayList<>()).add(track);
            }
        }
    }

    /**
     * Get all tracks
     */
    public List<AudioMetadata> getAllTracks() {
        synchronized (allTracks) {
            return new ArrayList<>(allTracks);
        }
    }

    /**
     * Get all artists
     */
    public Set<String> getAllArtists() {
        synchronized (allTracks) {
            return new TreeSet<>(artistsMap.keySet());
        }
    }

    /**
     * Get tracks by artist
     */
    public List<AudioMetadata> getTracksByArtist(String artist) {
        synchronized (allTracks) {
            List<AudioMetadata> tracks = artistsMap.get(artist);
            return tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
        }
    }

    /**
     * Get all albums
     */
    public Set<String> getAllAlbums() {
        synchronized (allTracks) {
            return new TreeSet<>(albumsMap.keySet());
        }
    }

    /**
     * Get tracks by album
     */
    public List<AudioMetadata> getTracksByAlbum(String album) {
        synchronized (allTracks) {
            List<AudioMetadata> tracks = albumsMap.get(album);
            return tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
        }
    }

    /**
     * Get all genres
     */
    public Set<String> getAllGenres() {
        synchronized (allTracks) {
            return new TreeSet<>(genresMap.keySet());
        }
    }

    /**
     * Get tracks by genre
     */
    public List<AudioMetadata> getTracksByGenre(String genre) {
        synchronized (allTracks) {
            List<AudioMetadata> tracks = genresMap.get(genre);
            return tracks != null ? new ArrayList<>(tracks) : new ArrayList<>();
        }
    }

    /**
     * Search tracks by query
     */
    public List<AudioMetadata> searchTracks(String query) {
        List<AudioMetadata> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        synchronized (allTracks) {
            for (AudioMetadata track : allTracks) {
                if (track.getTitle().toLowerCase().contains(lowerQuery) ||
                    track.getArtist().toLowerCase().contains(lowerQuery) ||
                    track.getAlbum().toLowerCase().contains(lowerQuery)) {
                    results.add(track);
                }
            }
        }

        return results;
    }

    /**
     * Get total track count
     */
    public int getTrackCount() {
        synchronized (allTracks) {
            return allTracks.size();
        }
    }

    /**
     * Get artist count
     */
    public int getArtistCount() {
        synchronized (allTracks) {
            return artistsMap.size();
        }
    }

    /**
     * Get album count
     */
    public int getAlbumCount() {
        synchronized (allTracks) {
            return albumsMap.size();
        }
    }

    /**
     * Clear library
     */
    public void clearLibrary() {
        synchronized (allTracks) {
            allTracks.clear();
            artistsMap.clear();
            albumsMap.clear();
            genresMap.clear();
        }
    }

    /**
     * Set scan listener
     */
    public void setScanListener(LibraryScanListener listener) {
        this.scanListener = listener;
    }

    private void notifyScanStarted() {
        if (scanListener != null) {
            scanListener.onScanStarted();
        }
    }

    private void notifyScanCompleted(int trackCount) {
        if (scanListener != null) {
            scanListener.onScanCompleted(trackCount);
        }
    }

    private void notifyScanError(Exception e) {
        if (scanListener != null) {
            scanListener.onScanError(e);
        }
    }

    /**
     * Listener interface for library scan events
     */
    public interface LibraryScanListener {
        void onScanStarted();
        void onScanCompleted(int trackCount);
        void onScanError(Exception e);
    }
}
