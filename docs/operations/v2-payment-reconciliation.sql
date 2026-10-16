-- db00只读证据。用户流水必须另用用户路由槽在其分库查账；禁止跨库更新余额。
SELECT o.order_id,o.user_id,o.state,o.recovery_status,o.paid_at,o.delivered_at,
       COUNT(f.transaction_id) AS paid_identities
FROM payment_order o LEFT JOIN provider_payment_fact f ON f.order_id=o.order_id AND f.paid=1
GROUP BY o.order_id,o.user_id,o.state,o.recovery_status,o.paid_at,o.delivered_at
HAVING (o.state IN ('PAID','DELIVERED') AND COUNT(f.transaction_id)<>1);
SELECT order_id,provider,account,environment,transaction_id,amount,currency,reversed,paid
FROM provider_payment_fact WHERE reversed=1;
SELECT order_id,reason,payload,created_at FROM payment_review ORDER BY created_at;
SELECT o.order_id,o.state,TIMESTAMPDIFF(MICROSECOND,o.paid_at,o.delivered_at)/1000 AS paid_to_delivered_ms
FROM payment_order o WHERE o.state='DELIVERED';
-- 在routeSlot对应用户分库、{{suffix}}替换为受信任000..003后执行：
-- SELECT user_id,biz_id,request_hash,delta,balance_after,receipt
-- FROM user_credit_ledger_{{suffix}} WHERE biz_type='CHARGE' AND biz_id=CONCAT('charge:',:order_id);
-- 对照冻结商品credit、PaymentPayload权益摘要与原CreditGranted receipt；balance_after是历史回执而非当前余额。
