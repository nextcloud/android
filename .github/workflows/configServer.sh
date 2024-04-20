#!/bin/sh

# Nextcloud Android Library
#
# SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
# SPDX-License-Identifier: MIT
#

apt-get update && apt-get install -y composer

/usr/local/bin/initnc.sh
