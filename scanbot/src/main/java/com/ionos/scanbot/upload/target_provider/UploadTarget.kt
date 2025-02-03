/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.upload.target_provider

import java.io.Serializable

/**
 * User: Dima Muravyov
 * Date: 27.05.2019
 */
interface UploadTarget: Serializable {

    val uploadPath: String

}

data class ScanbotUploadTarget(
    override val uploadPath: String,
): UploadTarget