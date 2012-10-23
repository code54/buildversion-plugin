#!/bin/bash

# Finish the release

if [[ $# -lt 1 ]]; then
  echo "usage: $(basename $0) new-version" >&2
  exit 1
fi

version=$1

echo "finish release of $version"

echo -n "Peform release.  enter to continue:" && read x \
&& mvn release:clean \
&& mvn release:prepare -Dgpg.keyname=02FCB552 \
&& git checkout develop; git merge "release-${version}" \
&& git checkout master; git merge "v${version}" \
&& mvn release:perform -Dgpg.keyname=02FCB552 \
&& mvn nexus:staging-close \
&& mvn nexus:staging-promote


#&& git flow release finish -n $version

