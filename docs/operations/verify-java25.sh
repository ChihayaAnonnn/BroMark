#!/usr/bin/env sh
set -eu
: "${V2_MYSQL_PASSWORD:?set isolated MySQL password}"
: "${V2_RABBIT_PASSWORD:?set isolated RabbitMQ password}"
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
router_root=${ROUTER_ROOT:-"$project_root/../db-router-spring-boot-starter"}
cache_dir=${MAVEN_CACHE_DIR:-/tmp/bromark-java25-m2}
build_image=maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976
export ROUTER_MYSQL_PASSWORD="$V2_MYSQL_PASSWORD"
export ROUTER_MYSQL_HOST=${ROUTER_MYSQL_HOST:-${V2_MYSQL_HOST:-host.docker.internal:13316}}
mkdir -p "$cache_dir"
docker run --rm -e ROUTER_MYSQL_PASSWORD -e ROUTER_MYSQL_HOST \
  -v "$router_root:/router" -v "$cache_dir:/m2" -w /router "$build_image" \
  mvn -B -ntp -Dmaven.repo.local=/m2 install
docker run --rm -e V2_MYSQL_PASSWORD -e V2_MYSQL_HOST -e V2_RABBIT_PASSWORD \
  -e V2_RABBIT_HOST -e V2_RABBIT_PORT -e V2_REDIS_HOST -e V2_REDIS_PORT \
  -v "$project_root:/workspace" -v "$cache_dir:/m2" -w /workspace "$build_image" \
  mvn -B -ntp -Dmaven.repo.local=/m2 -pl bm-app -am -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false '-Dtest=*V2Test,Java25RouterIntegrationTest,AliasMethodAlgorithmTest' package
docker build -t bromark-v2:java25-it "$project_root/bm-app"
docker run --rm --entrypoint java bromark-v2:java25-it -version
