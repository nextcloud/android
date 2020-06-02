FROM nextcloudci/server:server-17

RUN mkdir /var/www/html/data
RUN chown -R www-data:www-data /var/www/html/data

RUN /usr/local/bin/initnc.sh

ENTRYPOINT /usr/local/bin/run.sh
