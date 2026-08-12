<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# E2EE vault tasks

Status values:

- `TODO`: not started.
- `IN_PROGRESS`: currently being implemented or analyzed.
- `REVIEW`: completed by the agent and waiting for human validation.
- `DONE`: validated explicitly by a human reviewer.

No task is marked `DONE` until it has been explicitly validated.

| Status | Task | Scope |
| --- | --- | --- |
| REVIEW | Analyze current E2EE architecture | Detection, metadata, key flow, downloads, decryption and storage. |
| REVIEW | Analyze current media architecture | Thumbnails, gallery rows, image viewer, video player and media tests. |
| REVIEW | Propose target vault architecture | Session, biometric gate, secret store, thumbnails, image source and video source. |
| DONE | Decide AndroidX Biometric dependency integration | Verify dependency/version, add wrapper and keep dependency impact minimal. |
| DONE | Implement `E2eeVaultSession` | Track unlocked vault sessions, expiry and explicit lock. |
| DONE | Implement `VaultBiometricManager` | Prompt on encrypted folder entry and return success/failure/cancel results. |
| DONE | Gate encrypted folder entry | Update `OCFileListFragment.folderOnItemClick()` so failed/cancelled authentication does not browse into the folder. |
| DONE | Add vault session tests | Verify encrypted folder requires auth, success grants access, cancel/failure blocks access and normal folders are unchanged. |
| REVIEW | Integrate vault lock with app lifecycle | Lock vault sessions when the app is backgrounded long enough or the device is locked. |
| REVIEW | Design Keystore-backed E2EE secret migration | Define migration from plaintext `ArbitraryDataProvider` values without losing existing setups. |
| REVIEW | Implement Keystore-backed E2EE secret storage | Protect private key and mnemonic with Android Keystore and recent authentication where supported. |
| REVIEW | Refactor metadata secret access | Make v1/v2 metadata decryption obtain secrets through an explicit vault-aware provider. |
| REVIEW | Add metadata and secret storage tests | Cover successful secret access, locked access, missing keys, wrong key and migration failure. |
| REVIEW | Block server previews for E2EE thumbnails | Ensure thumbnail code never calls server preview endpoints for encrypted media. |
| REVIEW | Implement local E2EE thumbnail provider | Download ciphertext if needed, decrypt locally after unlock, decode thumbnail and apply video overlay. |
| REVIEW | Implement session-scoped thumbnail memory cache | Cache plaintext thumbnails only in memory and clear them on vault lock. |
| REVIEW | Add thumbnail tests | Cover generation, cache hit/miss, invalidation and absence of server preview calls for E2EE files. |
| REVIEW | Implement E2EE image preview source | Reuse existing image viewer while avoiding persistent plaintext image files. |
| REVIEW | Add E2EE image viewer cleanup | Release bitmaps, streams and plaintext references when viewer closes or vault locks. |
| REVIEW | Add image viewer tests | Cover open, corrupt file, invalid tag, close cleanup and background lock behavior. |
| REVIEW | Implement E2EE video media source design | Choose Media3 `DataSource`, pipe or internal provider based on no-plaintext persistence constraints. |
| REVIEW | Implement E2EE video playback | Download full ciphertext, authenticate/decrypt locally and stream plaintext only to in-app player. |
| REVIEW | Add video cleanup and lock behavior | Stop player, close streams and invalidate media handles when the vault locks. |
| REVIEW | Add video tests | Cover playback, interrupted download, invalid tag, missing storage, deleted file and session expiry. |
| REVIEW | Apply strict secure display mode for vault content | Enable `FLAG_SECURE` while encrypted folder/media content is visible. |
| REVIEW | Add normal-folder non-regression tests | Verify normal folders, previews, thumbnails, downloads and streaming keep existing behavior. |
| REVIEW | Write vault security documentation | Create `docs/e2ee-vault/SECURITY.md` with covered and uncovered threats. |
| REVIEW | Run focused quality checks | Run targeted unit/instrumented tests for changed areas, plus lint/detekt/spotless where practical. |
| REVIEW | Fix manual-test vault media regressions | Route encrypted media to the vault viewer, improve decrypt diagnostics and avoid repeated failed thumbnail decrypts. |
| REVIEW | Fix E2EE media authentication tag validation | Compare metadata authentication tags with the GCM tag stored in ciphertext instead of the cipher IV. |
| REVIEW | Fix E2EE encrypted video source compatibility | Improve local video thumbnail extraction and player media source metadata without persisting plaintext. |
