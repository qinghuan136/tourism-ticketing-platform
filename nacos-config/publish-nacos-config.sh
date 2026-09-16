#!/bin/sh
set -eu

for file in /config/*.yml; do
  data_id=$(basename "$file")
  curl -fsS -X POST http://nacos:8848/nacos/v3/admin/cs/config \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "groupName=$NACOS_CONFIG_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@$file" >/dev/null
  echo "published=$NACOS_CONFIG_GROUP/$data_id"
done
