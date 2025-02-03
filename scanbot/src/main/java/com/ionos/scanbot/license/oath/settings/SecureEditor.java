/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.settings;

import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;


class SecureEditor implements SharedPreferences.Editor {

	private final SharedPreferences.Editor editor;
	private final SecureEncryptor encryptor;

	public SecureEditor(SharedPreferences.Editor editor, SecureEncryptor encryptor) {
		this.editor = editor;
		this.encryptor = encryptor;
	}

	@Override
	public SharedPreferences.Editor putString(String key, String value) {
		this.editor.putString(key, this.encryptor.encrypt(value));
		return this;
	}

	@Override
	public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
		Set<String> encryptedValues = new HashSet<>();
		for (String value : values) {
			encryptedValues.add(this.encryptor.encrypt(value));
		}
		this.editor.putStringSet(key, encryptedValues);
		return this;
	}

	@Override
	public SharedPreferences.Editor putInt(String key, int value) {
		this.editor.putString(key, this.encryptor.encrypt(String.valueOf(value)));
		return this;
	}

	@Override
	public SharedPreferences.Editor putLong(String key, long value) {
		this.editor.putString(key, this.encryptor.encrypt(String.valueOf(value)));
		return this;
	}

	@Override
	public SharedPreferences.Editor putFloat(String key, float value) {
		this.editor.putString(key, this.encryptor.encrypt(String.valueOf(value)));
		return this;
	}

	@Override
	public SharedPreferences.Editor putBoolean(String key, boolean value) {
		this.editor.putString(key, this.encryptor.encrypt(String.valueOf(value)));
		return this;
	}

	@Override
	public SharedPreferences.Editor remove(String key) {
		this.editor.remove(key);
		return this;
	}

	@Override
	public SharedPreferences.Editor clear() {
		this.editor.clear();
		return this;
	}

	@Override
	public boolean commit() {
		return this.editor.commit();
	}

	@Override
	public void apply() {
		this.editor.apply();
	}
}
