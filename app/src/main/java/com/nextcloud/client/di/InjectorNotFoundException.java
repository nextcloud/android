/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.di;

class InjectorNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 2026042918255104421L;

    InjectorNotFoundException(Object object, Throwable cause) {
        super(
            String.format(
                "Injector not registered for %s. Have you added it to %s?",
                object.getClass().getName(),
                ComponentsModule.class.getName()
            ),
            cause
        );
    }
}
