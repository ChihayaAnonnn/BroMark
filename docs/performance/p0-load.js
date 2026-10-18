import http from 'k6/http';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';
import { Counter, Trend } from 'k6/metrics';

// P0 7:1:1:1 混合负载。BASE_URL/SEED_DIR 必填，脚本不内置任何主机名。
// 输出：每个请求一行纯 JSON（runbook 用 --log-format raw --console-output 收集）。
const SEED_DIR = (__ENV.SEED_DIR || '').replace(/\/+$/, '');
const BASE_URL = (__ENV.BASE_URL || '').replace(/\/+$/, '');
if (!SEED_DIR) throw new Error('SEED_DIR is required and must contain seed.json/tokens.json');
if (!BASE_URL) throw new Error('BASE_URL is required, for example http://M5_IP:8091');

const seed = JSON.parse(open(SEED_DIR + '/seed.json'));
const tokens = new SharedArray('p0_tokens', function () {
  const dir = (__ENV.SEED_DIR || '').replace(/\/+$/, '');
  return Object.entries(JSON.parse(open(dir + '/tokens.json')));
});

if (!tokens.length) throw new Error('tokens.json must contain at least one identity');
if (!Array.isArray(seed.activities) || seed.activities.length === 0)
  throw new Error('seed.json activities must be a non-empty array');
if (!Array.isArray(seed.prizes) || seed.prizes.length === 0)
  throw new Error('seed.json prizes must be a non-empty array');
if (seed.fallback && seed.fallback.type !== 'NONE')
  throw new Error('P0 seed fallback must be NONE');
for (const prize of seed.prizes) {
  if (prize.type !== 'CREDIT' || !prize.stockPoolId)
    throw new Error('P0 requires every CREDIT prize to have an independent stockPoolId');
}

function positiveInt(value, fallback, name) {
  if (value === undefined || value === '') return fallback;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) throw new Error(name + ' must be a positive integer');
  return parsed;
}

const VUS = positiveInt(__ENV.VUS, 100, 'VUS');
const DURATION = __ENV.DURATION || '3m';
const RATE = __ENV.RATE === undefined || __ENV.RATE === '' ? null : Number(__ENV.RATE);
if (RATE !== null && (!Number.isFinite(RATE) || RATE <= 0))
  throw new Error('RATE must be a positive number when provided');

const RUN_ID = (__ENV.RUN_ID || `${seed.runId}-${Date.now()}`).replace(/\s+/g, '_');
const TIMEOUT = __ENV.TIMEOUT || '10s';
const USER_COUNT = tokens.length;
const ACTIVITY_COUNT = seed.activities.length;

// RATE 是混合请求总到达率；7:1:1:1 下 draw/s = RATE / 10。
export const options = RATE
  ? {
      scenarios: {
        load: {
          executor: 'constant-arrival-rate',
          rate: RATE,
          timeUnit: '1s',
          duration: DURATION,
          preAllocatedVUs: VUS,
          maxVUs: VUS,
        },
      },
      discardResponseBodies: false,
      summaryTrendStats: ['p(50)', 'p(95)', 'p(99)', 'max'],
      systemTags: ['status', 'method', 'name', 'scenario'],
    }
  : {
      vus: VUS,
      duration: DURATION,
      discardResponseBodies: false,
      summaryTrendStats: ['p(50)', 'p(95)', 'p(99)', 'max'],
      systemTags: ['status', 'method', 'name', 'scenario'],
    };

const sent = new Counter('p0_sent_total');
const accepted = new Counter('p0_accepted_total');
const rejected = new Counter('p0_rejected_total');
const technicalErrors = new Counter('p0_technical_errors_total');
const latency = new Trend('p0_request_latency_ms');

// 除带 op tag 的总量外，再给每个操作独立计数器，便于 summary 直接读 7:1:1:1。
const OPS = ['query', 'result', 'exchange', 'draw'];
const perOperation = {};
for (const opName of OPS) {
  perOperation[opName] = {
    sent: new Counter(`p0_${opName}_sent_total`),
    accepted: new Counter(`p0_${opName}_accepted_total`),
    rejected: new Counter(`p0_${opName}_rejected_total`),
    errors: new Counter(`p0_${opName}_technical_errors_total`),
  };
}

const USER_OFFSET = { query: 0, result: 53, exchange: 101, draw: 149 };

function userFor(op, ordinal) {
  return tokens[(ordinal + USER_OFFSET[op]) % USER_COUNT];
}

function activityFor(op, cycle) {
  return seed.activities[(cycle + USER_OFFSET[op]) % ACTIVITY_COUNT];
}

export default function () {
  // 全局 iteration 让 7:1:1:1 在开环、闭环和任意 VU 数下都成立。
  const iteration = exec.scenario.iterationInTest;
  const cycle = Math.floor(iteration / 10);
  const slot = iteration % 10;
  const op = slot < 7 ? 'query' : slot === 7 ? 'result' : slot === 8 ? 'exchange' : 'draw';
  const ordinal = op === 'query' ? cycle * 7 + slot : cycle;
  const [user, token] = userFor(op, ordinal);
  const activity = activityFor(op, cycle);
  const requestId = `${RUN_ID}:${op}:${iteration}`;

  let method = 'GET';
  let path = `/api/v2/activities/${activity.activityId}`;
  let body = null;
  let targetOrderId = null;

  if (op === 'result') {
    if (!seed.resultOrders || !seed.resultOrders[user]) {
      // 正式 seed 会为每个用户预置一条 DELIVERED 结果订单；缺失时仍发请求，
      // 让 404 进入 rejected 统计而不是让整个压测在初始化阶段崩掉。
      targetOrderId = `P0-MISSING-${user}`;
    } else {
      targetOrderId = seed.resultOrders[user];
    }
    path = `/api/v2/draws/${encodeURIComponent(targetOrderId)}`;
  } else if (op === 'exchange') {
    method = 'POST';
    path = '/api/v2/credit/exchanges';
    body = { requestId, sku: seed.sku, configVersion: seed.exchangeVersion || seed.version };
  } else if (op === 'draw') {
    method = 'POST';
    path = '/api/v2/draws';
    body = { requestId, activityId: activity.activityId };
  }

  const headers = { Authorization: `Bearer ${token}` };
  if (method === 'POST') headers['Content-Type'] = 'application/json';

  sent.add(1, { op });
  perOperation[op].sent.add(1);
  const response = http.request(method, BASE_URL + path, body === null ? null : JSON.stringify(body), {
    headers,
    tags: { name: op, op },
    timeout: TIMEOUT,
  });

  let value = {};
  try {
    value = response.json();
  } catch (e) {
    value = {};
  }
  const status = response.status;
  const state = value && value.state ? value.state : null;
  const ok = status >= 200 && status < 300 && state !== 'REJECTED';
  const isTechnical = status === 0 || status >= 500;

  if (ok) {
    accepted.add(1, { op });
    perOperation[op].accepted.add(1);
  } else {
    rejected.add(1, { op });
    perOperation[op].rejected.add(1);
  }
  if (isTechnical) {
    technicalErrors.add(1, { op });
    perOperation[op].errors.add(1);
  }
  latency.add(response.timings.duration, { op });

  const line = {
    requestId,
    runId: RUN_ID,
    user,
    op,
    status,
    latency: Math.round(response.timings.duration * 1000) / 1000,
    orderId: value && value.orderId ? value.orderId : null,
    state,
    activityId: op === 'query' || op === 'draw' ? activity.activityId : null,
    targetOrderId,
    error: isTechnical ? `HTTP ${status}` : null,
    reason: value && (value.reason || value.info) ? value.reason || value.info : null,
    at: new Date().toISOString(),
  };
  console.log(JSON.stringify(line));
}
