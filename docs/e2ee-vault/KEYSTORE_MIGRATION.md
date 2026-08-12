<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# Keystore-backed E2EE secret migration

This document defines the migration plan for local E2EE secrets used by the vault feature.

## Current state

The app currently stores these E2EE setup values through `ArbitraryDataProvider`:

- `EncryptionUtils.PUBLIC_KEY`: actually the public certificate. This is not secret.
- `EncryptionUtils.PRIVATE_KEY`: decrypted private key material. This is secret.
- `EncryptionUtils.MNEMONIC`: user mnemonic/passphrase. This is secret.

The private key and mnemonic are used by:

- E2EE folder metadata decryption and verification;
- E2EE v1 checksum verification;
- E2EE v2 metadata signing and metadata key decryption;
- E2EE upload/create/remove operations;
- certificate renewal;
- settings screens that show the mnemonic after local credentials;
- E2EE setup/import.

The migration must not break existing users who already have plaintext values in `ArbitraryDataProvider`.

## Target storage model

Keep `EncryptionUtils.PUBLIC_KEY` in `ArbitraryDataProvider`.

Move `EncryptionUtils.PRIVATE_KEY` and `EncryptionUtils.MNEMONIC` into a new vault secret store:

- encrypted payload stored in app-private persistent storage through `ArbitraryDataProvider`;
- payload encrypted with AES-GCM;
- AES key generated and held by Android Keystore;
- Keystore key alias derived from the account name through a stable hash, never from the raw account name;
- Keystore key requires recent local user authentication;
- plaintext secret values are only returned to callers while the vault session is unlocked;
- in-memory plaintext is cleared as soon as reasonable for the call site.

Suggested stored payload:

```json
{
  "version": 1,
  "privateKey": "...",
  "mnemonic": "..."
}
```

Suggested `ArbitraryDataProvider` keys:

- `E2EE_VAULT_SECRETS_V1`: encrypted payload and nonce/authentication data;
- `E2EE_VAULT_SECRETS_MIGRATED_V1`: optional marker for diagnostics and guarded cleanup.

## Keystore key policy

Use `AndroidKeyStore` with AES-GCM.

Preferred policy:

- `KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT`;
- block mode `KeyProperties.BLOCK_MODE_GCM`;
- padding `KeyProperties.ENCRYPTION_PADDING_NONE`;
- `setUserAuthenticationRequired(true)`;
- authentication timeout equal to `E2eeVaultSessionConfig.unlockDurationMillis`;
- `KeyProperties.AUTH_BIOMETRIC_STRONG` where available.

Compatibility notes:

- On Android versions where `setUserAuthenticationParameters()` is available, use it.
- On older supported versions, use the older validity-duration API.
- If a device cannot create an auth-bound key, vault unlock must fail closed for E2EE vault access.

## Migration flow

Migration should happen lazily after a successful vault unlock or successful E2EE setup/import.

1. Check whether `E2EE_VAULT_SECRETS_V1` exists.
2. If it exists, decrypt it through Keystore and use it.
3. If it does not exist, read legacy `PRIVATE_KEY` and `MNEMONIC`.
4. If both legacy values are present:
   - create or load the account Keystore key;
   - encrypt both values into `E2EE_VAULT_SECRETS_V1`;
   - immediately decrypt the new payload once to verify it;
   - only after verification, delete legacy `PRIVATE_KEY` and `MNEMONIC`;
   - keep `PUBLIC_KEY` unchanged.
5. If migration fails, leave legacy values untouched and keep the vault locked.

This avoids locking out existing users during a failed migration.

## Setup/import flow

When a user enters the mnemonic for a new device:

1. decrypt the server-stored private key with the mnemonic;
2. verify private/public key matching exactly as today;
3. store `PUBLIC_KEY` in `ArbitraryDataProvider`;
4. store private key and mnemonic through the vault secret store;
5. do not write `PRIVATE_KEY` or `MNEMONIC` back to legacy storage.

When creating new keys:

1. generate the mnemonic and key pair as today;
2. upload the server-encrypted private key as today;
3. store `PUBLIC_KEY` in `ArbitraryDataProvider`;
4. store private key and mnemonic through the vault secret store.

## Access rules after migration

All code that needs `PRIVATE_KEY` or `MNEMONIC` should use a vault-aware provider instead of reading
`ArbitraryDataProvider` directly.

Allowed direct `ArbitraryDataProvider` access:

- `PUBLIC_KEY`;
- non-secret E2EE metadata;
- legacy migration fallback inside the secret store only.

Not allowed:

- direct reads of `PRIVATE_KEY` outside migration;
- direct reads of `MNEMONIC` outside migration;
- direct writes of `PRIVATE_KEY` or `MNEMONIC` after setup migration;
- logging any decrypted secret or encrypted payload contents.

## Removal/reset

`EncryptionUtils.removeE2E()` must delete:

- legacy `PRIVATE_KEY`;
- legacy `MNEMONIC`;
- `PUBLIC_KEY`;
- new encrypted secret payload;
- migration marker;
- Android Keystore key alias for the account.

If Keystore key deletion fails, the reset operation should report a non-fatal local cleanup warning but still
remove database references.

## Test plan

Unit tests:

- stores private key and mnemonic in encrypted payload;
- does not store plaintext values in legacy keys for new setup;
- migrates existing legacy values and deletes them only after verification;
- leaves legacy values untouched if encryption/decryption verification fails;
- reports locked state when no recent local authentication is available;
- deletes both legacy and new values on E2EE reset.

Integration tests:

- existing E2EE setup can still open an encrypted folder after migration;
- new setup can open an encrypted folder without plaintext legacy keys;
- show mnemonic still requires local credentials and reads from the vault secret store;
- metadata v1/v2 decryption works through the vault-aware secret provider.
