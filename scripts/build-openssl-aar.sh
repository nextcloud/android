#!/usr/bin/env bash
# Nextcloud - Android Client
#
# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later
#
# Builds OpenSSL 3.5.6 for Android (arm64-v8a + x86_64) and produces the
# prefab-enabled AAR that is committed to the repository.
#
# Prerequisites:
#   - Android SDK installed (sdk.dir in local.properties or ANDROID_SDK_ROOT set)
#   - Perl available in PATH (comes with macOS / most Linux distros)
#   - OpenSSL 3.5.6 source tarball:
#       curl -LO https://github.com/openssl/openssl/releases/download/openssl-3.5.6/openssl-3.5.6.tar.gz
#
# Usage:
#   bash scripts/build-openssl-aar.sh [/path/to/openssl-3.5.6.tar.gz]
#
# The script re-creates app/libs/local-maven with a fresh AAR. Commit the
# result — no further setup is needed by other developers after git clone.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OPENSSL_VERSION="3.5.6"
ABIS=("arm64-v8a" "x86_64")
MIN_API=28

NDK_VERSION="29.0.14206865"

# Locate the NDK ─────────────────────────────────────────────────────────────
if [[ -z "${ANDROID_NDK_ROOT:-}" ]]; then
    LOCAL_PROPS="$REPO_ROOT/local.properties"
    SDK_DIR=""
    if [[ -f "$LOCAL_PROPS" ]]; then
        SDK_DIR=$(grep "^sdk.dir=" "$LOCAL_PROPS" | cut -d= -f2 | tr -d '[:space:]')
    fi
    SDK_DIR="${SDK_DIR:-${ANDROID_SDK_ROOT:-}}"
    if [[ -z "$SDK_DIR" ]]; then
        echo "ERROR: Cannot locate Android SDK. Set ANDROID_SDK_ROOT or add sdk.dir to local.properties."
        exit 1
    fi
    ANDROID_NDK_ROOT="$SDK_DIR/ndk/$NDK_VERSION"
fi

if [[ ! -d "$ANDROID_NDK_ROOT" ]]; then
    echo "ERROR: NDK not found at $ANDROID_NDK_ROOT"
    echo "Install it via Android Studio → SDK Manager → NDK $NDK_VERSION"
    exit 1
fi

TOOLCHAIN="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/darwin-x86_64"
if [[ ! -d "$TOOLCHAIN" ]]; then
    TOOLCHAIN="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64"
fi

# Locate (or extract) OpenSSL source ─────────────────────────────────────────
TARBALL="${1:-}"
BUILD_BASE="$(mktemp -d)"
trap 'rm -rf "$BUILD_BASE"' EXIT

if [[ -n "$TARBALL" && -f "$TARBALL" ]]; then
    echo "Extracting $TARBALL …"
    tar -xzf "$TARBALL" -C "$BUILD_BASE"
    OPENSSL_SRC="$BUILD_BASE/openssl-$OPENSSL_VERSION"
else
    echo "No tarball supplied — checking for pre-built .so files in sibling openssl/ dir …"
    PREBUILT_INSTALL="$(cd "$REPO_ROOT/.." && pwd)/openssl/build-android/install"
    if [[ ! -d "$PREBUILT_INSTALL/arm64-v8a/lib" ]]; then
        echo "ERROR: Neither a tarball nor pre-built libs were found."
        echo "Provide the tarball: bash scripts/build-openssl-aar.sh /path/to/openssl-$OPENSSL_VERSION.tar.gz"
        exit 1
    fi
    echo "Using pre-built libs from $PREBUILT_INSTALL"
    OPENSSL_SRC=""
fi

ABI_TRIPLES=(
    "arm64-v8a:aarch64-linux-android"
    "x86_64:x86_64-linux-android"
)

declare -A INSTALL_DIRS

if [[ -n "$OPENSSL_SRC" ]]; then
    for ENTRY in "${ABI_TRIPLES[@]}"; do
        ABI="${ENTRY%%:*}"
        TRIPLE="${ENTRY##*:}"
        OPENSSL_PLATFORM="android-arm64"
        [[ "$ABI" == "x86_64" ]] && OPENSSL_PLATFORM="android-x86_64"

        INSTALL_DIR="$BUILD_BASE/install/$ABI"
        INSTALL_DIRS[$ABI]="$INSTALL_DIR"

        echo ""
        echo "── Building OpenSSL $OPENSSL_VERSION for $ABI ──────────────────────────"
        SRC_COPY="$BUILD_BASE/src-$ABI"
        cp -r "$OPENSSL_SRC" "$SRC_COPY"
        (
            cd "$SRC_COPY"
            export ANDROID_NDK_ROOT
            export PATH="$TOOLCHAIN/bin:$PATH"
            # no-asm: disables architecture-specific assembly including SVE probes
            # that can cause SIGILL on devices which incorrectly report SVE support.
            perl Configure "$OPENSSL_PLATFORM" \
                "-D__ANDROID_API__=$MIN_API" \
                "--prefix=$INSTALL_DIR" \
                "--openssldir=$INSTALL_DIR/ssl" \
                no-tests no-fuzz-libfuzzer no-fuzz-afl no-asm \
                shared
            make -j"$(sysctl -n hw.logicalcpu 2>/dev/null || nproc)" build_sw
            make install_sw
        )
    done
else
    for ABI in "${ABIS[@]}"; do
        INSTALL_DIRS[$ABI]="$PREBUILT_INSTALL/$ABI"
    done
fi

# Package into prefab AAR ─────────────────────────────────────────────────────
GROUP="com/nextcloud"
ARTIFACT="openssl"
VERSION="$OPENSSL_VERSION"
OUT_MAVEN="$REPO_ROOT/app/libs/local-maven/$GROUP/$ARTIFACT/$VERSION"

STAGE="$(mktemp -d)"

cp "${INSTALL_DIRS[arm64-v8a]}/../../../openssl/build-android/aar-staging/AndroidManifest.xml" "$STAGE/" 2>/dev/null || \
    cat > "$STAGE/AndroidManifest.xml" <<XML
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.nextcloud.openssl" android:versionCode="1" android:versionName="$VERSION"/>
XML
touch "$STAGE/R.txt"
jar cf "$STAGE/classes.jar" -C /dev/null . 2>/dev/null || touch "$STAGE/classes.jar"

mkdir -p "$STAGE/prefab"
cat > "$STAGE/prefab/prefab.json" <<JSON
{
  "schema_version": 2,
  "name": "openssl",
  "version": "$VERSION",
  "dependencies": []
}
JSON

package_module() {
    local MODULE="$1"
    local EXPORTS="$2"
    local MOD_DIR="$STAGE/prefab/modules/$MODULE"
    mkdir -p "$MOD_DIR"
    cat > "$MOD_DIR/module.json" <<JSON
{
  "export_libraries": $EXPORTS,
  "library_name": "lib$MODULE",
  "android": {}
}
JSON
    local FIRST_ABI="${ABIS[0]}"
    cp -r "${INSTALL_DIRS[$FIRST_ABI]}/include/." "$MOD_DIR/include/"

    for ABI in "${ABIS[@]}"; do
        local ABI_DIR="$MOD_DIR/libs/android.$ABI"
        mkdir -p "$ABI_DIR"
        cp "${INSTALL_DIRS[$ABI]}/lib/lib${MODULE}.so" "$ABI_DIR/lib${MODULE}.so"
        mkdir -p "$STAGE/jni/$ABI"
        cp "${INSTALL_DIRS[$ABI]}/lib/lib${MODULE}.so" "$STAGE/jni/$ABI/"
        cat > "$ABI_DIR/abi.json" <<JSON
{
  "abi": "$ABI",
  "api": $MIN_API,
  "ndk": 29,
  "stl": "none",
  "static": false
}
JSON
    done
}

package_module "crypto" "[]"
package_module "ssl"    "[\":crypto\"]"

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

AAR_FILE="$OUT_MAVEN/$ARTIFACT-$VERSION.aar"
(cd "$STAGE" && zip -r "$AAR_FILE" . -x "*.DS_Store") > /dev/null

echo ""
echo "✅  AAR written to: $AAR_FILE"
echo "Commit app/libs/local-maven and the build will work for everyone after git clone."
