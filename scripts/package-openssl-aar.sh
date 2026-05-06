#!/usr/bin/env bash
# Nextcloud - Android Client
#
# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# Packages the locally-built OpenSSL 3.5.6 into a prefab-enabled AAR and
# places it in app/libs/local-maven so Gradle can resolve it as a normal
# Maven dependency — no extra setup needed after git clone.
#
# Run once (or whenever you rebuild OpenSSL):
#   bash scripts/package-openssl-aar.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

STAGING_DIR="$REPO_ROOT/../openssl/build-android/aar-staging"
ABIS=("arm64-v8a" "x86_64")
GROUP="com/nextcloud"
ARTIFACT="openssl"
VERSION="3.5.6"
OUT_MAVEN="$REPO_ROOT/app/libs/local-maven/$GROUP/$ARTIFACT/$VERSION"

if [[ ! -d "$STAGING_DIR" ]]; then
    echo "ERROR: staging dir not found: $STAGING_DIR"
    echo "Build OpenSSL for Android first."
    exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

cp -r "$STAGING_DIR/." "$WORK_DIR/"

# ── prefab/prefab.json ──────────────────────────────────────────────────────
mkdir -p "$WORK_DIR/prefab"
cat > "$WORK_DIR/prefab/prefab.json" <<JSON
{
  "schema_version": 2,
  "name": "openssl",
  "version": "$VERSION",
  "dependencies": []
}
JSON

package_module() {
    local MODULE="$1"     # e.g. ssl or crypto
    local LIB="lib${MODULE}.so"
    local EXPORTS="$2"    # JSON array string, e.g. [] or [":crypto"]

    local MOD_DIR="$WORK_DIR/prefab/modules/$MODULE"

    cat > "$MOD_DIR/module.json" <<JSON
{
  "export_libraries": $EXPORTS,
  "library_name": "lib$MODULE",
  "android": {}
}
JSON

    for ABI in "${ABIS[@]}"; do
        local ABI_DIR="$MOD_DIR/libs/android.$ABI"
        mkdir -p "$ABI_DIR"
        cp "$STAGING_DIR/jni/$ABI/$LIB" "$ABI_DIR/$LIB"
        cat > "$ABI_DIR/abi.json" <<JSON
{
  "abi": "$ABI",
  "api": 28,
  "ndk": 29,
  "stl": "none",
  "static": false
}
JSON
    done
}

# crypto has no inter-module deps; ssl depends on crypto
mkdir -p "$WORK_DIR/prefab/modules/crypto"
mkdir -p "$WORK_DIR/prefab/modules/ssl"

package_module "crypto" "[]"
package_module "ssl"    "[\":crypto\"]"

# Headers live once at the package level (both modules share the same include/)
cp -r "$STAGING_DIR/headers/." "$WORK_DIR/prefab/modules/ssl/include/"
cp -r "$STAGING_DIR/headers/." "$WORK_DIR/prefab/modules/crypto/include/"

# ── Write minimal POM ───────────────────────────────────────────────────────
mkdir -p "$OUT_MAVEN"
cat > "$OUT_MAVEN/$ARTIFACT-$VERSION.pom" <<XML
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.nextcloud</groupId>
  <artifactId>openssl</artifactId>
  <version>$VERSION</version>
  <packaging>aar</packaging>
</project>
XML

# ── Zip into AAR ────────────────────────────────────────────────────────────
AAR_FILE="$OUT_MAVEN/$ARTIFACT-$VERSION.aar"
(cd "$WORK_DIR" && zip -r "$AAR_FILE" . -x "*.DS_Store") > /dev/null

echo "✅  AAR written to: $AAR_FILE"
echo "Commit the app/libs/local-maven directory and you are done."
