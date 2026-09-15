/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 XeniaCloud
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.ecosystem

/**
 * The XeniaCloud companion apps this client can hand an account to.
 *
 * Replaces android-common's `EcosystemApp`, whose package names are hardcoded to
 * upstream's clients. There is no FILES entry: this *is* the Files client.
 *
 * These are the production applicationIds. The dev and QA flavours of those apps carry
 * `.dev`/`.qa` suffixes and are deliberately not listed: the drawer should find the app a
 * user actually installs, not a debug build. Add them here if that ever changes.
 */
enum class XeniaEcosystemApp(val packageNames: List<String>) {
    NOTES(listOf("eu.xeniacloud.notes")),
    TALK(listOf("eu.xeniacloud.talk"))
}
