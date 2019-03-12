# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Inspired by:
# https://hub.docker.com/r/runmymind/docker-android-sdk/~/dockerfile/

FROM ubuntu:18.04

ENV ANDROID_BUILD_TOOLS "27.0.3"

# Version number of the "Command line tools only" SDK download:
# https://developer.android.com/studio/#command-tools
ENV ANDROID_SDK_VERSION "3859397"

ENV ANDROID_PLATFORM_VERSION "27"

ENV PROJECT_REPOSITORY "https://github.com/mozilla-mobile/firefox-tv.git"

# Disable interactive gradle output on taskcluster
ENV TERM dumb

# -- System -----------------------------------------------------------------------------

RUN apt-get update -qq

# We need to install tzdata before all of the other packages. Otherwise it will show an interactive dialog that
# we cannot navigate while building the Docker image.
RUN apt-get install -y tzdata

RUN apt-get install -y openjdk-8-jdk \
	wget \
	expect \
	git \
	curl \
	ruby \
	ruby-dev \
	ruby-build \
	python \
	python-dev \
	python-pip \
	optipng \
	imagemagick \
	locales \
	unzip

RUN gem install fastlane

RUN locale-gen en_US.UTF-8

ENV LANG en_US.UTF-8

# -- Android SDK ------------------------------------------------------------------------

RUN mkdir -p /build/android-sdk
WORKDIR /build

ENV ANDROID_HOME /build/android-sdk
ENV ANDROID_SDK_HOME /build/android-sdk
ENV PATH ${PATH}:${ANDROID_SDK_HOME}/tools:${ANDROID_SDK_HOME}/tools/bin:${ANDROID_SDK_HOME}/platform-tools:/opt/tools:${ANDROID_SDK_HOME}/build-tools/${ANDROID_BUILD_TOOLS}

RUN curl -L https://dl.google.com/android/repository/sdk-tools-linux-${ANDROID_SDK_VERSION}.zip > sdk.zip \
        && unzip sdk.zip -d ${ANDROID_SDK_HOME} \
        && rm sdk.zip

RUN mkdir -p /build/android-sdk/.android/
RUN touch /build/android-sdk/.android/repositories.cfg

RUN yes | sdkmanager --licenses

# -- Project setup ----------------------------------------------------------------------

WORKDIR /opt

# Checkout source code
RUN git clone --depth=1 ${PROJECT_REPOSITORY}

# Build project and run gradle tasks once to pull all dependencies
WORKDIR /opt/firefox-tv/
RUN ./gradlew --no-daemon clean assembleDebug lint checkstyle ktlint pmd detekt testSystemDebugUnitTest \
    && ./gradlew --no-daemon clean

# -- Post setup -------------------------------------------------------------------------

# Install dependencies needed to run Android2Po
RUN pip install -r tools/l10n/android2po/requirements.txt

# Install taskcluster python library (used by decision tasks)
RUN pip install 'taskcluster>=4,<5'

# Install Python client for Testdroid Cloud APIv2 (used for running UI tests on Bitbar Cloud)
RUN pip install testdroid
