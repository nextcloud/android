/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.di.qualifiers

import javax.inject.Qualifier
import kotlin.annotation.AnnotationTarget.*

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
@Target(FIELD, FUNCTION, PROPERTY, VALUE_PARAMETER)
annotation class ScanbotLicenseKeyUrl