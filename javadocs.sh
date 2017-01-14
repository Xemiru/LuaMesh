#!/bin/bash
set -ev

# get the version we're using and our commit
COMMIT=`git rev-parse --verify HEAD`
VERSION="$1"

# get our deploy key ready
openssl aes-256-cbc -K $encrypted_1ebe7c3c7313_key -iv $encrypted_1ebe7c3c7313_iv -in gh_pages.enc -out gh_pages -d
chmod 600 gh_pages
eval `ssh-agent -s`
ssh-add gh_pages

# clone our gh-pages branch
cd "$TRAVIS_BUILD_DIR/../"
git clone -b gh-pages -- https://github.com/Xemiru/LuaMesh.git jdocs
cd jdocs

# get into our version directory and clean it out
rm -rf ./$VERSION
mkdir $VERSION
cd $VERSION

# generate jdocs
CACHEDIR="$HOME/.gradle/caches/modules-2/files-2.1"
javadoc -sourcepath "$TRAVIS_BUILD_DIR/src/main/java/" -classpath "$CACHEDIR/org.luaj/luaj-jse/3.0.1/99245b2df284805e1cb835e9be47c243f9717511/luaj-jse-3.0.1.jar" -subpackages com.github.xemiru.luamesh -d ./

if [[ "$VERSION" != "latest" ]]; then
    cd "$TRAVIS_BUILD_DIR/../jdocs/"
	rm -rf "./latest"
	cp -r "$VERSION" "latest"
fi

# push it to the repo
git config --global user.name "Travis CI"
git config --global user.email "xemiruk@gmail.com"
git add -A
git commit -m "Update javadocs for version $VERSION: $COMMIT"
git push git@github.com:Xemiru/LuaMesh.git gh-pages
