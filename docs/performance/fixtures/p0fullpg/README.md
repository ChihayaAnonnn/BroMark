# P0 2,000 用户 PostgreSQL fixture

**仅用于本地/压测，不是生产数据。** 包含测试 JWT 私钥、tokens 和 2,000 用户/10 活动/10 奖的 PG 数据。

- `seed.json` / `tokens.json` / `private.pem` / `public.der`：run-id `p0fullpg` 的 seed 与 token。
- `db/router_p0_p0fullpg_{control,one,two}.dump`：`pg_dump -Fc` 的完整三库（schema + data）。

## 恢复

```sh
for part in control one two; do
  createdb -h 127.0.0.1 -p 15432 -U postgres router_p0_p0fullpg_$part
  pg_restore -h 127.0.0.1 -p 15432 -U postgres \
    -d router_p0_p0fullpg_$part db/router_p0_p0fullpg_$part.dump
done
```

应用启动：

```sh
export V2_DB00=router_p0_p0fullpg_control
export V2_DB01=router_p0_p0fullpg_one
export V2_DB02=router_p0_p0fullpg_two
export BM_V2_AUTH_PUBLIC_KEY="$(python3 -c 'import json;print(json.load(open("docs/performance/fixtures/p0fullpg/seed.json"))["publicKey"])')"
```

之后按根 README 的 `v2-p0` 启动和 k6 步骤执行。
