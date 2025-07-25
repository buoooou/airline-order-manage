mysql -h 18.207.201.92 -P 3306 -u root -p --skip-ssl

-- 查看所有数据库
SHOW DATABASES;

-- 切换到您的应用数据库
USE airline_order_db;

-- 查看当前数据库中的所有表
SHOW TABLES;

-- 查看 'orders' 表的结构 (字段、类型等)
DESCRIBE orders;
-- 或者用缩写
DESC orders;

-- 查看 'app_users' 表的结构
DESC app_users;

-- 查看所有用户 (数据不多时适用)
SELECT * FROM app_users;

-- 查看所有订单
-- 注意：如果列太多，显示会很乱。
SELECT * FROM orders;

-- 使用 \G 代替 ; 可以让长行记录垂直显示，非常易读！
SELECT * FROM orders WHERE id = 1\G

-- 查询特定状态的订单，例如所有待支付的订单
SELECT id, order_number, status, amount FROM orders WHERE status = 'PENDING_PAYMENT';

-- 查询所有出票失败的订单
SELECT id, order_number, status, creation_date FROM orders WHERE status = 'TICKETING_FAILED';

-- 计算每种状态的订单有多少个，这对于测试状态机非常有用！
SELECT status, COUNT(*) AS total FROM orders GROUP BY status;

-- 使用 JOIN 查询订单及其所属的用户信息
SELECT
    o.id AS order_id,
    o.order_number,
    o.status,
    o.amount,
    u.username AS owner
FROM
    orders o
JOIN
    app_users u ON o.user_id = u.id
WHERE
    o.id = 5;


-- 示例：手动将 ID 为 7 的订单状态从 'TICKETING_IN_PROGRESS' 改为 'TICKETED'
UPDATE orders
SET status = 'TICKETED'
WHERE id = 7;

-- 操作后可以立即查询确认结果
SELECT * FROM orders WHERE id = 7


-- 示例：删除 ID 为 6 的订单
DELETE FROM orders WHERE id = 6;