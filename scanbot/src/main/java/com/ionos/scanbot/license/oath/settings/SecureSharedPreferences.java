/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.settings;

import android.content.SharedPreferences;

import com.ionos.scanbot.util.logger.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.Keep;


public class SecureSharedPreferences implements SharedPreferences {

	private final SharedPreferences sharedPreferences;
	private final List<OnSharedPreferenceChangeListener> listeners;
	private final SecureEncryptor encryptor;

	public SecureSharedPreferences(SharedPreferences sharedPreferences, SecureEncryptor encryptor) {
		this.sharedPreferences = sharedPreferences;
		this.listeners = new ArrayList<>();
		this.encryptor = encryptor;
		// uses internally WeakHashMap. Do not use lambda, method reference
		this.sharedPreferences.registerOnSharedPreferenceChangeListener(this.onSharedPreferenceChangeListener);
	}

	@Override
	public Map<String, ?> getAll() {
		return new HashMap<String, Object>();
	}

	@Override
	public String getString(String key, String defValue) {
		try {
			String value = this.sharedPreferences.getString(key, null);
			if (value != null) {
				return this.encryptor.decrypt(value);
			}
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return defValue;
	}

	@Override
	public Set<String> getStringSet(String key, Set<String> defValues) {
		Set<String> stringsSet = this.sharedPreferences.getStringSet(key, null);
		try {
			if (stringsSet != null) {
				Set<String> values = new HashSet<>();
				for (String value : stringsSet) {
					values.add(this.encryptor.decrypt(value));
				}
				return values;
			}
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return defValues;
	}

	@Override
	public int getInt(String key, int defValue) {
		try {
			String value = this.sharedPreferences.getString(key, null);
			if (value != null) {
				return Integer.valueOf(this.encryptor.decrypt(value));
			}
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return defValue;
	}

	@Override
	public long getLong(String key, long defValue) {
		try {
			String value = this.sharedPreferences.getString(key, null);
			if (value != null) {
				return Long.valueOf(this.encryptor.decrypt(value));
			}
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return defValue;
	}

	@Override
	public float getFloat(String key, float defValue) {
		try {
			String value = this.sharedPreferences.getString(key, null);
			if (value != null) {
				return Float.valueOf(this.encryptor.decrypt(value));
			}
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return defValue;
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		try {
			String value = this.sharedPreferences.getString(key, null);
			if (value != null) {
				return Boolean.valueOf(this.encryptor.decrypt(value));
			}
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return defValue;
	}

	@Override
	public boolean contains(String key) {
		return this.sharedPreferences.contains(key);
	}

	@Override
	public Editor edit() {
		return new SecureEditor(this.sharedPreferences.edit(), this.encryptor);
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		if (!this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		this.listeners.remove(listener);
	}

	@Keep
	private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
			(sharedPreferences, key) -> onSharedPreferenceChanged(key);

	private void onSharedPreferenceChanged(String key) {
		for (OnSharedPreferenceChangeListener listener : listeners) {
			listener.onSharedPreferenceChanged(SecureSharedPreferences.this, key);
		}
	}

}
