#!/bin/sh

# Nextcloud Android Library
#
# SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
# SPDX-License-Identifier: MIT
#

OC_PASS=user1 php /var/www/html/occ user:add --password-from-env --display-name='User One' user1
OC_PASS=user2 php /var/www/html/occ user:add --password-from-env --display-name='User Two' user2
OC_PASS=user3 php /var/www/html/occ user:add --password-from-env --display-name='User Three' user3
php /var/www/html/occ user:setting user2 files quota 1G
php /var/www/html/occ group:add users
php /var/www/html/occ group:adduser users user1
php /var/www/html/occ group:adduser users user2
git clone -b master https://github.com/nextcloud/activity.git /var/www/html/apps/activity/
php /var/www/html/occ app:enable activity
git clone -b main https://github.com/nextcloud/text.git /var/www/html/apps/text/
php /var/www/html/occ app:enable text
git clone -b master https://github.com/nextcloud/end_to_end_encryption/  /var/www/html/apps/end_to_end_encryption/
php /var/www/html/occ app:enable end_to_end_encryption
git clone https://github.com/nextcloud/photos.git /var/www/html/apps/photos/
cd /var/www/html/apps/photos; composer install
php /var/www/html/occ app:enable -f photos
