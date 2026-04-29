#!/bin/sh
set -eu

mkdir -p /home/rocketmq/logs/rocketmqlogs

exec /home/rocketmq/rocketmq-5.3.4/bin/mqbroker \
  -c /home/rocketmq/rocketmq-5.3.4/conf/broker.conf
