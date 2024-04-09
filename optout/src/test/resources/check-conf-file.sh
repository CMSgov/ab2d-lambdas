#!/bin/bash

set -e

# Echo to stderr
err() {
  >&2 echo "$@"
}

conf_file=$1
today=$(date +%Y%m%d)
expected=$(mktemp)

err "Expected file content:"
err

# Use head from gnu coreutils to remove trailing newline
head -c-1 <<EOF | >&2 tee "$expected"
HDR_BENECONFIRM${today}
1S00E00JG37${today}NAccepted  00
7SP1D00AA00${today}NAccepted  00
2SY1D00AA00${today}YAccepted  00
7SF9C00AA00${today}NAccepted  00
6SF9C00AA00${today}NAccepted  00
DUMMY000006${today}YAccepted  00
DUMMY000007${today}NAccepted  00
TRL_BENECONFIRM${today}0000000007
EOF

# Print some newlines to make logs easier to read
err
err

err "Testing confirmation file..."

# Hide output for now to avoid revealing sensitive info
if ! diff -u "$conf_file" "$expected" > /dev/null; then
  err "Confirmation file differs from expected"
  exit 1
fi

err "Confirmation file matches expected"
