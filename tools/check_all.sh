#!/bin/sh
# Every static gate, in one command. Run before every build.
set -e
cd "$(dirname "$0")/.."
echo "== structural =="   && python3 tools/static_check.py
echo "== resolution ==" && python3 tools/deep_check.py
echo "== modules ==" && python3 tools/module_check.py
echo "== hilt ==" && python3 tools/hilt_check.py
echo "== xml ==" && python3 tools/xml_check.py
echo "== algorithms ==" && python3 tools/verify_algorithms.py
echo
echo "All gates passed. NOTE: none of these is a Kotlin compiler. They cannot check types,"
echo "generics, nullability, overload resolution, or Compose/Hilt code generation."
