/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.authorization_method

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ViewAuthorizationMethodBinding

class AuthorizationMethodActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun createInstance(context: Context) = Intent(context, AuthorizationMethodActivity::class.java)
    }

    private val viewBinding by lazy { ViewAuthorizationMethodBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        initListeners()
    }

    private fun initListeners() {
        viewBinding.bLogin.setOnClickListener { login() }
    }

    private fun login() {
        val intent = Intent(this, AuthenticatorActivity::class.java).apply {
            putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, true)
        }
        startActivity(intent);
    }
}