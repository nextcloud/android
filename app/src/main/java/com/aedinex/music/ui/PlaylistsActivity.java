/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aedinex.music.library.PlaylistManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.owncloud.android.R;

import java.util.List;

/**
 * Activity for managing playlists.
 */
public class PlaylistsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyLayout;
    private FloatingActionButton fabCreate;

    private PlaylistManager playlistManager;
    private PlaylistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);

        // Initialize manager
        playlistManager = new PlaylistManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_playlists);
        emptyLayout = findViewById(R.id.layout_empty);
        fabCreate = findViewById(R.id.fab_create_playlist);

        // Setup RecyclerView
        adapter = new PlaylistAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup listeners
        fabCreate.setOnClickListener(v -> showCreatePlaylistDialog());

        adapter.setOnPlaylistClickListener(playlist -> {
            // TODO: Open playlist detail activity
        });

        adapter.setOnPlaylistLongClickListener(playlist -> {
            showPlaylistOptionsDialog(playlist);
            return true;
        });

        loadPlaylists();
    }

    private void loadPlaylists() {
        List<PlaylistManager.Playlist> playlists = playlistManager.getAllPlaylists();
        adapter.setPlaylists(playlists);

        if (playlists.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(this);
        input.setHint("Playlist name");

        new AlertDialog.Builder(this)
            .setTitle(R.string.music_action_create_playlist)
            .setView(input)
            .setPositiveButton("Create", (dialog, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    playlistManager.createPlaylist(name);
                    loadPlaylists();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showPlaylistOptionsDialog(PlaylistManager.Playlist playlist) {
        String[] options = {
            "Rename",
            "Delete"
        };

        new AlertDialog.Builder(this)
            .setTitle(playlist.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Rename
                        showRenamePlaylistDialog(playlist);
                        break;
                    case 1: // Delete
                        showDeletePlaylistDialog(playlist);
                        break;
                }
            })
            .show();
    }

    private void showRenamePlaylistDialog(PlaylistManager.Playlist playlist) {
        EditText input = new EditText(this);
        input.setText(playlist.getName());
        input.selectAll();

        new AlertDialog.Builder(this)
            .setTitle("Rename Playlist")
            .setView(input)
            .setPositiveButton("Rename", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    playlistManager.renamePlaylist(playlist.getId(), newName);
                    loadPlaylists();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showDeletePlaylistDialog(PlaylistManager.Playlist playlist) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.music_action_delete_playlist)
            .setMessage("Delete \"" + playlist.getName() + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> {
                playlistManager.deletePlaylist(playlist.getId());
                loadPlaylists();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Simple adapter for playlists RecyclerView
     */
    private static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

        private final List<PlaylistManager.Playlist> playlists = new java.util.ArrayList<>();
        private OnPlaylistClickListener clickListener;
        private OnPlaylistLongClickListener longClickListener;

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            PlaylistManager.Playlist playlist = playlists.get(position);
            holder.text1.setText(playlist.getName());
            holder.text2.setText(playlist.getTrackCount() + " tracks • " + playlist.getFormattedDuration());

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPlaylistClick(playlist);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onPlaylistLongClick(playlist);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }

        public void setPlaylists(List<PlaylistManager.Playlist> newPlaylists) {
            playlists.clear();
            playlists.addAll(newPlaylists);
            notifyDataSetChanged();
        }

        public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
            this.clickListener = listener;
        }

        public void setOnPlaylistLongClickListener(OnPlaylistLongClickListener listener) {
            this.longClickListener = listener;
        }

        interface OnPlaylistClickListener {
            void onPlaylistClick(PlaylistManager.Playlist playlist);
        }

        interface OnPlaylistLongClickListener {
            boolean onPlaylistLongClick(PlaylistManager.Playlist playlist);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView text1;
            android.widget.TextView text2;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}
