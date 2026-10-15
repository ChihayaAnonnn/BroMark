#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
IMAGE=${IMAGE:-bromark-v2:java25-it}
PLATFORM=${PLATFORM:-linux/arm64}
docker build --platform "$PLATFORM" -t "$IMAGE" .
docker image inspect "$IMAGE" --format '{{.Id}} {{.Architecture}} {{json .RepoDigests}}'
