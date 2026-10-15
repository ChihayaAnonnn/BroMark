#!/usr/bin/env sh
# Phase4: only isolated test databases; additive 004 migration, no deployment.
set -eu
export V2_MIGRATION_THROUGH=004
exec "$(dirname -- "$0")/verify-assets.sh"
