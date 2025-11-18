/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.playback;

import com.aedinex.music.model.AudioMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the playback queue with shuffle, repeat, and queue manipulation.
 */
public class QueueManager {

    private final List<AudioMetadata> originalQueue = new ArrayList<>();
    private final List<AudioMetadata> shuffledQueue = new ArrayList<>();
    private final List<Integer> playHistory = new ArrayList<>();

    private boolean shuffleEnabled = false;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private int currentIndex = -1;

    private final Random random = new Random();

    public enum RepeatMode {
        OFF,
        ALL,
        ONE
    }

    /**
     * Set the queue with a list of tracks
     */
    public void setQueue(List<AudioMetadata> tracks, int startIndex) {
        originalQueue.clear();
        originalQueue.addAll(tracks);
        shuffledQueue.clear();
        playHistory.clear();

        currentIndex = startIndex;

        if (shuffleEnabled) {
            createShuffledQueue();
        }
    }

    /**
     * Get current queue (shuffled or original)
     */
    public List<AudioMetadata> getCurrentQueue() {
        return shuffleEnabled ? new ArrayList<>(shuffledQueue) : new ArrayList<>(originalQueue);
    }

    /**
     * Get current track
     */
    public AudioMetadata getCurrentTrack() {
        List<AudioMetadata> queue = getCurrentQueue();
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            return queue.get(currentIndex);
        }
        return null;
    }

    /**
     * Move to next track
     */
    public AudioMetadata getNextTrack() {
        if (repeatMode == RepeatMode.ONE) {
            return getCurrentTrack();
        }

        List<AudioMetadata> queue = getCurrentQueue();

        if (currentIndex < queue.size() - 1) {
            currentIndex++;
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0;
        } else {
            return null; // End of queue
        }

        playHistory.add(currentIndex);
        return getCurrentTrack();
    }

    /**
     * Move to previous track
     */
    public AudioMetadata getPreviousTrack() {
        if (!playHistory.isEmpty()) {
            playHistory.remove(playHistory.size() - 1);
            if (!playHistory.isEmpty()) {
                currentIndex = playHistory.get(playHistory.size() - 1);
                return getCurrentTrack();
            }
        }

        if (currentIndex > 0) {
            currentIndex--;
            return getCurrentTrack();
        }

        return getCurrentTrack();
    }

    /**
     * Jump to specific index
     */
    public AudioMetadata jumpToIndex(int index) {
        List<AudioMetadata> queue = getCurrentQueue();
        if (index >= 0 && index < queue.size()) {
            currentIndex = index;
            playHistory.add(index);
            return getCurrentTrack();
        }
        return null;
    }

    /**
     * Add track to end of queue
     */
    public void addToQueue(AudioMetadata track) {
        originalQueue.add(track);
        if (shuffleEnabled) {
            shuffledQueue.add(track);
        }
    }

    /**
     * Play next (add after current track)
     */
    public void playNext(AudioMetadata track) {
        int insertIndex = currentIndex + 1;
        originalQueue.add(insertIndex, track);

        if (shuffleEnabled) {
            shuffledQueue.add(insertIndex, track);
        }
    }

    /**
     * Remove track from queue
     */
    public boolean removeFromQueue(int index) {
        List<AudioMetadata> queue = getCurrentQueue();
        if (index >= 0 && index < queue.size()) {
            if (shuffleEnabled) {
                AudioMetadata track = shuffledQueue.remove(index);
                originalQueue.remove(track);
            } else {
                originalQueue.remove(index);
            }

            if (index < currentIndex) {
                currentIndex--;
            } else if (index == currentIndex) {
                // Current track removed, stay at same index (which now points to next track)
            }
            return true;
        }
        return false;
    }

    /**
     * Move track in queue
     */
    public boolean moveTrack(int fromIndex, int toIndex) {
        List<AudioMetadata> queue = getCurrentQueue();
        if (fromIndex >= 0 && fromIndex < queue.size() &&
            toIndex >= 0 && toIndex < queue.size()) {

            AudioMetadata track = queue.remove(fromIndex);
            queue.add(toIndex, track);

            // Update current index if needed
            if (fromIndex == currentIndex) {
                currentIndex = toIndex;
            } else if (fromIndex < currentIndex && toIndex >= currentIndex) {
                currentIndex--;
            } else if (fromIndex > currentIndex && toIndex <= currentIndex) {
                currentIndex++;
            }

            return true;
        }
        return false;
    }

    /**
     * Clear the queue
     */
    public void clearQueue() {
        originalQueue.clear();
        shuffledQueue.clear();
        playHistory.clear();
        currentIndex = -1;
    }

    /**
     * Toggle shuffle mode
     */
    public void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;

        if (shuffleEnabled) {
            createShuffledQueue();
        } else {
            // Map current track back to original queue
            AudioMetadata currentTrack = getCurrentTrack();
            if (currentTrack != null) {
                currentIndex = originalQueue.indexOf(currentTrack);
            }
            shuffledQueue.clear();
        }
    }

    /**
     * Create shuffled version of queue
     */
    private void createShuffledQueue() {
        AudioMetadata currentTrack = null;
        if (currentIndex >= 0 && currentIndex < originalQueue.size()) {
            currentTrack = originalQueue.get(currentIndex);
        }

        shuffledQueue.clear();
        shuffledQueue.addAll(originalQueue);
        Collections.shuffle(shuffledQueue, random);

        // Ensure current track stays at current position
        if (currentTrack != null) {
            int shuffledIndex = shuffledQueue.indexOf(currentTrack);
            if (shuffledIndex != currentIndex && shuffledIndex >= 0) {
                Collections.swap(shuffledQueue, currentIndex, shuffledIndex);
            }
        }
    }

    /**
     * Cycle repeat mode
     */
    public RepeatMode cycleRepeatMode() {
        switch (repeatMode) {
            case OFF:
                repeatMode = RepeatMode.ALL;
                break;
            case ALL:
                repeatMode = RepeatMode.ONE;
                break;
            case ONE:
                repeatMode = RepeatMode.OFF;
                break;
        }
        return repeatMode;
    }

    /**
     * Set repeat mode
     */
    public void setRepeatMode(RepeatMode mode) {
        this.repeatMode = mode;
    }

    /**
     * Get queue size
     */
    public int getQueueSize() {
        return originalQueue.size();
    }

    /**
     * Get current index
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Check if has next track
     */
    public boolean hasNext() {
        if (repeatMode != RepeatMode.OFF) {
            return true;
        }
        return currentIndex < getCurrentQueue().size() - 1;
    }

    /**
     * Check if has previous track
     */
    public boolean hasPrevious() {
        return currentIndex > 0 || !playHistory.isEmpty();
    }

    // Getters
    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }
}
