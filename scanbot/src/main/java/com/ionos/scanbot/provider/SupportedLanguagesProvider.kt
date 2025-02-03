/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.provider

import io.scanbot.sdk.entity.Language
import javax.inject.Inject

internal class SupportedLanguagesProvider @Inject constructor(
) {

    fun get(): Set<Language> = setOf(
        Language.FRA,
        Language.ENG,
    )

}
