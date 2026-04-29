-- =========================================================
-- 支付网关服务 - 本地开发初始化脚本
-- 文件名：01-schema.sql
-- 说明：
-- 1. 仅包含支付网关服务本身的核心支撑表
-- 2. 不包含核心账务、清结算、资金台账等领域表
-- 3. 适用于本地开发、联调、测试环境初始化
-- =========================================================

CREATE DATABASE IF NOT EXISTS gateway_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE gateway_db;

-- =========================================================
-- 1. 商户接入配置表
-- 用于保存网关层识别商户所需的最小配置，不保存不必要敏感明文
-- =========================================================
CREATE TABLE IF NOT EXISTS merchant_access_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    merchant_id VARCHAR(64) NOT NULL COMMENT '商户号',
    merchant_name VARCHAR(128) NOT NULL COMMENT '商户名称',
    app_id VARCHAR(64) DEFAULT NULL COMMENT '应用标识',
    channel_code VARCHAR(64) DEFAULT NULL COMMENT '渠道编码',
    sign_alg VARCHAR(32) NOT NULL DEFAULT 'RSA256' COMMENT '签名算法',
    encrypt_alg VARCHAR(32) DEFAULT NULL COMMENT '加密算法',
    public_key_ref VARCHAR(256) DEFAULT NULL COMMENT '商户公钥引用',
    private_key_ref VARCHAR(256) DEFAULT NULL COMMENT '平台私钥引用',
    certificate_ref VARCHAR(256) DEFAULT NULL COMMENT '证书引用',
    callback_whitelist TEXT DEFAULT NULL COMMENT '回调白名单，逗号分隔或JSON',
    ip_whitelist TEXT DEFAULT NULL COMMENT '请求来源IP白名单',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_id (merchant_id),
    KEY idx_app_id (app_id),
    KEY idx_channel_code (channel_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户接入配置表';

-- =========================================================
-- 2. 网关接口路由配置表
-- 用于外部接口到内部服务的路由配置
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_route_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    route_code VARCHAR(64) NOT NULL COMMENT '路由编码',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型，如PAY、REFUND、QUERY',
    api_code VARCHAR(64) NOT NULL COMMENT '外部接口编码',
    target_protocol VARCHAR(32) NOT NULL DEFAULT 'DUBBO' COMMENT '目标协议：DUBBO/HTTP/MQ',
    target_service VARCHAR(128) NOT NULL COMMENT '目标服务名或接口名',
    target_method VARCHAR(128) DEFAULT NULL COMMENT '目标方法名',
    target_version VARCHAR(32) DEFAULT NULL COMMENT '目标版本',
    timeout_ms INT NOT NULL DEFAULT 3000 COMMENT '超时时间毫秒',
    retry_times INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    sentinel_resource VARCHAR(128) DEFAULT NULL COMMENT 'Sentinel资源名',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级，数字越小优先级越高',
    ext_config JSON DEFAULT NULL COMMENT '扩展配置JSON',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_route_code (route_code),
    KEY idx_biz_type_api_code (biz_type, api_code),
    KEY idx_status_priority (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关路由配置表';

-- =========================================================
-- 3. 请求幂等记录表
-- 用于防重复提交与幂等控制
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_idempotency_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    merchant_id VARCHAR(64) NOT NULL COMMENT '商户号',
    request_id VARCHAR(64) NOT NULL COMMENT '请求号',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    request_hash VARCHAR(512) DEFAULT NULL COMMENT '请求摘要',
    process_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-处理中，1-成功，2-失败',
    response_code VARCHAR(64) DEFAULT NULL COMMENT '响应码',
    response_message VARCHAR(255) DEFAULT NULL COMMENT '响应信息',
    result_gateway_payment_id VARCHAR(64) DEFAULT NULL COMMENT '网关支付单号',
    result_status VARCHAR(32) DEFAULT NULL COMMENT '处理结果状态',
    result_route_code VARCHAR(64) DEFAULT NULL COMMENT '命中路由编码',
    result_processed_at DATETIME(3) DEFAULT NULL COMMENT '处理完成时间',
    expire_at DATETIME(3) NOT NULL COMMENT '过期时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    KEY idx_merchant_request (merchant_id, request_id),
    KEY idx_expire_at (expire_at),
    KEY idx_process_status (process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请求幂等记录表';

-- =========================================================
-- 4. 防重放记录表
-- 用于时间戳窗口内重放保护
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_replay_protection_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    replay_key VARCHAR(128) NOT NULL COMMENT '防重放键',
    merchant_id VARCHAR(64) NOT NULL COMMENT '商户号',
    nonce VARCHAR(128) DEFAULT NULL COMMENT '随机串',
    request_timestamp DATETIME(3) NOT NULL COMMENT '请求时间',
    expire_at DATETIME(3) NOT NULL COMMENT '过期时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_replay_key (replay_key),
    KEY idx_merchant_id (merchant_id),
    KEY idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='防重放记录表';

-- =========================================================
-- 5. 网关请求流水表
-- 用于记录网关接入请求的关键上下文，支持审计与排障
-- 注意：不保存不必要敏感明文
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_request_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求号',
    idempotency_key VARCHAR(128) DEFAULT NULL COMMENT '幂等键',
    merchant_id VARCHAR(64) DEFAULT NULL COMMENT '商户号',
    app_id VARCHAR(64) DEFAULT NULL COMMENT '应用标识',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    api_code VARCHAR(64) NOT NULL COMMENT '接口编码',
    http_method VARCHAR(16) NOT NULL COMMENT 'HTTP方法',
    request_uri VARCHAR(255) NOT NULL COMMENT '请求URI',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    user_agent VARCHAR(255) DEFAULT NULL COMMENT '用户代理',
    request_time DATETIME(3) NOT NULL COMMENT '请求开始时间',
    finish_time DATETIME(3) DEFAULT NULL COMMENT '请求结束时间',
    duration_ms INT DEFAULT NULL COMMENT '耗时毫秒',
    route_code VARCHAR(64) DEFAULT NULL COMMENT '命中路由',
    target_service VARCHAR(128) DEFAULT NULL COMMENT '目标服务',
    response_code VARCHAR(64) DEFAULT NULL COMMENT '响应码',
    response_status VARCHAR(32) DEFAULT NULL COMMENT '响应状态，如SUCCESS/FAIL/BLOCK/FALLBACK',
    error_type VARCHAR(64) DEFAULT NULL COMMENT '错误类型',
    error_message VARCHAR(512) DEFAULT NULL COMMENT '错误信息摘要',
    request_summary JSON DEFAULT NULL COMMENT '脱敏后的请求摘要',
    ext_json JSON DEFAULT NULL COMMENT '扩展字段',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_trace_id (trace_id),
    KEY idx_request_id (request_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_request_time (request_time),
    KEY idx_biz_type_api_code (biz_type, api_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关请求流水表';

-- =========================================================
-- 6. 网关支付单表
-- 用于记录网关受理后的支付单与下游流水关联关系
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_payment_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    gateway_payment_id VARCHAR(64) NOT NULL COMMENT '网关支付单号',
    merchant_id VARCHAR(64) NOT NULL COMMENT '商户号',
    request_id VARCHAR(64) NOT NULL COMMENT '请求号',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    route_code VARCHAR(64) NOT NULL COMMENT '命中路由编码',
    target_service VARCHAR(128) NOT NULL COMMENT '目标服务',
    downstream_payment_id VARCHAR(64) NOT NULL COMMENT '下游支付单号',
    payment_status VARCHAR(32) NOT NULL COMMENT '当前支付状态',
    amount VARCHAR(32) NOT NULL COMMENT '金额文本',
    currency VARCHAR(16) NOT NULL COMMENT '币种',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_gateway_payment_id (gateway_payment_id),
    KEY idx_merchant_request (merchant_id, request_id),
    KEY idx_downstream_payment_id (downstream_payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关支付单表';

-- =========================================================
-- 6.1 网关退款单表
-- 用于记录网关受理后的退款单与下游退款流水关联关系
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_refund_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    gateway_refund_id VARCHAR(64) NOT NULL COMMENT '网关退款单号',
    merchant_id VARCHAR(64) NOT NULL COMMENT '商户号',
    request_id VARCHAR(64) NOT NULL COMMENT '请求号',
    gateway_payment_id VARCHAR(64) NOT NULL COMMENT '关联网关支付单号',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    route_code VARCHAR(64) NOT NULL COMMENT '命中路由编码',
    target_service VARCHAR(128) NOT NULL COMMENT '目标服务',
    downstream_refund_id VARCHAR(64) NOT NULL COMMENT '下游退款单号',
    refund_status VARCHAR(32) NOT NULL COMMENT '当前退款状态',
    amount VARCHAR(32) NOT NULL COMMENT '金额文本',
    currency VARCHAR(16) NOT NULL COMMENT '币种',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_gateway_refund_id (gateway_refund_id),
    KEY idx_refund_merchant_request (merchant_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关退款单表';

-- =========================================================
-- 7. 网关异常事件表
-- 用于记录安全失败、限流、熔断、路由失败、下游异常等
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_exception_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
    request_id VARCHAR(64) DEFAULT NULL COMMENT '请求号',
    merchant_id VARCHAR(64) DEFAULT NULL COMMENT '商户号',
    biz_type VARCHAR(64) DEFAULT NULL COMMENT '业务类型',
    api_code VARCHAR(64) DEFAULT NULL COMMENT '接口编码',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型，如SIGN_VERIFY_FAIL、RATE_LIMIT_BLOCK、DUBBO_TIMEOUT',
    event_level VARCHAR(16) NOT NULL DEFAULT 'WARN' COMMENT '事件级别：INFO/WARN/ERROR',
    event_code VARCHAR(64) DEFAULT NULL COMMENT '事件编码',
    event_message VARCHAR(512) DEFAULT NULL COMMENT '事件描述',
    detail_json JSON DEFAULT NULL COMMENT '扩展详情',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_trace_id (trace_id),
    KEY idx_request_id (request_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_event_type (event_type),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网关异常事件表';

-- =========================================================
-- 8. MQ事件出站记录表
-- 用于异步通知、补偿、回执等事件的发送跟踪
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_mq_outbox (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_key VARCHAR(128) NOT NULL COMMENT '事件键',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    topic VARCHAR(128) NOT NULL COMMENT 'Topic',
    tag VARCHAR(64) DEFAULT NULL COMMENT 'Tag',
    message_key VARCHAR(128) DEFAULT NULL COMMENT '消息Key',
    payload_json JSON NOT NULL COMMENT '消息体JSON',
    send_status TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态：0-待发送，1-已发送，2-发送失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_time DATETIME(3) DEFAULT NULL COMMENT '下次重试时间',
    last_error_message VARCHAR(512) DEFAULT NULL COMMENT '最后错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_key (event_key),
    KEY idx_send_status_next_retry_time (send_status, next_retry_time),
    KEY idx_biz_type (biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ事件出站记录表';

-- =========================================================
-- 8.1 MQ消费记录表
-- 用于记录通知消费结果、失败重试与死信边界
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_message_consume_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_key VARCHAR(128) NOT NULL COMMENT '消息Key',
    biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    consumer_group VARCHAR(128) NOT NULL COMMENT '消费组',
    payload_json JSON NOT NULL COMMENT '消息体JSON',
    consume_status VARCHAR(32) NOT NULL COMMENT '消费状态：SUCCESS/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '消费重试次数',
    dead_letter TINYINT NOT NULL DEFAULT 0 COMMENT '是否进入死信语义：0-否，1-是',
    last_error_message VARCHAR(512) DEFAULT NULL COMMENT '最后错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_key (message_key),
    KEY idx_consume_status_dead_letter (consume_status, dead_letter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ消费记录表';

-- =========================================================
-- 9. 系统参数配置表
-- 用于数据库维度保存少量动态参数，不替代 Nacos
-- =========================================================
CREATE TABLE IF NOT EXISTS gateway_system_param (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    param_group VARCHAR(64) NOT NULL COMMENT '参数分组',
    param_key VARCHAR(128) NOT NULL COMMENT '参数键',
    param_value VARCHAR(1024) NOT NULL COMMENT '参数值',
    value_type VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT '值类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_key (param_group, param_key),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数配置表';

-- =========================================================
-- 初始化基础数据
-- =========================================================

INSERT INTO merchant_access_config
(merchant_id, merchant_name, app_id, channel_code, sign_alg, encrypt_alg, public_key_ref, private_key_ref, certificate_ref, callback_whitelist, ip_whitelist, status, remark)
VALUES
('MCH100001', '本地测试商户A', 'APP100001', 'DEFAULT', 'RSA256', 'AES', 'classpath:security/test-certs/mch100001-public.pem', 'classpath:security/test-certs/platform-private.pem', 'classpath:security/test-certs/platform-cert.pem', 'http://localhost:8088/callback', '127.0.0.1', 1, '本地联调用测试商户')
ON DUPLICATE KEY UPDATE merchant_name = VALUES(merchant_name), updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO gateway_route_config
(route_code, biz_type, api_code, target_protocol, target_service, target_method, target_version, timeout_ms, retry_times, sentinel_resource, status, priority, ext_config, remark)
VALUES
('ROUTE_PAY_CREATE', 'PAY', 'CREATE', 'DUBBO', 'com.example.payment.api.PaymentCreateFacade', 'createPayment', '1.0.0', 3000, 0, 'gateway:pay:create', 1, 100, JSON_OBJECT('checkSign', true, 'checkReplay', true), '支付创建路由'),
('ROUTE_PAY_QUERY', 'PAY', 'QUERY', 'DUBBO', 'com.example.payment.api.PaymentQueryFacade', 'queryPayment', '1.0.0', 2000, 0, 'gateway:pay:query', 1, 100, JSON_OBJECT('checkSign', true, 'checkReplay', false), '支付查询路由'),
('ROUTE_REFUND_CREATE', 'REFUND', 'CREATE', 'DUBBO', 'com.example.payment.api.RefundFacade', 'createRefund', '1.0.0', 3000, 0, 'gateway:refund:create', 1, 100, JSON_OBJECT('checkSign', true, 'checkReplay', true), '退款创建路由'),
('ROUTE_REFUND_QUERY', 'REFUND', 'QUERY', 'DUBBO', 'com.example.payment.api.RefundFacade', 'queryRefund', '1.0.0', 2000, 0, 'gateway:refund:query', 1, 100, JSON_OBJECT('checkSign', true, 'checkReplay', false), '退款查询路由')
ON DUPLICATE KEY UPDATE target_service = VALUES(target_service), updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO gateway_system_param
(param_group, param_key, param_value, value_type, status, remark)
VALUES
('security', 'request.expire.seconds', '300', 'INTEGER', 1, '请求时间戳有效期，单位秒'),
('security', 'replay.protect.seconds', '300', 'INTEGER', 1, '防重放保护时间窗口，单位秒'),
('idempotency', 'record.expire.seconds', '86400', 'INTEGER', 1, '幂等记录保留时间，单位秒'),
('governance', 'default.timeout.ms', '3000', 'INTEGER', 1, '默认超时时间'),
('governance', 'default.retry.times', '0', 'INTEGER', 1, '默认重试次数')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value), updated_at = CURRENT_TIMESTAMP(3);
