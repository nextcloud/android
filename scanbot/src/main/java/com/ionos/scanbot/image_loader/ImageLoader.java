
/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.image_loader;

import java.io.File;

import androidx.annotation.NonNull;

public interface ImageLoader {

	ImageRequestBuilder load(@NonNull File file);

}
