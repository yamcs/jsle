#!/bin/bash
set -e

MAVEN_BUCKET=${MAVEN_BUCKET:-gs://yamcs-maven}

# Maven does not set <latest> in maven-metadata.xml (neither when creating it,
# nor when merging into it), so set it to the version just published, and
# update the checksums.
set_latest() {
    local file=$1 version=$2
    VERSION=$version perl -0pi -e '
        s#<latest>[^<]*</latest>#<latest>$ENV{VERSION}</latest># or
        s#(\s*)<versions>#$1<latest>$ENV{VERSION}</latest>$1<versions>#' "$file"
    for algorithm in md5 sha1 sha256 sha512; do
        printf '%s' "$(openssl dgst -$algorithm -r "$file" | cut -d' ' -f1)" > "$file.$algorithm"
    done
}

# Publishing to https://maven.yamcs.org, a Google Cloud Storage bucket with
# releases/ and snapshots/ prefixes, happens in three steps:
#
# 1. prepare_yamcs_maven creates a local staging directory.
# 2. The maven build deploys to it, by invoking the deploy goal directly
#    ($yamcs_maven_goals). In release builds, the deploy phase itself belongs
#    to the central-publishing-maven-plugin, which publishes to Maven Central.
# 3. upload_to_yamcs_maven uploads the staging directory to the bucket.
prepare_yamcs_maven() {
    yamcs_maven_repo=releases
    if [ $snapshot -eq 1 ]; then
        yamcs_maven_repo=snapshots
    fi
    local remote="$MAVEN_BUCKET/$yamcs_maven_repo"

    local groupid=`mvn -q -f $clonedir help:evaluate -Dexpression=project.groupId -DforceStdout`
    local artifactid=`mvn -q -f $clonedir help:evaluate -Dexpression=project.artifactId -DforceStdout`
    local artifactpath=${groupid//.//}/$artifactid

    # Check before building, as the same build may also publish to Maven Central
    local existing
    if [ $snapshot -eq 0 ]; then
        if existing=$(gcloud storage ls "$remote/$artifactpath/$pomversion/" 2>&1); then
            echo "$pomversion is already published to maven.yamcs.org" >&2
            exit 1
        elif [[ $existing != *"matched no objects"* ]]; then
            echo "$existing" >&2
            exit 1
        fi
    fi

    yamcs_maven_staging=`mktemp -d`

    # Seed staging with the published metadata, so that Maven merges into it
    # rather than producing metadata with only this version.
    # (For snapshots, the version-level metadata holds the build number.)
    # One checksum is enough for Maven to validate the metadata.
    local dirs=("$artifactpath")
    if [ $snapshot -eq 1 ]; then
        dirs+=("$artifactpath/$pomversion")
    fi
    local result
    for dir in "${dirs[@]}"; do
        mkdir -p "$yamcs_maven_staging/$dir"
        for file in maven-metadata.xml maven-metadata.xml.sha1; do
            if ! result=$(gcloud storage cp "$remote/$dir/$file" "$yamcs_maven_staging/$dir/" 2>&1); then
                if [[ $result != *"matched no objects"* ]]; then
                    echo "$result" >&2
                    exit 1
                fi
            fi
        done
    done

    yamcs_maven_goals=(
        org.apache.maven.plugins:maven-deploy-plugin:3.1.2:deploy
        -DaltDeploymentRepository=yamcs-maven::file://$yamcs_maven_staging
        -Daether.checksums.algorithms=SHA-512,SHA-256,SHA-1,MD5
    )
}

upload_to_yamcs_maven() {
    local staging=$yamcs_maven_staging
    local remote="$MAVEN_BUCKET/$yamcs_maven_repo"

    # Split what was deployed into artifacts and metadata
    local artifacts=$staging/upload/artifacts
    local metadata=$staging/upload/metadata
    for versiondir in `cd $staging && find org -type d -name "$pomversion"`; do
        local artifactdir=`dirname $versiondir`
        mkdir -p $artifacts/$artifactdir $metadata/$artifactdir
        mv $staging/$versiondir $artifacts/$artifactdir/
        mv $staging/$artifactdir/maven-metadata.xml* $metadata/$artifactdir/
        set_latest $metadata/$artifactdir/maven-metadata.xml $pomversion
        if [ $snapshot -eq 1 ]; then
            mkdir -p $metadata/$versiondir
            mv $artifacts/$versiondir/maven-metadata.xml* $metadata/$versiondir/
        fi
    done

    # Artifacts first, metadata last, so metadata never references
    # artifacts that are not uploaded yet.
    gcloud storage cp -r --no-clobber \
        --cache-control='public, max-age=31536000, immutable' \
        $artifacts/org "$remote/"
    gcloud storage cp -r \
        --cache-control='public, max-age=60' \
        $metadata/org "$remote/"

    rm -rf $staging
    echo "Published to https://maven.yamcs.org/$yamcs_maven_repo/org/yamcs/"
}

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
    central='Maven Central'
else
    central='Sonatype Snapshots'
fi
echo "Where do you want to publish $pomversion maven artifacts?"
echo "  1) $central"
echo "  2) maven.yamcs.org"
echo "  3) Both"
echo "  4) Nowhere"
while true; do
    read -p "Choice [4]: " target
    target=${target:-4}
    if [[ $target =~ ^[1-4]$ ]]; then
        break
    fi
done

# A single build publishes to both, so the artifacts are identical
yamcs_maven_goals=()
if [[ $target == 2 || $target == 3 ]]; then
    prepare_yamcs_maven
fi
if [[ $target == 1 || $target == 3 ]]; then
    if [ $snapshot -eq 0 ]; then
        mvn -f $clonedir -P jsle-release -DskipTests deploy "${yamcs_maven_goals[@]}"
        echo 'Release the staging repository at https://central.sonatype.com'
    else
        mvn -f $clonedir -P jsle-release -DskipTests -DskipStaging deploy "${yamcs_maven_goals[@]}"
    fi
elif [[ $target == 2 ]]; then
    # Not the deploy phase, which would publish to Maven Central
    mvn -f $clonedir -P jsle-release -DskipTests verify "${yamcs_maven_goals[@]}"
fi
if [[ $target == 2 || $target == 3 ]]; then
    upload_to_yamcs_maven
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
