#!/bin/sh

# Nextcloud Android Library
#
# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: MIT
#

wget -O /etc/apt/trusted.gpg.d/php.gpg https://packages.sury.org/php/apt.gpg
apt-get update && apt-get install -y composer 
mkdir /var/www/.nvm /var/www/.npm
mkdir /var/www/.cache/
touch /var/www/.bashrc
chown -R 33:33 /var/www/.nvm /var/www/.npm /var/www/.bashrc /var/www/.cache

cd /var/www/html/
rm data -rf
rm config/config.php
su www-data -c "git reset --hard"
BRANCH="$1" /usr/local/bin/initnc.sh
