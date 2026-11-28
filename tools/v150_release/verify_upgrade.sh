#!/usr/bin/env bash
set -euo pipefail
bash tools/auto_release/verify_delta_install.sh
python3 tools/v150_release/qa_arena.py
