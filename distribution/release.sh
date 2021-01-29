#!/bin/bash
set -e

cd `dirname $0`/..
jslehome=`pwd`

if [[ -n $(git status -s) ]]; then
    read -p 'Your workspace contains dirty or untracked files. These will not be part of your release. Continue? [Y/n] ' yesNo
    if [[ -n $yesNo ]] && [[ $yesNo == 'n' ]]; then
        exit 0
    fi
fi

pomversion=`mvn -q help:evaluate -Dexpression=project.version -DforceStdout`
read -p "Enter the new version to set [$pomversion] " newVersion
if [[ -n $newVersion ]]; then
    pomversion=$newVersion
    mvn versions:set -DnewVersion=$newVersion versions:commit
fi

if [[ $pomversion == *-SNAPSHOT ]]; then
    snapshot=1
    d=`date +%Y%m%d%H%M%S`
    version=${pomversion/-SNAPSHOT/}
    release=SNAPSHOT$d
else
    snapshot=0
    version=$pomversion
    release=1  # Incremental release number for a specific version
fi

if [[ -n $(git status -s) ]]; then
    git commit . -v -em"Prepare release jsle-${version}" || :
    if [ $snapshot -eq 0 ]; then
        git tag jsle-$version
    fi
fi

mvn -q clean

clonedir=$jslehome/distribution/target/jsle-clone

mkdir -p $clonedir
git clone . $clonedir
rm -rf $clonedir/.git

cd $clonedir


mvn package -P jsle-release -DskipTests
cp target/jsle-$pomversion.tar.gz $jslehome/distribution/target

cd $jslehome

ls -lh `find distribution/target -maxdepth 1 -type f`
echo

if [ $snapshot -eq 0 ]; then
    read -p "Do you want to stage $pomversion maven artifacts to Maven Central? [y/N] " yesNo
    if [[ $yesNo == 'y' ]]; then
        mvn -f $clonedir -P jsle-release -DskipTests deploy
        echo 'Release the staging repository at https://oss.sonatype.org'
    fi
else
    read -p "Do you want to publish $pomversion maven artifacts to Sonatype Snapshots? [y/N] " yesNo
    if [[ $yesNo == 'y' ]]; then
        mvn -f $clonedir -P jsle-release -DskipTests -DskipStaging deploy
    fi
fi

rm -rf $clonedir $rpmtopdir

# Upgrade version in pom.xml files
# For example: 1.2.3 --> 1.2.4-SNAPSHOT
if [ $snapshot -eq 0 ]; then
    if [[ $version =~ ([0-9]+)\.([0-9]+)\.([0-9]+) ]]; then
        developmentVersion=${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))-SNAPSHOT
        mvn versions:set -DnewVersion=$developmentVersion versions:commit
        git commit . -v -em"Prepare next development iteration"
    else
        echo 'Failed to set development version'
        exit 1
    fi
fi
