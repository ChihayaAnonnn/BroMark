#!/usr/bin/env sh
# 只连接本地隔离容器；不读取dev/prod配置。
set -eu
: "${V2_MYSQL_PASSWORD:?set isolated MySQL password}"
: "${V2_RABBIT_PASSWORD:?set isolated RabbitMQ password}"
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
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
                         'bromark-v2-mysql', 'mysql'] + cleaned, env=os.environ).returncode)
PY
chmod +x "$test_bin/mysql"
PATH="$test_bin:$PATH" MYSQL_PWD="$V2_MYSQL_PASSWORD" \
  python3 "$project_root/docs/dev-ops/mysql/tests/test_migrations.py"
"$project_root/docs/operations/verify-java25.sh"
