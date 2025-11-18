/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aedinex.music.library.MusicLibraryManager;
import com.aedinex.music.model.AudioMetadata;
import com.aedinex.music.playback.MusicPlayerController;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.owncloud.android.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity for browsing music library (Songs, Artists, Albums).
 */
public class MusicBrowserActivity extends AppCompatActivity {

    private TabLayout tabs;
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private TextInputEditText searchEdit;
    private TextView tracksCount, artistsCount, albumsCount;
    private View emptyLayout;
    private Button scanButton;
    private FloatingActionButton fabShuffleAll;

    private MusicLibraryManager libraryManager;
    private MusicPlayerController playerController;
    private NowPlayingBottomSheet nowPlayingSheet;

    private int currentTab = 0; // 0=Songs, 1=Artists, 2=Albums

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_browser);

        // Initialize managers
        libraryManager = new MusicLibraryManager(this);
        playerController = MusicPlayerController.getInstance(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.drawer_item_all_files);
        }

        // Initialize views
        tabs = findViewById(R.id.tabs);
        recyclerView = findViewById(R.id.recycler_music);
        searchEdit = findViewById(R.id.edit_search);
        tracksCount = findViewById(R.id.txt_tracks_count);
        artistsCount = findViewById(R.id.txt_artists_count);
        albumsCount = findViewById(R.id.txt_albums_count);
        emptyLayout = findViewById(R.id.layout_empty);
        scanButton = findViewById(R.id.btn_scan_library);
        fabShuffleAll = findViewById(R.id.fab_shuffle_all);

        // Setup RecyclerView
        adapter = new MusicAdapter(MusicAdapter.DisplayMode.TRACK);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup Now Playing bottom sheet
        View bottomSheetView = findViewById(R.id.bottom_sheet_now_playing);
        nowPlayingSheet = new NowPlayingBottomSheet(this, bottomSheetView);

        setupListeners();
        setupLibraryScan();

        // Bind to player service
        playerController.bindService();
    }

    private void setupListeners() {
        // Tab selection
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateDisplayMode();
                loadContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Search
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Scan library button
        scanButton.setOnClickListener(v -> scanLibrary());

        // Shuffle all FAB
        fabShuffleAll.setOnClickListener(v -> shuffleAll());

        // Item click - play track
        adapter.setOnItemClickListener((item, position) -> {
            List<AudioMetadata> queue = adapter.getItems();
            playerController.playQueue(queue, position);
        });

        // Item long click - show context menu
        adapter.setOnItemLongClickListener(this::showContextMenu);
    }

    private void setupLibraryScan() {
        libraryManager.setScanListener(new MusicLibraryManager.LibraryScanListener() {
            @Override
            public void onScanStarted() {
                runOnUiThread(() -> {
                    // TODO: Show progress dialog
                });
            }

            @Override
            public void onScanCompleted(int trackCount) {
                runOnUiThread(() -> {
                    updateStats();
                    loadContent();
                });
            }

            @Override
            public void onScanError(Exception e) {
                runOnUiThread(() -> {
                    // TODO: Show error
                });
            }
        });

        // Auto-scan on first launch
        if (libraryManager.getTrackCount() == 0) {
            scanLibrary();
        } else {
            loadContent();
            updateStats();
        }
    }

    private void scanLibrary() {
        libraryManager.scanLocalLibrary();
    }

    private void updateDisplayMode() {
        MusicAdapter.DisplayMode mode;
        switch (currentTab) {
            case 1: // Artists
                mode = MusicAdapter.DisplayMode.ARTIST;
                break;
            case 2: // Albums
                mode = MusicAdapter.DisplayMode.ALBUM;
                break;
            default: // Songs
                mode = MusicAdapter.DisplayMode.TRACK;
                break;
        }
        adapter.setDisplayMode(mode);
    }

    private void loadContent() {
        List<AudioMetadata> content = new ArrayList<>();

        switch (currentTab) {
            case 0: // Songs
                content = libraryManager.getAllTracks();
                break;
            case 1: // Artists
                content = getArtistsTracks();
                break;
            case 2: // Albums
                content = getAlbumsTracks();
                break;
        }

        adapter.setItems(content);

        if (content.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    private List<AudioMetadata> getArtistsTracks() {
        List<AudioMetadata> result = new ArrayList<>();
        Set<String> artists = libraryManager.getAllArtists();
        for (String artist : artists) {
            List<AudioMetadata> tracks = libraryManager.getTracksByArtist(artist);
            if (!tracks.isEmpty()) {
                result.add(tracks.get(0)); // Use first track to represent artist
            }
        }
        return result;
    }

    private List<AudioMetadata> getAlbumsTracks() {
        List<AudioMetadata> result = new ArrayList<>();
        Set<String> albums = libraryManager.getAllAlbums();
        for (String album : albums) {
            List<AudioMetadata> tracks = libraryManager.getTracksByAlbum(album);
            if (!tracks.isEmpty()) {
                result.add(tracks.get(0)); // Use first track to represent album
            }
        }
        return result;
    }

    private void updateStats() {
        int tracks = libraryManager.getTrackCount();
        int artists = libraryManager.getArtistCount();
        int albums = libraryManager.getAlbumCount();

        tracksCount.setText(tracks + " Songs");
        artistsCount.setText(artists + " Artists");
        albumsCount.setText(albums + " Albums");
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            loadContent();
        } else {
            List<AudioMetadata> results = libraryManager.searchTracks(query);
            adapter.setItems(results);

            if (results.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
            }
        }
    }

    private void shuffleAll() {
        List<AudioMetadata> allTracks = libraryManager.getAllTracks();
        if (!allTracks.isEmpty()) {
            playerController.playQueue(allTracks, 0);
            playerController.toggleShuffle();
        }
    }

    private void showContextMenu(AudioMetadata item, int position) {
        // TODO: Implement context menu with:
        // - Play Now
        // - Play Next
        // - Add to Queue
        // - Add to Playlist
        // - Go to Artist
        // - Go to Album
        // - Delete
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nowPlayingSheet != null) {
            nowPlayingSheet.destroy();
        }
        playerController.unbindService();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Helper method to get items for adapter
    private List<AudioMetadata> getItems() {
        List<AudioMetadata> items = new ArrayList<>();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            AudioMetadata item = adapter.getItem(i);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}
