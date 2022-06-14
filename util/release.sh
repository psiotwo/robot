#!/usr/bin/env nix-shell
#! nix-shell -i bash -p jdk11_headless maven git gitAndTools.gh travis jq semver-tool gnupg
#
# This script helps to automate ROBOT releases.
# When a manual step is required, it will wait for the user.
# Otherwise it will just go ahead and do the work.

# http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'


### Utility Functions
#
# Prompt for user confirmation.
# Based on https://stackoverflow.com/a/3232082
confirm() {
  # call with a prompt string or use a default
  read -r -p "${1:-Are you sure?} [y/N] " response
  case "$response" in
    [yY][eE][sS]|[yY])
      true
      ;;
    *)
      exit 1
      ;;
  esac
}

STEP="0. Initializing"
step() {
  STEP=$1
  echo "${STEP}"
}


### Initialization

# If there are problems initializing, print usage.
print_usage() {
  echo "Usage: util/release.sh <version>"
}
trap 'print_usage' INT TERM EXIT

# Arguments
VERSION=$1 # the version of this release
NEXT="$(semver bump minor "${VERSION}")-SNAPSHOT" # next version after this release
TOPLEVEL=$(git rev-parse --show-toplevel)

if [ "${TOPLEVEL}" != "$(pwd)" ]; then
  echo "ERROR: Please run this script from the root of the repository as 'util/release.sh'"
  exit 1
fi

if [ ! -f ~/.m2/settings.xml  ]; then
  echo "ERROR: Please supply a ~/.m2/settings.xml file for 'maven deploy'"
  echo "See https://central.sonatype.org/pages/apache-maven.html"
  exit 1
fi

# If the script exits early, print the current step.
print_step() {
  echo "release.sh stopped at: ${STEP}"
}
trap 'print_step' INT TERM EXIT


### Steps

echo "This script will walk you through making a ROBOT release ${VERSION}"
confirm "Ready?"

step "Update git"
git fetch
git status | head -n2
confirm "Correct branch and up to date?"

step "Check GitHub Actions"
curl -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/ontodev/robot/actions/runs \
| jq -e '[.workflow_runs[]|select(.event=="push")][0].conclusion|test("success")'

step "Update OBO context (curie map)"
( cd robot-maven-plugin && mvn install )
mvn robot:UpdateContext -N

step "Set the the version number for this release"
mvn versions:set -DnewVersion="${VERSION}"

step "Check the JavaDocs"
mvn clean site

step "Check the test suite"
mvn clean verify

step "Updating CHANGELOG.md"
< CHANGELOG.md \
  sed "/^## \[Unreleased\]/a## [${VERSION}] - $(date '+%Y-%m-%d')" \
| sed "/^## \[Unreleased\]/G " \
| sed "s/^\[Unreleased\]: /[${VERSION}]: /" \
| sed "s/..HEAD$/..v${VERSION}/" \
| sed "/\[${VERSION}]: /i[Unreleased]: https://github.com/ontodev/robot/compare/v${VERSION}...HEAD" \
> CHANGELOG.new.md
mv CHANGELOG.new.md CHANGELOG.md

step "Manually check CHANGELOG.md and obo_context.jsonld"
confirm "CHANGELOG.md and obo_context.jsonld good?"

echo "Everything looks good!"

# This extra step is required to avoid issue https://github.com/ontodev/robot/issues/411
step "Make a copy of robot.jar for upload to GitHub"
cp bin/robot.jar robot.jar

step "Review changes for release"
git diff
confirm "Commit and push?"
git commit --all --message "Bump version to ${VERSION}"
git push

### Maven Central Release
#
# - documentation at <https://central.sonatype.org/pages/apache-maven.html>
# - make sure your have a username and password in `~/.m2/settings.xml`
# - enter your GPG password, maybe with a dialog window
step "Release to Maven Central"
mvn clean deploy -P release

step "Create draft GitHub release"
< CHANGELOG.md \
  sed -n "/^## \[${VERSION}\]/,/^## /p" \
| sed '1d;2d;$d' \
| sed "s/[][]//g" \
> RELEASE.md
gh release create "v${VERSION}" robot.jar --draft --title "v${VERSION}" --notes-file RELEASE.md

step "Update JAPICMP target version in robot-core/pom.xml"
sed -i "s|<version>\(.*\)</version> <!-- japicmp target -->|<version>${VERSION}</version> <!-- japicmp target -->|" robot-core/pom.xml

step "Set the next SNAPSHOT version number"
mvn versions:set -DnewVersion="${NEXT}"

step "Review changes for SNAPSHOT version"
git diff
confirm "Commit and push?"
git commit --all --message "Bump version to ${NEXT}"
git push

step "Send announcement email"
EMAIL="From: james@overton.ca
To: obo-discuss@googlegroups.com, obo-tools@googlegroups.com
Subject: ROBOT ${VERSION} Released

ROBOT ${VERSION} has been released:

https://github.com/ontodev/robot/releases/tag/v${VERSION}

ROBOT is a command-line tool for automating ontology development tasks. This release includes ...

See our homepage for more information about ROBOT: http://robot.obolibrary.org

James"
if [ "$(command -v draft-email)" ]; then
  [ -z "${IMAP_PASSWORD-}" ] || read -rsp "Enter IMAP password:" IMAP_PASSWORD
  echo "${EMAIL}" | draft-email
  echo "A draft email has been created"
else
  echo "Please send an email like this:"
  echo "${EMAIL}"
fi
confirm "Sent?"

trap '' INT TERM EXIT
echo "Draft GitHub release created for ROBOT ${VERSION}"
echo "Maven Central and javadoc.io should show the new release within 24 hours"

