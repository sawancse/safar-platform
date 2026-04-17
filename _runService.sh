#!/bin/bash
#
# Helper invoked by startAllServicesTabs.sh inside each Windows Terminal tab.
# Usage: _runService.sh <service-name>
#
# Tab is intentionally hard to close: Ctrl+C and Ctrl+D will NOT terminate it.
# Close the tab via the Windows Terminal X button or `exit` typed at the prompt.
#

# Ignore SIGINT (Ctrl+C) and SIGQUIT in this wrapper so the tab survives.
trap '' INT QUIT

SERVICE="$1"
SVC_DIR="/c/Users/Win-10/safar-platform/services/$SERVICE"

run_service() {
  if [ -z "$SERVICE" ]; then
    echo "Usage: $0 <service-name>"
    return
  fi
  if [ ! -d "$SVC_DIR" ]; then
    echo "ERROR: $SVC_DIR not found"
    return
  fi
  cd "$SVC_DIR" || { echo "cd failed"; return; }
  echo "==> Starting $SERVICE in $SVC_DIR"
  mvn clean spring-boot:run "-Dspring-boot.run.jvmArguments=-XX:+UseG1GC -XX:ParallelGCThreads=2 -Xms512m -Xmx512m"
  echo
  echo "==> $SERVICE exited (code $?)."
}

run_service

# Keep tab alive forever. Ctrl+D (EOF) just restarts the inner shell.
# Type 'restart' to re-run mvn, 'exit' (then confirm) to actually close.
while true; do
  echo
  echo "==> Tab kept alive. Commands: 'restart' to relaunch $SERVICE, 'exit' to close tab."
  # `bash -i` gives an interactive shell; `|| true` swallows non-zero so loop continues.
  bash -i || true
  echo "==> Inner shell ended. Looping (Ctrl+C / Ctrl+D will NOT close this tab)."
done
