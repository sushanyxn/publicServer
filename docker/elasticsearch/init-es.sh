#!/bin/sh
# SLG 告警日志分析系统 - ES 初始化脚本
# 创建 ILM 策略、索引模板，在集群首次启动时自动执行

ES_URL="http://es01:9200"

echo "===> 等待 ES 集群就绪..."
until curl -s "$ES_URL/_cluster/health" | grep -q '"status":"green"\|"status":"yellow"'; do
  sleep 2
done
echo "===> ES 集群已就绪"

# 1. 创建 ILM 生命周期策略
echo "===> 创建 ILM 策略: slg-logs-policy"
curl -s -X PUT "$ES_URL/_ilm/policy/slg-logs-policy" \
  -H 'Content-Type: application/json' \
  -d '{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_age": "7d",
            "max_primary_shard_size": "10gb"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}'
echo ""

# 2. 创建索引模板
echo "===> 创建索引模板: slg-logs-template"
curl -s -X PUT "$ES_URL/_index_template/slg-logs-template" \
  -H 'Content-Type: application/json' \
  -d '{
  "index_patterns": ["slg-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 2,
      "number_of_replicas": 1,
      "index.lifecycle.name": "slg-logs-policy",
      "index.refresh_interval": "5s"
    },
    "mappings": {
      "properties": {
        "@timestamp":    { "type": "date" },
        "message":       { "type": "text", "analyzer": "standard" },
        "level":         { "type": "keyword" },
        "logger_name":   { "type": "keyword" },
        "thread_name":   { "type": "keyword" },
        "server_id":     { "type": "keyword" },
        "server_type":   { "type": "keyword" },
        "stack_trace":   { "type": "text" }
      }
    }
  },
  "priority": 100
}'
echo ""

# 3. 验证
echo "===> 验证集群状态..."
curl -s "$ES_URL/_cluster/health?pretty"
echo ""
echo "===> 验证索引模板..."
curl -s "$ES_URL/_index_template/slg-logs-template?pretty" | head -5
echo ""
echo "===> 初始化完成!"
