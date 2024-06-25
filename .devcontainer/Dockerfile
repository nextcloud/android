FROM ubuntu:noble@sha256:2e863c44b718727c860746568e1d54afd13b2fa71b160f5cd9058fc436217b30

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
RUN mkdir -p $HOME/.gradle
RUN echo "org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError" > $HOME/.gradle/gradle.properties
