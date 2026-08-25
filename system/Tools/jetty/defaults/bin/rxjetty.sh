#!/usr/bin/env bash
# chkconfig: 2345 99 01
# description: Percussion CMS (Jetty) server - Rhythmyx
# LSB Tags
### BEGIN INIT INFO
# Provides:          ${rxjetty_service}
# Required-Start:    $local_fs $network $remote_fs
# Should-Start:      $named mysqld mysql mariadb
# Required-Stop:     $local_fs $network $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Jetty start script.
# Description:       Start Jetty web server.
### END INIT INFO

# Startup script for jetty under *nix systems (it works under NT/cygwin too).

##################################################
# Set the name which is used by other variables.
# Defaults to the file name without extension.
##################################################
NAME=$(echo $(basename $0) | sed -e 's/^[SK][0-9]*//' -e 's/\.sh$//')

# To get the service to restart correctly on reboot, uncomment below (3 lines):
# ========================
#chkconfig: 3 99 99
# description: Rhythmyx Jetty 9 webserver
# processname: ${rxjetty_service}
# ========================

# Configuration files
#
# /etc/default/$NAME
#   If it exists, this is read at the start of script. It may perform any 
#   sequence of shell commands, like setting relevant environment variables.
#
# $HOME/.$NAMErc (e.g. $HOME/.jettyrc)
#   If it exists, this is read at the start of script. It may perform any 
#   sequence of shell commands, like setting relevant environment variables.
#
# /etc/$NAME.conf
#   If found, and no configurations were given on the command line,
#   the file will be used as this script's configuration. 
#   Each line in the file may contain:
#     - A comment denoted by the pound (#) sign as first non-blank character.
#     - The path to a regular file, which will be passed to jetty as a 
#       config.xml file.
#     - The path to a directory. Each *.xml file in the directory will be
#       passed to jetty as a config.xml file.
#     - All other lines will be passed, as-is to the start.jar
#
#   The files will be checked for existence before being passed to jetty.
#
# Configuration variables
#
# JAVA
#   Command to invoke Java. If not set, java (from the PATH) will be used.
#
# JAVA_OPTIONS
#   Extra options to pass to the JVM
#
# JETTY_HOME
#   Where Jetty is installed. If not set, the script will try go
#   guess it by looking at the invocation path for the script
#   The java system property "jetty.home" will be
#   set to this value for use by configure.xml files, f.e.:
#
#    <Arg><Property name="jetty.home" default="."/>/webapps/jetty.war</Arg>
#
# JETTY_BASE
#   Where your Jetty base directory is.  If not set, the value from
#   $JETTY_HOME will be used.
#
# JETTY_RUN
#   Where the $NAME.pid file should be stored. It defaults to the
#   first available of /var/run, /usr/var/run, JETTY_BASE and /tmp
#   if not set.
#  
# JETTY_PID
#   The Jetty PID file, defaults to $JETTY_RUN/$NAME.pid
#   
# JETTY_ARGS
#   The default arguments to pass to jetty.
#   For example
#      JETTY_ARGS=jetty.http.port=8080 jetty.ssl.port=8443
#
# JETTY_USER
#   if set, then used as a username to run the server as
#
# JETTY_SHELL
#   If set, then used as the shell by su when starting the server.  Will have
#   no effect if start-stop-daemon exists.  Useful when JETTY_USER does not
#   have shell access, e.g. /bin/false
#
# WAIT_FOR_DB_HOST
#   Optional. Hostname or IP of the database the CMS depends on. If set
#   (with WAIT_FOR_DB_PORT), the start path will retry a TCP connection to
#   host:port until the timeout elapses before invoking Jetty. This is the
#   recommended way to make the init script survive a reboot when the DB is
#   remote (e.g. AWS RDS) and there is no local mysqld init script to order
#   against. Empty/unset disables the wait.
#
# WAIT_FOR_DB_PORT
#   Optional. TCP port for WAIT_FOR_DB_HOST. Default: 3306.
#
# WAIT_FOR_DB_TIMEOUT
#   Optional. Seconds to keep retrying the DB TCP probe before giving up.
#   Default: 120. A value of 0 disables the wait (same as unset).
#
# WAIT_FOR_DB_INTERVAL
#   Optional. Seconds between DB probe attempts. Default: 2.
#

usage()
{
    echo "Usage: ${0##*/} [-d] {start|stop|run|restart|check|supervise} [ CONFIGS ... ] "
    exit 1
}

[ $# -gt 0 ] || usage


##################################################
# Some utility functions
##################################################
findDirectory()
{
  local L OP=$1
  shift
  for L in "$@"; do
    [ "$OP" "$L" ] || continue 
    printf %s "$L"
    break
  done 
}

running()
{
  if [ -f "$1" ]
  then
    local PID=$(cat "$1" 2>/dev/null) || return 1
    kill -0 "$PID" 2>/dev/null
    return
  fi
  rm -f "$1"
  return 1
}

started()
{
     rm -f $JETTY_RUN/${NAME}.pid
     rm -f $JETTY_BASE/${NAME}.state

  # wait for 500*8s to see "STARTED" in PID file, needs jetty-started.xml as argument
  for T in {1..500}
  do
    sleep 8 
    [ -z "$(grep STARTED $1 2>/dev/null)" ] || return 0
    [ -z "$(grep STOPPED $1 2>/dev/null)" ] || return 1
    [ -z "$(grep FAILED $1 2>/dev/null)" ] || return 1
    local PID=$(cat "$2" 2>/dev/null) || return 1
    kill -0 "$PID" 2>/dev/null || return 1
    echo -n ". "
  done

  return 1;
}


readConfig()
{
  (( DEBUG )) && echo "Reading $1.."
  source "$1"
}

dumpEnv()
{
    echo "JAVA           =  $JAVA"
    echo "JAVA_OPTIONS   =  ${JAVA_OPTIONS[*]}"
    echo "JETTY_HOME     =  $JETTY_HOME"
    echo "JETTY_BASE     =  $JETTY_BASE"
    echo "START_D        =  $START_D"
    echo "START_INI      =  $START_INI"
    echo "JETTY_START    =  $JETTY_START"
    echo "JETTY_CONF     =  $JETTY_CONF"
    echo "JETTY_ARGS     =  ${JETTY_ARGS[*]}"
    echo "JETTY_RUN      =  $JETTY_RUN"
    echo "JETTY_PID      =  $JETTY_PID"
    echo "JETTY_START_LOG=  $JETTY_START_LOG"
    echo "JETTY_STATE    =  $JETTY_STATE"
    echo "RUN_CMD        =  ${RUN_CMD[*]}"
}

##################################################
# Find the actual Java/Jetty PID for this install.
#
# Strategy:
#   1. Trust JETTY_PID file if it points to a live process whose cmdline
#      matches this JETTY_BASE.
#   2. Otherwise, scan /proc/*/cmdline for a java process whose -Djetty.base
#      matches JETTY_BASE (handles stale/missing PID file).
#
# Echoes the PID on stdout, nothing if no match.
##################################################
find_jetty_pid()
{
  local PID_FILE="$1"
  local BASE="$2"
  local pid matched_cmdline

  # 1. Trust the PID file first
  if [ -f "$PID_FILE" ]; then
    pid=$(cat "$PID_FILE" 2>/dev/null)
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      if [ -r "/proc/$pid/cmdline" ]; then
        matched_cmdline=$(tr '\0' ' ' < "/proc/$pid/cmdline" 2>/dev/null)
        if [[ "$matched_cmdline" == *"java"* && "$matched_cmdline" == *"-Djetty.base=${BASE}"* ]]; then
          echo "$pid"
          return 0
        fi
      fi
    fi
    # PID file is stale or points to the wrong process; drop it so stop
    # does not falsely report success.
    rm -f "$PID_FILE"
  fi

  # 2. Scan /proc for a matching java process
  if [ -d /proc ]; then
    for pid in $(pgrep -f "java.*-Djetty.base=${BASE}" 2>/dev/null); do
      if [ -r "/proc/$pid/cmdline" ] && kill -0 "$pid" 2>/dev/null; then
        echo "$pid"
        return 0
      fi
    done
  fi

  return 1
}

##################################################
# Wait for the database to accept TCP connections.
#
# Used during start when the DB is remote (no local mysqld init script
# to order against). Probes host:port with `nc -z` (fallback: bash
# /dev/tcp) every WAIT_FOR_DB_INTERVAL seconds until WAIT_FOR_DB_TIMEOUT
# elapses. No-op if WAIT_FOR_DB_HOST is unset.
##################################################
wait_for_db()
{
  local host="${WAIT_FOR_DB_HOST:-}"
  local port="${WAIT_FOR_DB_PORT:-3306}"
  local timeout="${WAIT_FOR_DB_TIMEOUT:-120}"
  local interval="${WAIT_FOR_DB_INTERVAL:-2}"
  local elapsed=0
  local probe_cmd

  if [ -z "$host" ] || [ "$timeout" -le 0 ]; then
    return 0
  fi

  if type -P nc >/dev/null 2>&1; then
    probe_cmd() { nc -z -w 2 "$host" "$port" >/dev/null 2>&1; }
  else
    # bash builtin /dev/tcp fallback (no -w support; rely on connection refused)
    probe_cmd() { (echo > "/dev/tcp/${host}/${port}") >/dev/null 2>&1; }
  fi

  echo -n "Waiting for database ${host}:${port} "
  while [ "$elapsed" -lt "$timeout" ]; do
    if probe_cmd; then
      echo "OK"
      return 0
    fi
    sleep "$interval"
    elapsed=$((elapsed + interval))
    echo -n ". "
  done
  echo "TIMEOUT"
  echo "ERROR: database ${host}:${port} did not become reachable within ${timeout}s" >&2
  return 1
}

##################################################
# Stop a PID with TERM, then KILL after a timeout.
# Echoes progress to stdout. Returns 0 if the process is gone, 1 otherwise.
##################################################
stop_pid()
{
  local pid="$1"
  local timeout="${STOP_TIMEOUT:-60}"
  local elapsed=0

  if [ -z "$pid" ] || ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi

  kill -TERM "$pid" 2>/dev/null || true
  while kill -0 "$pid" 2>/dev/null; do
    if [ "$elapsed" -ge "$timeout" ]; then
      kill -KILL "$pid" 2>/dev/null || true
      # give the kernel a moment to reap
      sleep 1
      if kill -0 "$pid" 2>/dev/null; then
        return 1
      fi
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done
  return 0
}



##################################################
# Get the action & configs
##################################################
CONFIGS=()
NO_START=0
DEBUG=0

while [[ $1 = -* ]]; do
  case $1 in
    -d) DEBUG=1 ;;
  esac
  shift
done
ACTION=$1
shift

##################################################
# Read any configuration files
##################################################
ETC=/etc
if [ $UID != 0 ]
then 
  ETC=$HOME/etc
fi

for CONFIG in {/etc,~/etc}/default/${NAME}{,9} $HOME/.${NAME}rc; do
  if [ -f "$CONFIG" ] ; then 
    readConfig "$CONFIG"
  fi
done


##################################################
# Set tmp if not already set.
##################################################
TMPDIR=${TMPDIR:-/tmp}

##################################################
# Jetty's hallmark
##################################################
JETTY_INSTALL_TRACE_FILE="start.jar"


##################################################
# Try to determine JETTY_HOME if not set
##################################################
if [ -z "$JETTY_HOME" ] 
then
  JETTY_SH=$0
  case "$JETTY_SH" in
    /*)     JETTY_HOME=${JETTY_SH%/*/*} ;;
    ./*/*)  JETTY_HOME=${JETTY_SH%/*/*} ;;
    ./*)    JETTY_HOME=.. ;;
    */*/*)  JETTY_HOME=./${JETTY_SH%/*/*} ;;
    */*)    JETTY_HOME=. ;;
    *)      JETTY_HOME=.. ;;
  esac

  if [ ! -f "$JETTY_HOME/$JETTY_INSTALL_TRACE_FILE" ]
  then 
    JETTY_HOME=
  fi
fi


##################################################
# No JETTY_HOME yet? We're out of luck!
##################################################
if [ -z "$JETTY_HOME" ]; then
  echo "** ERROR: JETTY_HOME not set, you need to set it or install in a standard location" 
  exit 1
fi

cd "$JETTY_HOME"
JETTY_HOME=$PWD


##################################################
# Set JETTY_BASE 
##################################################
if [ -z "$JETTY_BASE" ]; then
  JETTY_BASE=$JETTY_HOME
fi

cd "$JETTY_BASE"
JETTY_BASE=$PWD


#####################################################
# Check that jetty is where we think it is
#####################################################
if [ ! -r "$JETTY_HOME/$JETTY_INSTALL_TRACE_FILE" ] 
then
  echo "** ERROR: Oops! Jetty doesn't appear to be installed in $JETTY_HOME"
  echo "** ERROR:  $JETTY_HOME/$JETTY_INSTALL_TRACE_FILE is not readable!"
  exit 1
fi

##################################################
# Try to find this script's configuration file,
# but only if no configurations were given on the
# command line.
##################################################
if [ -z "$JETTY_CONF" ] 
then
  if [ -f $ETC/${NAME}.conf ]
  then
    JETTY_CONF=$ETC/${NAME}.conf
  elif [ -f "$JETTY_BASE/etc/jetty.conf" ]
  then
    JETTY_CONF=$JETTY_BASE/etc/jetty.conf
  elif [ -f "$JETTY_HOME/etc/jetty.conf" ]
  then
    JETTY_CONF=$JETTY_HOME/etc/jetty.conf
  fi
fi

#####################################################
# Find a location for the pid file
#####################################################
if [ -z "$JETTY_RUN" ] 
then
  JETTY_RUN=$(findDirectory -w /var/run /usr/var/run $JETTY_BASE /tmp)
fi

#####################################################
# Find a pid and state file
#####################################################
if [ -z "$JETTY_PID" ] 
then
  JETTY_PID="$JETTY_RUN/${NAME}.pid"
fi

if [ -z "$JETTY_STATE" ] 
then
  JETTY_STATE=$JETTY_BASE/${NAME}.state
fi

case "`uname`" in
CYGWIN*) JETTY_STATE="`cygpath -w $JETTY_STATE`";;
esac


JETTY_ARGS=(${JETTY_ARGS[*]} "jetty.state=$JETTY_STATE")

##################################################
# Get the list of config.xml files from jetty.conf
##################################################
if [ -f "$JETTY_CONF" ] && [ -r "$JETTY_CONF" ] 
then
  while read -r CONF
  do
    if expr "$CONF" : '#' >/dev/null ; then
      continue
    fi

    if [ -d "$CONF" ] 
    then
      # assume it's a directory with configure.xml files
      # for example: /etc/jetty.d/
      # sort the files before adding them to the list of JETTY_ARGS
      for XMLFILE in "$CONF/"*.xml
      do
        if [ -r "$XMLFILE" ] && [ -f "$XMLFILE" ] 
        then
          JETTY_ARGS=(${JETTY_ARGS[*]} "$XMLFILE")
        else
          echo "** WARNING: Cannot read '$XMLFILE' specified in '$JETTY_CONF'" 
        fi
      done
    else
      # assume it's a command line parameter (let start.jar deal with its validity)
      JETTY_ARGS=(${JETTY_ARGS[*]} "$CONF")
    fi
  done < "$JETTY_CONF"
fi

##################################################
# Setup JAVA if unset
##################################################
if [ -z "$JAVA" ]
then
  JAVA=$(which java)
fi

if [ -z "$JAVA" ]
then
  echo "Cannot find a Java JDK. Please set either set JAVA or put java (>=1.5) in your PATH." >&2
  exit 1
fi

#####################################################
# See if JETTY_LOGS is defined
#####################################################
if [ -z "$JETTY_LOGS" ] && [ -d $JETTY_BASE/logs ] 
then
  JETTY_LOGS=$JETTY_BASE/logs
fi
if [ -z "$JETTY_LOGS" ] && [ -d $JETTY_HOME/logs ] 
then
  JETTY_LOGS=$JETTY_HOME/logs
fi
if [ "$JETTY_LOGS" ]
then

  case "`uname`" in
  CYGWIN*) JETTY_LOGS="`cygpath -w $JETTY_LOGS`";;
  esac

  JAVA_OPTIONS=(${JAVA_OPTIONS[*]} "-Djetty.logging.dir=$JETTY_LOGS")
fi

#####################################################
# Are we running on Windows? Could be, with Cygwin/NT.
#####################################################
case "`uname`" in
CYGWIN*) PATH_SEPARATOR=";";;
*) PATH_SEPARATOR=":";;
esac


#####################################################
# Add jetty properties to Java VM options.
#####################################################

case "`uname`" in
CYGWIN*) 
JETTY_HOME="`cygpath -w $JETTY_HOME`"
JETTY_BASE="`cygpath -w $JETTY_BASE`"
TMPDIR="`cygpath -w $TMPDIR`"
;;
esac

JAVA_OPTIONS=(${JAVA_OPTIONS[*]} "-XX:+DisableAttachMechanism" "-Djetty.home=$JETTY_HOME" "-Djetty.base=$JETTY_BASE" "-Djava.io.tmpdir=$TMPDIR")

#####################################################
# This is how the Jetty server will be started
#####################################################

JETTY_START=$JETTY_HOME/start.jar
START_INI=$JETTY_BASE/start.ini
START_D=$JETTY_BASE/start.d
if [ ! -f "$START_INI" -a ! -d "$START_D" ]
then
  echo "Cannot find a start.ini file or a start.d directory in your JETTY_BASE directory: $JETTY_BASE" >&2
  exit 1
fi

case "`uname`" in
CYGWIN*) JETTY_START="`cygpath -w $JETTY_START`";;
esac

RUN_ARGS=(${JAVA_OPTIONS[@]} -jar "$JETTY_START" ${JETTY_ARGS[*]})
RUN_CMD=("$JAVA" ${RUN_ARGS[@]})

#####################################################
# Comment these out after you're happy with what 
# the script is doing.
#####################################################
if (( DEBUG ))
then
  dumpEnv
fi

##################################################
# Do the action
##################################################
case "$ACTION" in
  start)
    echo -n "Starting Jetty: "

    if (( NO_START )); then
      echo "Not starting ${NAME} - NO_START=1";
      exit
    fi

    # Optional: block until a (remote) DB is reachable so reboot works
    # without a local mysqld init script to order against. No-op when
    # WAIT_FOR_DB_HOST is unset; failure is non-fatal so installs with a
    # healthy local DB still come up.
    if ! wait_for_db; then
      echo "WARNING: continuing Jetty start despite database not being reachable; CMS may fail until it is."
    fi

	dtest=$(start-stop-daemon --test --oknodo -S -p"$JETTY_PID"  -d"$JETTY_BASE" -b -m -a "$JAVA" -- "${RUN_ARGS[@]}" start-log-file="$JETTY_LOGS/start.log" 2>&1 )


if [ $UID -eq 0 ] && [[ "$dtest" !=  *"not found"* &&  "$dtest" !=  *"invalid argument"* ]]; then

      unset CH_USER
      if [ -n "$JETTY_USER" ]
      then
        CH_USER="-c$JETTY_USER"
      fi

      start-stop-daemon -S -p"$JETTY_PID" $CH_USER -d"$JETTY_BASE" -b -m -a "$JAVA" -- "${RUN_ARGS[@]}" start-log-file="$JETTY_START_LOG"

    else

      if running $JETTY_PID
      then
        echo "Already Running $(cat $JETTY_PID)!"
        exit 1
      fi

      if [ -n "$JETTY_USER" ] && [ `whoami` != "$JETTY_USER" ]
      then
        unset SU_SHELL
        if [ "$JETTY_SHELL" ]
        then
          SU_SHELL="-s $JETTY_SHELL"
        fi

        chown -R "$JETTY_USER:$JETTY_USER" "$JETTY_RUN"
        touch "$JETTY_PID"

        chown "$JETTY_USER:$JETTY_USER" "$JETTY_PID"
        # FIXME: Broken solution: wordsplitting, pathname expansion, arbitrary command execution, etc.
        su - "$JETTY_USER" $SU_SHELL -c "
          cd ${JETTY_BASE}
          0<&- &>/dev/null
          nohup ${RUN_CMD[*]} start-log-file="$JETTY_START_LOG" 0<&- &>/dev/null &
          echo \$! > '$JETTY_PID'"
      else
        "${RUN_CMD[@]}" > /dev/null &
        disown $!
        echo $! > "$JETTY_PID"
      fi

    fi

    if expr "${JETTY_ARGS[*]}" : '.*jetty-started.xml.*' >/dev/null
    then
      if started "$JETTY_STATE" "$JETTY_PID"
      then
        echo "OK `date`"
      else
        echo "FAILED `date`"
        exit 1
      fi
    else
      echo "ok `date`"
    fi

    ;;

  stop)
    echo -n "Stopping Jetty: "

    # Locate the actual Java/Jetty PID for this install. This tolerates
    # a missing, stale, or wrong PID file by falling back to /proc scan.
    JETTY_PID_FILE="$JETTY_PID"
    if [ -z "$JETTY_PID_FILE" ]; then
      JETTY_PID_FILE="$JETTY_RUN/${NAME}.pid"
    fi
    PID=$(find_jetty_pid "$JETTY_PID_FILE" "$JETTY_BASE" 2>/dev/null)

    # Only delegate to start-stop-daemon when (a) we are root, (b) the
    # binary exists and accepts the arguments, and (c) we have a real
    # PID for this install. The previous version checked `$UID -eq 2`
    # which is the `daemon` account, not root, so the start-stop-daemon
    # path was effectively dead code.
    USE_SSD="false"
    if [ $UID -eq 0 ] && [ -n "$PID" ] && type -P start-stop-daemon >/dev/null 2>&1; then
      dtest=$(start-stop-daemon --test --oknodo -K -p"$JETTY_PID_FILE" -d"$JETTY_BASE" -a "$JAVA" 2>&1)
      if [[ "$dtest" != *"not found"* && "$dtest" != *"invalid argument"* ]]; then
        USE_SSD="true"
      fi
    fi

    if [ "$USE_SSD" = "true" ]; then
      # Default signal for start-stop-daemon -K is SIGTERM, which Jetty
      # handles for a graceful shutdown. SIGHUP was the wrong signal here.
      start-stop-daemon -K -p"$JETTY_PID_FILE" -d"$JETTY_BASE" -a "$JAVA"

      TIMEOUT=60
      while running "$JETTY_PID_FILE"; do
        if (( TIMEOUT-- == 0 )); then
          start-stop-daemon -K -p"$JETTY_PID_FILE" -d"$JETTY_BASE" -a "$JAVA" -s KILL
        fi

        sleep 1
      done
    else
      if [ -z "$PID" ]; then
        # No live Jetty process for this install. Nothing to stop; clear
        # any leftover state files and exit 0 so `service ... stop` is
        # idempotent and never leaves init in a confused state.
        rm -f "$JETTY_PID_FILE" "$JETTY_RUN/${NAME}.pid" "$JETTY_BASE/${NAME}.state"
        echo "OK (not running)"
        exit 0
      fi

      stop_pid "$PID"
      STOP_RC=$?
    fi

    # Verify the JVM is actually gone. `kill` returning 0 is not proof -
    # the PID may have been reused, or we may have signalled a parent
    # shell rather than the JVM. Re-resolve and confirm.
    if PID=$(find_jetty_pid "$JETTY_PID_FILE" "$JETTY_BASE" 2>/dev/null) && [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
      echo "FAILED (Jetty PID $PID still alive)"
      exit 1
    fi

    rm -f "$JETTY_RUN/${NAME}.pid" "$JETTY_BASE/${NAME}.state"

    echo "OK"

    ;;

  restart)
    JETTY_SH=$0
    > "$JETTY_STATE"
    if [ ! -f $JETTY_SH ]; then
      if [ ! -f $JETTY_HOME/bin/jetty.sh ]; then
        echo "$JETTY_HOME/bin/jetty.sh does not exist."
        exit 1
      fi
      JETTY_SH=$JETTY_HOME/bin/jetty.sh
    fi

    "$JETTY_SH" stop "$@"
    "$JETTY_SH" start "$@"

    ;;

  supervise)
    #
    # Under control of daemontools supervise monitor which
    # handles restarts and shutdowns via the svc program.
    #
    exec "${RUN_CMD[@]}"

    ;;

  run|demo)
    echo "Running Jetty: "

    if running "$JETTY_PID"
    then
      echo Already Running $(cat "$JETTY_PID")!
      exit 1
    fi

    exec "${RUN_CMD[@]}"
    ;;

  check|status)
    if running "$JETTY_PID"
    then
      echo "Jetty running pid=$(< "$JETTY_PID")"
    else
      echo "Jetty NOT running"
    fi
    echo
    dumpEnv
    echo
    
    if running "$JETTY_PID"
    then
      exit 0
    fi
    exit 1

    ;;

  *)
    usage

    ;;
esac

exit 0
