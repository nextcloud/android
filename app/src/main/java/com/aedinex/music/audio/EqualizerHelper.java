/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.audio;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;

/**
 * Helper class for audio equalizer and effects.
 * Provides presets and custom equalizer controls.
 */
public class EqualizerHelper {

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    private final int audioSessionId;
    private boolean enabled = false;

    /**
     * Equalizer presets
     */
    public enum Preset {
        FLAT(0),
        ROCK(1),
        POP(2),
        JAZZ(3),
        CLASSICAL(4),
        BASS_BOOST(5),
        TREBLE_BOOST(6),
        VOCAL(7);

        private final int index;

        Preset(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public EqualizerHelper(int audioSessionId) {
        this.audioSessionId = audioSessionId;
        initializeEqualizer();
    }

    /**
     * Initialize equalizer and effects
     */
    private void initializeEqualizer() {
        try {
            // Create equalizer
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(false);

            // Create bass boost
            bassBoost = new BassBoost(0, audioSessionId);
            bassBoost.setEnabled(false);

            // Create virtualizer (surround sound effect)
            virtualizer = new Virtualizer(0, audioSessionId);
            virtualizer.setEnabled(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Enable/disable equalizer
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (equalizer != null) {
            equalizer.setEnabled(enabled);
        }
    }

    /**
     * Check if equalizer is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Apply preset
     */
    public void applyPreset(Preset preset) {
        if (equalizer == null) {
            return;
        }

        switch (preset) {
            case FLAT:
                setFlat();
                break;
            case ROCK:
                setRock();
                break;
            case POP:
                setPop();
                break;
            case JAZZ:
                setJazz();
                break;
            case CLASSICAL:
                setClassical();
                break;
            case BASS_BOOST:
                setBassBoost();
                break;
            case TREBLE_BOOST:
                setTrebleBoost();
                break;
            case VOCAL:
                setVocal();
                break;
        }

        setEnabled(true);
    }

    /**
     * Get number of bands
     */
    public short getNumberOfBands() {
        return equalizer != null ? equalizer.getNumberOfBands() : 0;
    }

    /**
     * Get band level range
     */
    public short[] getBandLevelRange() {
        if (equalizer == null) {
            return new short[]{0, 0};
        }
        return new short[]{
            equalizer.getBandLevelRange()[0],
            equalizer.getBandLevelRange()[1]
        };
    }

    /**
     * Set band level
     */
    public void setBandLevel(short band, short level) {
        if (equalizer != null) {
            try {
                equalizer.setBandLevel(band, level);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get band level
     */
    public short getBandLevel(short band) {
        if (equalizer != null) {
            try {
                return equalizer.getBandLevel(band);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Get center frequency for band
     */
    public int getCenterFreq(short band) {
        if (equalizer != null) {
            try {
                return equalizer.getCenterFreq(band);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Set bass boost strength (0-1000)
     */
    public void setBassBoostStrength(short strength) {
        if (bassBoost != null) {
            try {
                bassBoost.setStrength(strength);
                bassBoost.setEnabled(strength > 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get bass boost strength
     */
    public short getBassBoostStrength() {
        if (bassBoost != null) {
            try {
                return bassBoost.getRoundedStrength();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Set virtualizer strength (0-1000)
     */
    public void setVirtualizerStrength(short strength) {
        if (virtualizer != null) {
            try {
                virtualizer.setStrength(strength);
                virtualizer.setEnabled(strength > 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get virtualizer strength
     */
    public short getVirtualizerStrength() {
        if (virtualizer != null) {
            try {
                return virtualizer.getRoundedStrength();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Release resources
     */
    public void release() {
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if (bassBoost != null) {
            bassBoost.release();
            bassBoost = null;
        }
        if (virtualizer != null) {
            virtualizer.release();
            virtualizer = null;
        }
    }

    // Preset implementations

    private void setFlat() {
        short bands = getNumberOfBands();
        for (short i = 0; i < bands; i++) {
            setBandLevel(i, (short) 0);
        }
        setBassBoostStrength((short) 0);
    }

    private void setRock() {
        // Boost bass and treble, reduce midrange
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 500);  // Bass
            setBandLevel((short) 1, (short) 300);
            setBandLevel((short) 2, (short) -100); // Reduce midrange
            setBandLevel((short) 3, (short) 200);
            setBandLevel((short) 4, (short) 400);  // Treble
        }
        setBassBoostStrength((short) 300);
    }

    private void setPop() {
        // Boost bass slightly, boost vocals
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 200);
            setBandLevel((short) 1, (short) 300);
            setBandLevel((short) 2, (short) 400);  // Boost vocals
            setBandLevel((short) 3, (short) 300);
            setBandLevel((short) 4, (short) 200);
        }
        setBassBoostStrength((short) 200);
    }

    private void setJazz() {
        // Balanced with slight bass and treble boost
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 300);
            setBandLevel((short) 1, (short) 100);
            setBandLevel((short) 2, (short) 100);
            setBandLevel((short) 3, (short) 200);
            setBandLevel((short) 4, (short) 300);
        }
        setBassBoostStrength((short) 100);
    }

    private void setClassical() {
        // Boost treble for clarity
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 0);
            setBandLevel((short) 1, (short) 100);
            setBandLevel((short) 2, (short) 200);
            setBandLevel((short) 3, (short) 300);
            setBandLevel((short) 4, (short) 400);
        }
        setBassBoostStrength((short) 0);
    }

    private void setBassBoost() {
        // Heavy bass boost
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 700);
            setBandLevel((short) 1, (short) 500);
            setBandLevel((short) 2, (short) 200);
            setBandLevel((short) 3, (short) 0);
            setBandLevel((short) 4, (short) 0);
        }
        setBassBoostStrength((short) 800);
    }

    private void setTrebleBoost() {
        // Heavy treble boost
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 0);
            setBandLevel((short) 1, (short) 0);
            setBandLevel((short) 2, (short) 200);
            setBandLevel((short) 3, (short) 500);
            setBandLevel((short) 4, (short) 700);
        }
        setBassBoostStrength((short) 0);
    }

    private void setVocal() {
        // Boost midrange for voice clarity
        if (getNumberOfBands() >= 5) {
            setBandLevel((short) 0, (short) 100);
            setBandLevel((short) 1, (short) 300);
            setBandLevel((short) 2, (short) 500);  // Boost vocals
            setBandLevel((short) 3, (short) 400);
            setBandLevel((short) 4, (short) 200);
        }
        setBassBoostStrength((short) 0);
    }
}
