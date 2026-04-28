# Nextcloud - Android Client
#
# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later

cmake_minimum_required(VERSION 3.10.2)

project(cms_verifier VERSION 1.0)

find_package(openssl REQUIRED CONFIG)

add_library(
    cms_verifier
    SHARED
    cms_verifier.cpp
)

target_link_libraries(
    cms_verifier
    openssl::crypto
    android
    log
)
