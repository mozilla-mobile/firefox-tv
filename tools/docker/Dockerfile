# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Inspired by:
# https://hub.docker.com/r/runmymind/docker-android-sdk/~/dockerfile/

FROM ubuntu:17.10

MAINTAINER Sebastian Kaspari "skaspari@mozilla.com"

# -- System -----------------------------------------------------------------------------

RUN apt-get update -qq

RUN apt-get install -y openjdk-8-jdk \
					   wget \
					   expect \
					   git \
					   curl \
					   ruby \
					   ruby-dev \
					   ruby-build \
					   python \
					   python-pip \
					   optipng \
					   imagemagick \
					   locales
RUN gem install fastlane

RUN locale-gen en_US.UTF-8

# -- Android SDK ------------------------------------------------------------------------

RUN cd /opt && wget -q https://dl.google.com/android/android-sdk_r24.4.1-linux.tgz -O android-sdk.tgz
RUN cd /opt && tar -xvzf android-sdk.tgz
RUN cd /opt && rm -f android-sdk.tgz

ENV ANDROID_SDK_HOME /opt/android-sdk-linux
ENV ANDROID_HOME /opt/android-sdk-linux
ENV PATH ${PATH}:${ANDROID_SDK_HOME}/tools:${ANDROID_SDK_HOME}/platform-tools:/opt/tools

# Platform tools
RUN echo y | android update sdk --no-ui --all --filter platform-tools | grep 'package installed'

# Android SDK
RUN echo y | android update sdk --no-ui --all --filter android-26 | grep 'package installed'

# Build tools
RUN echo y | android update sdk --no-ui --all --filter build-tools-26.0.1 | grep 'package installed'
RUN echo y | android update sdk --no-ui --all --filter build-tools-26.0.2 | grep 'package installed'

# Extras
RUN echo y | android update sdk --no-ui --all --filter extra-android-m2repository | grep 'package installed'
RUN echo y | android update sdk --no-ui --all --filter extra-google-m2repository | grep 'package installed'
RUN echo y | android update sdk --no-ui --all --filter extra-google-google_play_services | grep 'package installed'

# Copy tools from our repository into the docker image
COPY tools /opt/tools

# Accept licenses
COPY licenses ${ANDROID_SDK_HOME}/licenses

# -- Update SDK -------------------------------------------------------------------------

# Update SDK
RUN /opt/tools/android-accept-licenses.sh android update sdk --no-ui --obsolete --force

# -- Project setup ----------------------------------------------------------------------

WORKDIR /opt

# Checkout source code
RUN git clone https://github.com/mozilla-mobile/firefox-tv.git

# Build project and run gradle tasks once to pull all dependencies
WORKDIR /opt/firefox-tv/
RUN ./gradlew clean assemble lint checkstyle ktlint pmd test

# -- Post setup -------------------------------------------------------------------------

# Install dependencies needed to run Android2Po
RUN pip install -r tools/l10n/android2po/requirements.txt

# Install taskcluster python library (used by decision tasks)
RUN pip install taskcluster

# -- Cleanup ----------------------------------------------------------------------------

RUN apt-get clean
