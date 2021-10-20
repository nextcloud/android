FROM nextcloudci/server:server-17


ARG BRANCH_ARG="stable22"
ENV BRANCH="${BRANCH_ARG}"

ENV EVAL=true

RUN BRANCH="$BRANCH" /usr/local/bin/initnc.sh
RUN su www-data -c "OC_PASS=user1 php /var/www/html/occ user:add --password-from-env --display-name='User One' user1"
RUN su www-data -c "OC_PASS=user2 php /var/www/html/occ user:add --password-from-env --display-name='User Two' user2"
RUN su www-data -c "OC_PASS=user3 php /var/www/html/occ user:add --password-from-env --display-name='User Three' user3"
RUN su www-data -c "php /var/www/html/occ user:setting user2 files quota 1G"
RUN su www-data -c "php /var/www/html/occ group:add users"
RUN su www-data -c "php /var/www/html/occ group:adduser users user1"
RUN su www-data -c "php /var/www/html/occ group:adduser users user2"
RUN su www-data -c "git clone -b $BRANCH https://github.com/nextcloud/activity.git /var/www/html/apps/activity/"
RUN su www-data -c "php /var/www/html/occ app:enable activity"
RUN su www-data -c "git clone -b $BRANCH https://github.com/nextcloud/text.git /var/www/html/apps/text/"
RUN su www-data -c "php /var/www/html/occ app:enable text"
RUN su www-data -c "git clone -b $BRANCH https://github.com/nextcloud/end_to_end_encryption.git /var/www/html/apps/end_to_end_encryption/"
RUN su www-data -c "php /var/www/html/occ app:enable end_to_end_encryption"

ENTRYPOINT /usr/local/bin/run.sh