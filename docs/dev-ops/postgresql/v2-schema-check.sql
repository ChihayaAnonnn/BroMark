-- 在空库执行 v2-schema.sql 两遍后运行；业务数据验证全部回滚。
BEGIN;
DO $$
DECLARE actual integer;
BEGIN
    SELECT count(*) INTO actual FROM pg_tables WHERE schemaname = 'public';
    ASSERT actual = 74, format('expected 74 tables, got %s', actual);
    SELECT count(*) INTO actual FROM pg_indexes WHERE schemaname = 'public';
    ASSERT actual = 120, format('expected 120 indexes, got %s', actual);
    SELECT count(*) INTO actual FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
      WHERE n.nspname = 'public' AND c.contype IN ('p','u','f','c');
    ASSERT actual = 130, format('expected 130 PK/unique/FK/check constraints, got %s', actual);
    SELECT count(*) INTO actual FROM pg_class WHERE relkind = 'S' AND relnamespace = 'public'::regnamespace;
    ASSERT actual = 5, format('expected 5 identity sequences, got %s', actual);
END $$;
SET LOCAL TIME ZONE 'Pacific/Honolulu';
INSERT INTO user_credit_account(user_id) VALUES ('pg_ddl_check'), ('PG_DDL_CHECK');
INSERT INTO user_credit_ledger_000(user_id,biz_type,biz_id,request_hash,delta,balance_after,receipt)
VALUES ('pg_ddl_check','TEST','one',repeat('a',64),1.25,1.25,'{"用户":"中文","n":1.25}');
INSERT INTO message_quarantine(consumer_name,body_hash,body,reason,created_at)
VALUES ('pg_ddl_check',repeat('b',64),decode('00ff805c','hex'),'test',timestamp '2026-10-18 01:02:03.123456');
INSERT INTO recovery_case(case_id,aggregate_type,aggregate_id,business_state,recovery_status,
    object_version,active_key,evidence,created_at)
VALUES ('pg_ddl_check1','TEST','one','TEST','ACTIVE',0,repeat('c',64),'test',clock_timestamp() AT TIME ZONE 'UTC'),
       ('pg_ddl_check2','TEST','one','TEST','CLOSED',0,NULL,'test',clock_timestamp() AT TIME ZONE 'UTC'),
       ('pg_ddl_check3','TEST','one','TEST','CLOSED',0,NULL,'test',clock_timestamp() AT TIME ZONE 'UTC');
DO $$
DECLARE generated_id bigint; recorded timestamp;
BEGIN
    SELECT ledger_id INTO generated_id FROM user_credit_ledger_000 WHERE user_id = 'pg_ddl_check';
    ASSERT generated_id > 0;
    ASSERT (SELECT receipt->>'用户' = '中文' AND receipt->>'n' = '1.25' AND balance_after = 1.25
        AND request_hash = repeat('a',64) FROM user_credit_ledger_000 WHERE ledger_id = generated_id);
    ASSERT (SELECT encode(body,'hex') = '00ff805c' AND created_at = timestamp '2026-10-18 01:02:03.123456'
        FROM message_quarantine WHERE consumer_name = 'pg_ddl_check');
    SELECT create_time INTO recorded FROM user_credit_account WHERE user_id = 'pg_ddl_check';
    ASSERT abs(extract(epoch FROM ((clock_timestamp() AT TIME ZONE 'UTC') - recorded))) < 2;
    BEGIN
        INSERT INTO user_credit_account(user_id,balance) VALUES ('pg_negative_check',-1);
        RAISE EXCEPTION 'negative balance accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO user_credit_ledger_000(user_id,biz_type,biz_id,request_hash,delta,balance_after,receipt)
        VALUES ('pg_ddl_check','TEST','one',repeat('a',64),1,1,'{}');
        RAISE EXCEPTION 'duplicate business identity accepted';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;
    BEGIN
        UPDATE recovery_case SET active_key = repeat('c',64) WHERE case_id = 'pg_ddl_check2';
        RAISE EXCEPTION 'duplicate active recovery accepted';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;
    UPDATE recovery_case SET active_key = NULL WHERE case_id = 'pg_ddl_check1';
    UPDATE recovery_case SET active_key = repeat('c',64) WHERE case_id = 'pg_ddl_check2';
END $$;
ROLLBACK;
