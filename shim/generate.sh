#!/usr/bin/env bash
set -eo pipefail
if [ -z "$1" ]; then
  echo "Must give location of original layoutlib as the first argument." >&2
  exit 1
fi
cp "$1" libs/layoutlib.jar
tar tf libs/layoutlib.jar | grep -vE '^android/view/(IRotationWatcher|IWindowManager)|^android/os/ServiceManager' | xargs zip -d libs/layoutlib.jar
