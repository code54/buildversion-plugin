#!/bin/bash

# start the release

if [[ $# -lt 2 ]]; then
  echo "usage: $(basename $0) previous-version new-version" >&2
  exit 1
fi

previous_version=$1
version=$2

echo ""
echo "Start release of $version, previous version is $previous_version"
echo ""
echo ""

git co -b "release-$version" || exit 1

#git flow release start $version || exit 1

echo ""
echo ""
echo "Changes since $previous_version"
git log --oneline  v$previous_version..
echo ""
echo ""
echo "Press [enter] to edit ReleaseNotes and README and other files refering to version number..."
read


$EDITOR ReleaseNotes.md
$EDITOR README.md
$EDITOR src/it/setting-git-cmd/pom.xml src/it/custom-properties/pom.xml src/it/simple-project/pom.xml

