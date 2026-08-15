#!/bin/sh
# One-time setup for a freshly extracted copy. See setup.ps1 for why this is needed.
set -e
cd "$(dirname "$0")"

for candidate in "$ANDROID_HOME" "$ANDROID_SDK_ROOT" "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
    if [ -n "$candidate" ] && [ -d "$candidate/platform-tools" ]; then
        printf 'sdk.dir=%s\n' "$candidate" > local.properties
        echo "SDK found:  $candidate"
        echo "Wrote:      local.properties"
        echo
        echo "Now run:  ./gradlew :app:assembleDebug"
        exit 0
    fi
done

echo "Could not find an Android SDK." >&2
echo "Install it via Android Studio, then re-run. Or: echo 'sdk.dir=/path/to/Sdk' > local.properties" >&2
exit 1
