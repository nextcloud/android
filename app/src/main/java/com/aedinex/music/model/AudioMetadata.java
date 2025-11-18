/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Model class representing audio file metadata.
 * Contains information extracted from audio files like ID3 tags.
 */
public class AudioMetadata implements Parcelable {

    private final String filePath;
    private String title;
    private String artist;
    private String album;
    private String albumArtist;
    private String genre;
    private int year;
    private int trackNumber;
    private int discNumber;
    private long duration; // in milliseconds
    private int bitrate; // in kbps
    private int sampleRate; // in Hz
    private String mimeType;
    private long fileSize;
    private String albumArtPath;

    public AudioMetadata(String filePath) {
        this.filePath = filePath;
        this.title = "";
        this.artist = "";
        this.album = "";
        this.albumArtist = "";
        this.genre = "";
        this.year = 0;
        this.trackNumber = 0;
        this.discNumber = 0;
        this.duration = 0;
        this.bitrate = 0;
        this.sampleRate = 0;
        this.fileSize = 0;
    }

    protected AudioMetadata(Parcel in) {
        filePath = in.readString();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        albumArtist = in.readString();
        genre = in.readString();
        year = in.readInt();
        trackNumber = in.readInt();
        discNumber = in.readInt();
        duration = in.readLong();
        bitrate = in.readInt();
        sampleRate = in.readInt();
        mimeType = in.readString();
        fileSize = in.readLong();
        albumArtPath = in.readString();
    }

    public static final Creator<AudioMetadata> CREATOR = new Creator<AudioMetadata>() {
        @Override
        public AudioMetadata createFromParcel(Parcel in) {
            return new AudioMetadata(in);
        }

        @Override
        public AudioMetadata[] newArray(int size) {
            return new AudioMetadata[size];
        }
    };

    // Getters
    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @NonNull
    public String getTitle() {
        return title != null && !title.isEmpty() ? title : getFileNameWithoutExtension();
    }

    @NonNull
    public String getArtist() {
        return artist != null && !artist.isEmpty() ? artist : "Unknown Artist";
    }

    @NonNull
    public String getAlbum() {
        return album != null && !album.isEmpty() ? album : "Unknown Album";
    }

    @Nullable
    public String getAlbumArtist() {
        return albumArtist;
    }

    @NonNull
    public String getGenre() {
        return genre != null && !genre.isEmpty() ? genre : "Unknown";
    }

    public int getYear() {
        return year;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getDiscNumber() {
        return discNumber;
    }

    public long getDuration() {
        return duration;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Nullable
    public String getAlbumArtPath() {
        return albumArtPath;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }

    public void setDiscNumber(int discNumber) {
        this.discNumber = discNumber;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
    }

    // Utility methods

    /**
     * Get formatted duration string (MM:SS)
     */
    @NonNull
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /**
     * Get formatted file size string
     */
    @NonNull
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Get display artist name (prefers album artist if available)
     */
    @NonNull
    public String getDisplayArtist() {
        if (albumArtist != null && !albumArtist.isEmpty()) {
            return albumArtist;
        }
        return getArtist();
    }

    /**
     * Get filename without extension from file path
     */
    @NonNull
    private String getFileNameWithoutExtension() {
        if (filePath == null || filePath.isEmpty()) {
            return "Unknown";
        }

        String fileName = filePath;
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < filePath.length() - 1) {
            fileName = filePath.substring(lastSlash + 1);
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            fileName = fileName.substring(0, lastDot);
        }

        return fileName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filePath);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(albumArtist);
        dest.writeString(genre);
        dest.writeInt(year);
        dest.writeInt(trackNumber);
        dest.writeInt(discNumber);
        dest.writeLong(duration);
        dest.writeInt(bitrate);
        dest.writeInt(sampleRate);
        dest.writeString(mimeType);
        dest.writeLong(fileSize);
        dest.writeString(albumArtPath);
    }

    @Override
    @NonNull
    public String toString() {
        return "AudioMetadata{" +
                "title='" + getTitle() + '\'' +
                ", artist='" + getArtist() + '\'' +
                ", album='" + getAlbum() + '\'' +
                ", duration=" + getFormattedDuration() +
                ", bitrate=" + bitrate + " kbps" +
                '}';
    }
}
