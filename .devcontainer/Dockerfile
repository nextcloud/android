FROM ubuntu:noble@sha256:7a398144c5a2fa7dbd9362e460779dc6659bd9b19df50f724250c62ca7812eb3

ARG DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/usr/lib/android-sdk

RUN apt-get update -y
RUN apt-get install -y unzip wget openjdk-17-jdk vim

RUN wget https://dl.google.com/android/repository/commandlinetools-linux-6858069_latest.zip -O /tmp/commandlinetools.zip
RUN cd /tmp && unzip commandlinetools.zip
RUN mkdir -p /usr/lib/android-sdk/cmdline-tools/
RUN cd /tmp/ && mv cmdline-tools/ latest/ && mv latest/ /usr/lib/android-sdk/cmdline-tools/
RUN mkdir /usr/lib/android-sdk/licenses/
RUN chmod -R 755 /usr/lib/android-sdk/
RUN mkdir -p "$HOME/.gradle" && \
    echo "org.gradle.jvmargs=-Xmx6g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g" > "$HOME/.gradle/gradle.properties" && \
    echo "org.gradle.caching=true" >> "$HOME/.gradle/gradle.properties" && \
    echo "org.gradle.parallel=true" >> "$HOME/.gradle/gradle.properties" && \
    echo "org.gradle.configureondemand=true" >> "$HOME/.gradle/gradle.properties" && \
    echo "kapt.incremental.apt=true" >> "$HOME/.gradle/gradle.properties"