#!/usr/bin/env sh
# Phase5 isolated sandbox only. Never deploy a real channel.
set -eu
export V2_MIGRATION_THROUGH=005
exec "$(dirname -- "$0")/verify-assets.sh"
