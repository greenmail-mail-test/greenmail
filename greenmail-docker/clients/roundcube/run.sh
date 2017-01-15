#!/bin/bash

cmd_php="php -S 0.0.0.0:80 -c php.ini -t /www"

genpasswd() {
  export LC_CTYPE=C
  local l=$1
  [ "$l" == "" ] && l=16
  cat /dev/urandom | tr -dc A-Za-z0-9_ | head -c ${l}
}

wait_for_php() {
  until curl --output /dev/null --silent --get --fail "http://localhost"; do
    echo "waiting for php to start..."
    sleep 2
  done
}

init_config() {
  :>/www/logs/errors
  export DES_KEY=$(genpasswd 24)
  cat config.php>/www/config/config.inc.php
  echo "<?php">/config/__config.php
  for e in $(env); do
    case $e in
      RC_*)
        e1=$(expr "$e" : 'RC_\([A-Z_]*\)')
        e2=$(expr "$e" : '\([A-Z_]*\)')
        echo "\$config['${e1,,}'] = getenv('$e2');">>/config/__config.php
    esac
  done
}

init_db() {
  $cmd_php &
  wait_for_php
  pid_php=$!
  echo "<?php \$config['enable_installer'] = true;">/config/___setup.php
  curl --silent --output /dev/null --data "initdb=Initialize+database" http://localhost/installer/index.php?_step=3
  kill $pid_php
  wait $pid_php 2>/dev/null
  rm -rf /www/installer /config/___setup.php
}

init_config

if [ ! -f .initialized ]; then
  init_db
  touch .initialized
fi

$cmd_php &
tail -f /www/logs/errors
