#!/bin/bash

# 服务器地址
BASE_URL="http://localhost:8080"

echo "--- 1. 正在登录并获取 Token ---"
# 使用 -s 来静默输出，只保留最终结果
TOKEN=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}' \
  "$BASE_URL/api/auth/login" | jq -r '.token')

if [ -z "$TOKEN" ]; then
    echo "获取 Token 失败！请检查后端服务是否正常。"
    exit 1
fi

echo "Token 获取成功！"
echo ""

echo "--- 2. 测试获取所有订单 ---"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/orders" | jq
echo ""

echo "--- 3. 测试获取 ID=1 的订单 ---"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/orders/1" | jq
echo ""

echo "--- 4. 测试支付 ID=5 的订单 ---"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/orders/5/pay" | jq
echo "支付请求已发送。请稍后手动查询 ID=5 的订单状态。"
echo ""

echo "--- 5. 测试重试 ID=3 的订单出票 ---"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/orders/3/retry-ticketing"
echo "重试请求已发送。请稍后手动查询 ID=3 的订单状态。"
echo ""

echo "--- 6. 测试取消 ID=4 的订单 ---"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/orders/4/cancel" | jq
echo ""

echo "--- 测试完成 ---"