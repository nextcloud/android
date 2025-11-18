/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aedinex.music.model.AudioMetadata;
import com.aedinex.music.utils.AudioFileFilter;
import com.owncloud.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying music tracks, albums, or artists.
 */
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private final List<AudioMetadata> items = new ArrayList<>();
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private DisplayMode displayMode = DisplayMode.TRACK;

    public enum DisplayMode {
        TRACK,      // Display individual tracks
        ALBUM,      // Display grouped by album
        ARTIST      // Display grouped by artist
    }

    public MusicAdapter() {
        // Default constructor
    }

    public MusicAdapter(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        AudioMetadata item = items.get(position);
        holder.bind(item, displayMode);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Set the list of items
     */
    public void setItems(List<AudioMetadata> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /**
     * Add item to list
     */
    public void addItem(AudioMetadata item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Remove item from list
     */
    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Clear all items
     */
    public void clearItems() {
        items.clear();
        notifyDataSetChanged();
    }

    /**
     * Get item at position
     */
    public AudioMetadata getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    /**
     * Set display mode
     */
    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        notifyDataSetChanged();
    }

    /**
     * Set click listener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Set long click listener
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    /**
     * ViewHolder class
     */
    static class MusicViewHolder extends RecyclerView.ViewHolder {

        private final TextView text1;
        private final TextView text2;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }

        public void bind(AudioMetadata item, DisplayMode mode) {
            switch (mode) {
                case TRACK:
                    bindTrack(item);
                    break;
                case ALBUM:
                    bindAlbum(item);
                    break;
                case ARTIST:
                    bindArtist(item);
                    break;
            }
        }

        private void bindTrack(AudioMetadata track) {
            text1.setText(track.getTitle());

            String subtitle = track.getArtist();
            String format = AudioFileFilter.getAudioFormatName(track.getFilePath());
            String quality = AudioFileFilter.getAudioQuality(track.getFilePath());

            if (track.getDuration() > 0) {
                subtitle += " • " + track.getFormattedDuration() + " • " + format;
            } else {
                subtitle += " • " + format + " (" + quality + ")";
            }

            text2.setText(subtitle);
        }

        private void bindAlbum(AudioMetadata track) {
            text1.setText(track.getAlbum());
            text2.setText(track.getArtist());
        }

        private void bindArtist(AudioMetadata track) {
            text1.setText(track.getArtist());
            text2.setText(track.getAlbum());
        }
    }

    /**
     * Click listener interface
     */
    public interface OnItemClickListener {
        void onItemClick(AudioMetadata item, int position);
    }

    /**
     * Long click listener interface
     */
    public interface OnItemLongClickListener {
        void onItemLongClick(AudioMetadata item, int position);
    }
}
