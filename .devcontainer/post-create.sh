#!/usr/bin/env bash
set -euo pipefail

bash "${PWD}/.devcontainer/load-env.sh"

if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  sbt update
else
  printf 'Skipping sbt update because GITHUB_TOKEN is not set.\n'
fi
