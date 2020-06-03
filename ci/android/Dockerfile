FROM nextcloudci/android:android-49

ADD emulator.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/*

ENTRYPOINT ["/usr/local/bin/emulator.sh"]
