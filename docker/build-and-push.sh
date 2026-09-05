#!/usr/bin/env bash
#
# Build and publish the HashtagCMS Workflows (Java) image to a registry.
#
# Multi-arch (linux/amd64 + linux/arm64) via buildx. By default it tags both the
# exact version and `latest`, and pushes to Docker Hub under `hashtagcms/workflows-java`.
#
# Usage:
#   ./docker/build-and-push.sh                 # build + push :1.0.1 and :latest
#   VERSION=1.2.0 ./docker/build-and-push.sh   # a specific version
#   IMAGE=you/workflows ./docker/build-and-push.sh
#   PUSH=false ./docker/build-and-push.sh      # build a local single-arch image only, no push
#
# Requires `docker login` beforehand (for PUSH=true).
set -euo pipefail

cd "$(dirname "$0")/.."

IMAGE="${IMAGE:-hashtagcms/workflows-java}"
# Default the version to the pom's <version> so the tag always matches the jar.
VERSION="${VERSION:-$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)}"
PLATFORMS="${PLATFORMS:-linux/amd64,linux/arm64}"
PUSH="${PUSH:-true}"

echo "Image:     ${IMAGE}"
echo "Version:   ${VERSION}"
echo "Platforms: ${PLATFORMS}"
echo "Push:      ${PUSH}"
echo

if [ "${PUSH}" = "true" ]; then
  # Multi-arch images must be pushed (buildx can't --load a multi-platform image).
  docker buildx create --use --name hashtagcms-workflows-builder >/dev/null 2>&1 || \
    docker buildx use hashtagcms-workflows-builder
  docker buildx build \
    --platform "${PLATFORMS}" \
    --build-arg "VERSION=${VERSION}" \
    --tag "${IMAGE}:${VERSION}" \
    --tag "${IMAGE}:latest" \
    --push \
    .
  echo
  echo "Pushed ${IMAGE}:${VERSION} and ${IMAGE}:latest (${PLATFORMS})."
else
  # Local build for the current architecture only, loaded into the daemon.
  docker build \
    --build-arg "VERSION=${VERSION}" \
    --tag "${IMAGE}:${VERSION}" \
    --tag "${IMAGE}:latest" \
    .
  echo
  echo "Built ${IMAGE}:${VERSION} locally (not pushed)."
fi
