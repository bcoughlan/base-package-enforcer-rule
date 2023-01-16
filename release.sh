#!/bin/bash

set -euox pipefail

VERSION=$1

if ! git diff-index --quiet HEAD; then 
  echo "Repo is not clean!"
  exit 1
fi

mvn versions:set -DgenerateBackupPoms=false -DnewVersion="$VERSION"
git commit -a -m "Release version $VERSION"
git tag -a "v$VERSION" -m "v$VERSION"

mvn -Prelease clean deploy

mvn versions:set -DgenerateBackupPoms=false -DnewVersion=0
git commit -a -m "Prepare for next version"

git push --atomic origin "refs/tags/v$VERSION" HEAD

gh release create "v$VERSION" --generate-notes