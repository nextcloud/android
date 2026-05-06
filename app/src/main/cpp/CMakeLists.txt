# Nextcloud - Android Client
#
# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later

cmake_minimum_required(VERSION 3.10.2)

project(cms_verifier VERSION 1.0)

set(OPENSSL_SO_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(OPENSSL_INCLUDE_DIR ${CMAKE_SOURCE_DIR}/openssl_headers)

add_library(
    cms_verifier
    SHARED
    cms_verifier.cpp
)

target_include_directories(cms_verifier PRIVATE ${OPENSSL_INCLUDE_DIR})

target_link_libraries(
    cms_verifier
    -L${OPENSSL_SO_DIR}
    ssl
    crypto
    android
    log
)