#!/bin/sh

#
# This file is part of Blokada.
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
#
# Copyright © 2023 Blocka AB. All rights reserved.
#
# @author Karol Gusak (karol@blocka.net)
#

set -e

if [ "$#" -ne 1 ]; then
  echo "Error: You must provide the version tag. ./publish.sh TAG" >&2
  exit 1
fi

echo "Publishing Blokada for Android (V6 and Family): $1..."

cd six-android
git co main 
git pull
git co $1
git submodule update

cd ../

commit="android push: $1"
tag="android.v6.$1"
tagFamily="android.family.$1"

git add six-android
git commit -m "$commit"
git tag $tag
git tag $tagFamily

git push
git push --tags

echo "Done"
