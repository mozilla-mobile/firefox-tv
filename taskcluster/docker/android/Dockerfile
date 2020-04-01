# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Inspired by:
# https://hub.docker.com/r/runmymind/docker-android-sdk/~/dockerfile/

FROM ubuntu:18.04


# Add worker user
RUN mkdir /builds && \
    useradd -d /builds/worker -s /bin/bash -m worker && \
    chown worker:worker /builds/worker && \
    mkdir /builds/worker/artifacts && \
    chown worker:worker /builds/worker/artifacts

WORKDIR /builds/worker/


ENV ANDROID_BUILD_TOOLS "27.0.3"

ENV ANDROID_SDK_ROOT '/builds/worker/android-sdk-linux'

# Version number of the "Command line tools only" SDK download:
# https://developer.android.com/studio/#command-tools
ENV ANDROID_SDK_VERSION "3859397"

ENV ANDROID_PLATFORM_VERSION "27"

ENV CURL 'curl --location --retry 5'

ENV PROJECT_REPOSITORY "https://github.com/mozilla-mobile/firefox-tv.git"

ENV SDK_ZIP_LOCATION "$HOME/sdk-tools-linux.zip"

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

RUN $CURL --output "$SDK_ZIP_LOCATION" "https://dl.google.com/android/repository/sdk-tools-linux-${ANDROID_SDK_VERSION}.zip" \
    && unzip -d "$ANDROID_SDK_ROOT" "$SDK_ZIP_LOCATION" \
    && rm "$SDK_ZIP_LOCATION" \
    && yes | "${ANDROID_SDK_ROOT}/tools/bin/sdkmanager" --licenses \
    && chown -R worker:worker "$ANDROID_SDK_ROOT"

# Install dependencies needed to run Android2Po
# %include tools/l10n/android2po/requirements.txt
COPY topsrcdir/tools/l10n/android2po/requirements.txt android2po_requirements.txt
RUN pip install -r android2po_requirements.txt

# Install taskcluster python library (used by decision tasks)
RUN pip install taskcluster

# Install Python client for Testdroid Cloud APIv2 (used for running UI tests on Bitbar Cloud)
RUN pip install testdroid


# %include-run-task

ENV SHELL=/bin/bash \
    HOME=/builds/worker \
    PATH="/builds/worker/.local/bin:$PATH"


VOLUME /builds/worker/checkouts
VOLUME /builds/worker/.cache


# run-task expects to run as root
USER root
