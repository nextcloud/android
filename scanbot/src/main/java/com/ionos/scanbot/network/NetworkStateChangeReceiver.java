/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkStateChangeReceiver extends BroadcastReceiver {

    public interface NetworkStateChangeReceiverListener {
        void onNetworkStateChanged(Context context, Intent intent, NetworkStateBundle state);
    }

    private volatile static NetworkStateBundle state = null;
    private static final List<NetworkStateChangeReceiverListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkStateBundle newState = NetworkUtils.getNetworkStateBundle(context);
        if (!newState.equals(state)) {
            setState(newState);
            for (NetworkStateChangeReceiverListener networkStateChangeReceiverListener : listeners) {
                networkStateChangeReceiverListener.onNetworkStateChanged(context, intent, state);
            }
        }
    }

    private static void setState(NetworkStateBundle newState) {
        state = newState;
    }

    public static void registerListener(NetworkStateChangeReceiverListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void unregisterListener(NetworkStateChangeReceiverListener listener) {
        listeners.remove(listener);
    }
}
