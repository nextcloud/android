/*
 * Nextcloud application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import android.accounts.Account;

import org.parceler.Parcel;

@Parcel
public class SignatureVerification {
    public boolean signatureValid;
    public Account account;

    public SignatureVerification(boolean signatureValid, Account account) {
        this.signatureValid = signatureValid;
        this.account = account;
    }

    public SignatureVerification() {
        // empty constructor
    }

    public boolean isSignatureValid() {
        return this.signatureValid;
    }

    public Account getAccount() {
        return this.account;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
