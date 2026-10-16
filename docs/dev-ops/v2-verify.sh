#!/usr/bin/env sh
# 验证已授权的本地隔离容器，不启动真实服务。
set -eu
[ "$#" -eq 2 ] && [ "$1" = --env ] && [ "$2" = v2-it ] || { echo 'usage: v2-verify.sh --env v2-it' >&2; exit 2; }
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
run_dir=${V2_EVIDENCE_DIR:-$(mktemp -d /tmp/bromark-phase6-verify.XXXXXX)}
mkdir -p "$run_dir"
export V2_MIGRATION_THROUGH=006
# verify-assets -> verify-java25 uses clean package: old XML cannot satisfy this run.
if "$project_root/docs/operations/verify-assets.sh" > "$run_dir/maven.log" 2>&1; then
  python3 "$project_root/docs/dev-ops/verify_v2_reports.py" --root "$project_root" --out "$run_dir/tests.json"
else
  echo '{"passed":false,"reason":"build/dependency/test failure; see maven.log"}' > "$run_dir/tests.json"
  tail -50 "$run_dir/maven.log"
  exit 1
fi
python3 -m unittest discover -s "$project_root/docs/performance" -p 'test_*.py' > "$run_dir/python.log" 2>&1
python3 -m unittest discover -s "$project_root/docs/dev-ops/mysql/tests" -p 'test_cutover*.py' >> "$run_dir/python.log" 2>&1
printf 'Evidence: %s\n' "$run_dir"
