/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.exception

import kotlin.reflect.KClass

internal class CreateIntentException(
	targetClass: KClass<*>,
) : Exception("${targetClass.java.simpleName} should be created via createIntent")
