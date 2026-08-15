#!/bin/sh
#
# Self-contained Gradle wrapper.
#
# WHY THIS DIFFERS FROM THE GENERATED ONE: Gradle's gradlew is a thin launcher that delegates to
# gradle-wrapper.jar, a compiled binary. That jar could not be shipped in this archive, so this
# script performs the jar's job itself in POSIX shell: read gradle-wrapper.properties, download and
# unpack the distribution under GRADLE_USER_HOME, then hand off to the real gradle launcher.
#
# It needs no gradle-wrapper.jar and no globally installed Gradle. It needs curl or wget once, on
# first run only; after that it is entirely offline.
#
# FIRST THING TO DO AFTER A SUCCESSFUL FIRST RUN:
#
#     ./gradlew wrapper
#
# That regenerates the canonical gradlew, gradlew.bat and gradle-wrapper.jar from the Gradle
# distribution this script just installed, replacing these files with the official ones. Recommended,
# and it changes no behaviour.
#

set -e

APP_HOME=$(cd "$(dirname "$0")" > /dev/null && pwd -P)
PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

die() {
    echo >&2
    echo "ERROR: $*" >&2
    echo >&2
    exit 1
}

# ---- Java ------------------------------------------------------------------------------------
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
    [ -x "$JAVACMD" ] || die "JAVA_HOME points at an invalid directory: $JAVA_HOME

Set JAVA_HOME to a JDK 17 installation, or unset it and put java on your PATH."
else
    JAVACMD=$(command -v java) || die "No Java found.

Install JDK 17, then set JAVA_HOME or put java on your PATH.
This project targets Java 17. JDK 21 produces errors that look like Kotlin problems but are not."
fi

# ---- Distribution ----------------------------------------------------------------------------
[ -f "$PROPERTIES" ] || die "Missing $PROPERTIES"

# Read the URL, un-escaping the '\:' that Java properties files require.
DISTRIBUTION_URL=$(sed -n 's/^distributionUrl=//p' "$PROPERTIES" | tr -d '\r' | sed 's/\\:/:/g')
DISTRIBUTION_PATH=$(sed -n 's/^distributionPath=//p' "$PROPERTIES" | tr -d '\r')
EXPECTED_SHA=$(sed -n 's/^distributionSha256Sum=//p' "$PROPERTIES" | tr -d '\r')
[ -n "$DISTRIBUTION_URL" ] || die "distributionUrl is not set in $PROPERTIES"
[ -n "$DISTRIBUTION_PATH" ] || DISTRIBUTION_PATH="wrapper/dists"

GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
ARCHIVE_NAME=$(basename "$DISTRIBUTION_URL")
BASE_NAME=$(echo "$ARCHIVE_NAME" | sed 's/\(-bin\|-all\)\{0,1\}\.zip$//')
INSTALL_DIR="$GRADLE_USER_HOME/$DISTRIBUTION_PATH/$BASE_NAME/shell-wrapper"
MARKER="$INSTALL_DIR/.installed"

find_gradle_home() {
    for candidate in "$INSTALL_DIR"/*/ ; do
        if [ -d "${candidate}bin" ] && [ -d "${candidate}lib" ] ; then
            echo "${candidate%/}"
            return 0
        fi
    done
    return 1
}

if [ ! -f "$MARKER" ] ; then
    mkdir -p "$INSTALL_DIR"
    ARCHIVE="$INSTALL_DIR/$ARCHIVE_NAME"

    if [ ! -f "$ARCHIVE" ] ; then
        echo "Downloading $DISTRIBUTION_URL"
        # Download to .part and rename on success, so an interrupted download is never mistaken for
        # a usable archive on the next run.
        if command -v curl > /dev/null 2>&1 ; then
            curl -fL --connect-timeout 30 -o "$ARCHIVE.part" "$DISTRIBUTION_URL" \
                || die "Download failed: $DISTRIBUTION_URL

If you are offline or behind a proxy, download that file by hand, place it at
  $ARCHIVE
and run this script again."
        elif command -v wget > /dev/null 2>&1 ; then
            wget -O "$ARCHIVE.part" "$DISTRIBUTION_URL" || die "Download failed: $DISTRIBUTION_URL"
        else
            die "Neither curl nor wget is available.

Download $DISTRIBUTION_URL by hand, place it at
  $ARCHIVE
and run this script again."
        fi
        mv "$ARCHIVE.part" "$ARCHIVE"
    fi

    if [ -n "$EXPECTED_SHA" ] ; then
        if command -v sha256sum > /dev/null 2>&1 ; then
            ACTUAL_SHA=$(sha256sum "$ARCHIVE" | cut -d' ' -f1)
        elif command -v shasum > /dev/null 2>&1 ; then
            ACTUAL_SHA=$(shasum -a 256 "$ARCHIVE" | cut -d' ' -f1)
        else
            ACTUAL_SHA=""
        fi
        if [ -n "$ACTUAL_SHA" ] && [ "$ACTUAL_SHA" != "$EXPECTED_SHA" ] ; then
            rm -f "$ARCHIVE"
            die "Checksum mismatch for $ARCHIVE_NAME
  expected: $EXPECTED_SHA
  actual:   $ACTUAL_SHA
The download was deleted. Check distributionSha256Sum against https://gradle.org/release-checksums/"
        fi
    fi

    echo "Unpacking to $INSTALL_DIR"
    if command -v unzip > /dev/null 2>&1 ; then
        unzip -q -o "$ARCHIVE" -d "$INSTALL_DIR"
    else
        # Every JDK ships jar, which reads zip. Avoids depending on unzip being installed.
        (cd "$INSTALL_DIR" && "$JAVA_HOME/bin/jar" xf "$ARCHIVE" 2>/dev/null) \
            || die "Neither unzip nor the jar tool is available to unpack $ARCHIVE"
    fi

    GRADLE_HOME=$(find_gradle_home) || die "Unpacked distribution contains no Gradle home under $INSTALL_DIR"
    chmod +x "$GRADLE_HOME/bin/gradle"
    rm -f "$ARCHIVE"
    touch "$MARKER"
fi

GRADLE_HOME=$(find_gradle_home) || die "Gradle installation at $INSTALL_DIR looks corrupt.

Delete that directory and run this script again to re-download."

# ---- Run -------------------------------------------------------------------------------------
JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(command -v java)")")}
export JAVA_HOME
export GRADLE_HOME
exec "$GRADLE_HOME/bin/gradle" "$@"
