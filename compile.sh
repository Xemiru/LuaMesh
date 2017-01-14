#!/bin/bash
set -ev

SOURCE="master"
TARGET="gh-pages"
TAG="latest"

./gradlew check

# dont do extra if we're not master or if its a pr
if [[ "$TRAVIS_BRANCH" != "$SOURCE" || "$TRAVIS_PULL_REQUEST" != "false" ]]; then
	echo "Did not push to $SOURCE or is pull request; building only."
	exit 0
fi

if [[ -n "${TRAVIS_TAG+}" ]]; then
	TAG="$TRAVIS_TAG"
fi

bash ./javadocs.sh "$TAG"
