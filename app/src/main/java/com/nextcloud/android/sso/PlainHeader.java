/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.sso;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PlainHeader implements Serializable {
    private static final long serialVersionUID = 3284979177401282512L;

    private String name;
    private String value;

    PlainHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeObject(name);
        oos.writeObject(value);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
        value = (String) in.readObject();
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }
}
