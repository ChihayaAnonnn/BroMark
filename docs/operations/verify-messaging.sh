#!/usr/bin/env sh
# Phase3验收沿用隔离资产环境；显式应用003，不部署应用。
set -eu
export V2_MIGRATION_THROUGH=003
exec "$(dirname -- "$0")/verify-assets.sh"
