#!/bin/bash -ex

echo "Running $0 ..."
TARGET_DIR="$1"

git config alias.co checkout
git config alias.br branch
git config alias.ci commit
git config alias.st status

#rm -rf $TARGET_DIR
mkdir -p $TARGET_DIR
cd $TARGET_DIR

git init
> README.txt

git add README.txt
git ci -m "Initial commit. Before any tag"

> README-release.txt
> README-master.txt

git add README*
git ci -m "First tagged commit"

git branch develop
git branch release

git co develop

git tag -a -m "v1.0.0-SNAPSHOT" "v1.0.0-SNAPSHOT"

echo 'dev commit 1' >>README.txt
git ci -a -m "dev commit 1"

echo 'dev commit 2' >>README.txt
git ci -a -m "dev commit 2"

git co -b "feature_XYZ"
sleep .1
echo 'feature XYZ commit 1' >>README.txt
git ci -a -m "feature XYZ commit 1"

git co -
git merge --no-ff feature_XYZ

sleep .1
echo 'dev commit 3' >>README.txt
git ci -a -m "dev commit 3"


# Pre-release 1.0.0
git co release
sleep .1
git merge --no-ff develop
git tag -a -m "v1.0.0-RC-SNAPSHOT" "v1.0.0-RC-SNAPSHOT"
git co -
git tag -a -m "v1.1.0-SNAPSHOT" "v1.1.0-SNAPSHOT"

sleep .1
echo 'dev commit 4' >>README.txt
git ci -a -m "dev commit 4"


# fix on pre-release:
git co release
sleep 1
echo 'release commit 1' >>README-release.txt
git ci -a -m "release commit 1"

git co develop
sleep .1
git merge --no-ff release

sleep .1
echo 'dev commit 5' >>README.txt
git ci -a -m "dev commit 5"

# Release
git co master
sleep 1
git merge --no-ff release
git tag -a -m "v1.0.0" "v1.0.0"

sleep 1
git co develop
echo 'dev commit 6' >>README.txt
git ci -a -m "dev commit 6"
echo 'dev commit 7' >>README.txt
git ci -a -m "dev commit 7"
echo 'dev commit 8' >>README.txt
git ci -a -m "dev commit 8"

# another feature
git co develop
git co -b "feature_WXY"
sleep .1
echo 'feature WXY commit 1' >>README.txt
git ci -a -m "feature WXY commit 1"
git co -
git merge --no-ff feature_WXY

sleep .1
echo 'dev commit 9' >>README.txt
git ci -a -m "dev commit 9"

# Pre-release 1.1.0
git co release
sleep .1
git merge --no-ff develop
git tag -a -m "v1.1.0-RC-SNAPSHOT" "v1.1.0-RC-SNAPSHOT"
git co -
git tag -a -m "v1.2.0-SNAPSHOT" "v1.2.0-SNAPSHOT"


# Release
git co master
sleep .1
git merge --no-ff release
git tag -a -m "v1.1.0" "v1.1.0"


sleep .1
git co develop
echo 'dev commit 10' >>README.txt
git ci -a -m "dev commit 10"

# hotfix
sleep 1
git co master
git co -b hotfix
echo 'hotfix commit 1' >>README-master.txt
git ci -a -m "hotfix commit 1"
git co master
sleep .1
git merge --no-ff hotfix
git tag -a -m "v1.1.1" "v1.1.1"
sleep .1
git co develop
git merge --no-ff master
