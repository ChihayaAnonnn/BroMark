#!/usr/bin/env sh
# 只连接本地隔离容器；不读取dev/prod配置。
set -eu
: "${V2_MYSQL_PASSWORD:?set isolated MySQL password}"
: "${V2_RABBIT_PASSWORD:?set isolated RabbitMQ password}"
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
V2_MIGRATION_CONTAINER=${V2_MYSQL_CONTAINER:-$(docker compose -f "$project_root/docs/dev-ops/docker-compose-v2-test.yml" ps -q mysql)}
if [ -z "$V2_MIGRATION_CONTAINER" ]; then V2_MIGRATION_CONTAINER=bromark-v2-mysql; fi
export V2_MIGRATION_CONTAINER
test_bin=$(mktemp -d /tmp/bromark-assets-test.XXXXXX)
trap 'rm -rf "$test_bin"' EXIT HUP INT TERM
cat > "$test_bin/mysql" <<'PY'
#!/usr/bin/env python3
import os
import subprocess
import sys
args = sys.argv[1:]
cleaned = []
i = 0
while i < len(args):
    if args[i] in ['-h', '-P']:
        i += 2
    else:
        cleaned.append(args[i])
        i += 1
sys.exit(subprocess.run(['docker', 'exec', '-i', '-e', 'MYSQL_PWD',
                         os.environ['V2_MIGRATION_CONTAINER'], 'mysql'] + cleaned, env=os.environ).returncode)
PY
chmod +x "$test_bin/mysql"
PATH="$test_bin:$PATH" MYSQL_PWD="$V2_MYSQL_PASSWORD" \
  python3 "$project_root/docs/dev-ops/mysql/tests/test_migrations.py"
if [ -n "${V2_MIGRATION_THROUGH:-}" ]; then
  PATH="$test_bin:$PATH" MYSQL_PWD="$V2_MYSQL_PASSWORD" V2_MIGRATION_ALLOW=isolated-router-test-only \
    python3 "$project_root/docs/dev-ops/mysql/apply_migrations.py" --env v2-it --through "$V2_MIGRATION_THROUGH"
fi
docker network inspect bromark-assets-cluster-it >/dev/null 2>&1 || docker network create bromark-assets-cluster-it >/dev/null
docker compose -f "$project_root/docs/dev-ops/docker-compose-v2-redis-cluster.yml" up -d
for n in 1 2 3; do
  attempt=0
  until docker exec "bromark-assets-redis-$n" redis-cli ping >/dev/null 2>&1; do
    attempt=$((attempt+1)); [ "$attempt" -lt 50 ] || exit 1
    sleep 0.2
  done
done
if docker exec bromark-assets-redis-1 redis-cli cluster info | grep -q 'cluster_slots_assigned:0'; then
  docker exec bromark-assets-redis-1 redis-cli --cluster create redis-1:6379 redis-2:6379 redis-3:6379 --cluster-replicas 0 --cluster-yes
fi
# Retained nodes.conf can contain noaddr peers after containers restart on new IPs.
# Reintroduce only the three containers owned by this isolated test stack; never reset slots/data.
for n in 1 2 3; do
  peer_ip=$(docker inspect "bromark-assets-redis-$n" --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
  for source in 1 2 3; do
    docker exec "bromark-assets-redis-$source" redis-cli cluster meet "$peer_ip" 6379 >/dev/null
  done
done
attempt=0
until docker exec bromark-assets-redis-1 redis-cli cluster info | grep -q 'cluster_state:ok' &&
      ! docker exec bromark-assets-redis-1 redis-cli cluster nodes | grep -Eq 'noaddr|fail|disconnected'; do
  attempt=$((attempt+1)); [ "$attempt" -lt 50 ] || exit 1
  sleep 0.2
done
export V2_TEST_DOCKER_NETWORK=bromark-assets-cluster-it
export V2_REDIS_CLUSTER_NODES=redis://redis-1:6379,redis://redis-2:6379,redis://redis-3:6379
export V2_TEST_SELECTOR='Router*Test,*V2Test,RedisClusterAdmissionIT,Java25RouterIntegrationTest,V2FoundationWiringTest,AliasMethodAlgorithmTest'
"$project_root/docs/operations/verify-java25.sh"
