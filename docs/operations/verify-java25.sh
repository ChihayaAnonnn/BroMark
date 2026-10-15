#!/usr/bin/env sh
set -eu
: "${V2_MYSQL_PASSWORD:?set isolated MySQL password}"
: "${V2_RABBIT_PASSWORD:?set isolated RabbitMQ password}"
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
cache_dir=${MAVEN_CACHE_DIR:-/tmp/bromark-java25-m2}
build_image=maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976
export ROUTER_MYSQL_PASSWORD="$V2_MYSQL_PASSWORD"
export ROUTER_MYSQL_HOST=${ROUTER_MYSQL_HOST:-${V2_MYSQL_HOST:-host.docker.internal:13316}}
mkdir -p "$cache_dir"
if [ -n "${V2_TEST_DOCKER_NETWORK:-}" ]; then set -- --network "$V2_TEST_DOCKER_NETWORK"; else set --; fi
test_selector=${V2_TEST_SELECTOR:-'Router*Test,*V2Test,Java25RouterIntegrationTest,V2FoundationWiringTest,AliasMethodAlgorithmTest'}
docker run --rm "$@" -e V2_REDIS_CLUSTER_NODES -e ROUTER_MYSQL_PASSWORD -e ROUTER_MYSQL_HOST \
  -e V2_MYSQL_PASSWORD -e V2_MYSQL_HOST -e V2_RABBIT_PASSWORD \
  -e V2_RABBIT_HOST -e V2_RABBIT_PORT -e V2_REDIS_HOST -e V2_REDIS_PORT \
  -v "$project_root:/workspace" -v "$cache_dir:/m2" -w /workspace "$build_image" \
  mvn -B -ntp -Dmaven.repo.local=/m2 -pl bm-app -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false "-Dtest=$test_selector" clean package
docker build -t bromark-v2:java25-it "$project_root/bm-app"
docker run --rm --entrypoint java bromark-v2:java25-it -version
