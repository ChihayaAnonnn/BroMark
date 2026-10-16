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
# Passing tests alone cannot represent complete six-domain validation.
if [ -n "${V2_RECONCILIATION_SEED:-}" ] && [ "${V2_RECONCILIATION_STOPPED:-}" = 1 ]; then
  if ! python3 "$project_root/docs/performance/reconcile_v2_run.py" --seed "$V2_RECONCILIATION_SEED" --out "$run_dir/reconciliation.json" --stopped > "$run_dir/reconciliation.log" 2>&1; then
    if [ ! -f "$run_dir/reconciliation.json" ]; then
      echo '{"passed":false,"reason":"reconciliation dependency/input failure"}' > "$run_dir/reconciliation.json"
    fi
  fi
else
  echo '{"passed":false,"reason":"V2_RECONCILIATION_SEED and stopped-writer assertion required"}' > "$run_dir/reconciliation.json"
fi
python3 "$project_root/docs/dev-ops/verify_v2_reports.py" --tests-json "$run_dir/tests.json" --financial-json "$run_dir/reconciliation.json" --validation-out "$run_dir/validation.json"
printf 'Evidence: %s\n' "$run_dir"
