import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const OPENMAT_ID = Number(__ENV.OPENMAT_ID || 0);
const TOKENS = (__ENV.ACCESS_TOKENS || '')
  .split(/[,\r\n]+/)
  .map((token) => token.trim())
  .filter(Boolean);
const TEST_USER_IDS = (__ENV.TEST_USER_IDS || '')
  .split(/[,\r\n]+/)
  .map((userId) => userId.trim())
  .filter(Boolean);
const SLEEP_SECONDS = Number(__ENV.SLEEP_SECONDS || 0.5);
const RESET_AFTER_APPLY = (__ENV.RESET_AFTER_APPLY || 'true').toLowerCase() === 'true';
const EXPECT_BUSINESS_REJECT = (__ENV.EXPECT_BUSINESS_REJECT || 'false').toLowerCase() === 'true';

const applySuccess = new Counter('openmat_apply_success_total');
const applyBusinessReject = new Counter('openmat_apply_business_reject_total');
const applyServerError = new Counter('openmat_apply_server_error_total');
const cancelSuccess = new Counter('openmat_cancel_success_total');
const cancelError = new Counter('openmat_cancel_error_total');
const applyOkRate = new Rate('openmat_apply_ok_rate');

export const options = {
  stages: [
    { duration: __ENV.STAGE_1_DURATION || '2m', target: Number(__ENV.STAGE_1_TARGET || 5) },
    { duration: __ENV.STAGE_2_DURATION || '3m', target: Number(__ENV.STAGE_2_TARGET || 10) },
    { duration: __ENV.STAGE_3_DURATION || '3m', target: Number(__ENV.STAGE_3_TARGET || 20) },
    { duration: __ENV.STAGE_4_DURATION || '2m', target: Number(__ENV.STAGE_4_TARGET || 30) },
    { duration: __ENV.STAGE_5_DURATION || '2m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<4000'],
    checks: ['rate>0.95'],
    openmat_apply_ok_rate: ['rate>0.90'],
  },
};

function parseJson(res) {
  try {
    return res.json();
  } catch (e) {
    return null;
  }
}

function authHeaders(token, testUserId) {
  const headers = {};

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (testUserId) {
    headers['X-Test-User-Id'] = testUserId;
  }

  return {
    headers,
  };
}

function pickIdentity() {
  if (TOKENS.length === 0 && TEST_USER_IDS.length === 0) {
    throw new Error('Provide ACCESS_TOKENS or TEST_USER_IDS.');
  }

  if (TEST_USER_IDS.length > 0) {
    const index = (__VU - 1) % TEST_USER_IDS.length;
    return {
      token: null,
      testUserId: TEST_USER_IDS[index],
    };
  }

  const index = (__VU - 1) % TOKENS.length;
  return {
    token: TOKENS[index],
    testUserId: null,
  };
}

function classifyApply(res, body) {
  if (res.status === 200 && body && body.success === true) {
    applySuccess.add(1);
    applyOkRate.add(true);
    return 'success';
  }

  if (
    res.status === 400 &&
    body &&
    body.success === false &&
    body.error &&
    typeof body.error.code === 'string'
  ) {
    applyBusinessReject.add(1);
    applyOkRate.add(EXPECT_BUSINESS_REJECT);
    return body.error.code;
  }

  if (res.status >= 500) {
    applyServerError.add(1);
  }

  applyOkRate.add(false);
  return 'unexpected';
}

export function setup() {
  if (!OPENMAT_ID) {
    throw new Error('OPENMAT_ID is required.');
  }

  if (TOKENS.length === 0 && TEST_USER_IDS.length === 0) {
    throw new Error('Provide ACCESS_TOKENS or TEST_USER_IDS.');
  }

  const detailRes = http.get(`${BASE_URL}/api/v1/open-mats/${OPENMAT_ID}`);
  check(detailRes, {
    'detail setup status is 200': (r) => r.status === 200,
  });

  const detailBody = parseJson(detailRes);

  if (!detailBody || detailBody.success !== true || !detailBody.data || detailBody.data.id !== OPENMAT_ID) {
    throw new Error(`Failed to validate OPENMAT_ID=${OPENMAT_ID} during setup.`);
  }

  return {
    openMatId: OPENMAT_ID,
  };
}

export default function (data) {
  const identity = pickIdentity();
  const applyRes = http.post(
    `${BASE_URL}/api/v1/open-mats/${data.openMatId}/apply`,
    null,
    authHeaders(identity.token, identity.testUserId)
  );

  const applyBody = parseJson(applyRes);
  const applyResult = classifyApply(applyRes, applyBody);

  check(applyRes, {
    'apply returned expected status': (r) => r.status === 200 || r.status === 400,
    'apply body is json': () => applyBody !== null,
    'apply result is meaningful': () => applyResult !== 'unexpected',
  });

  if (RESET_AFTER_APPLY && applyResult === 'success') {
    const cancelRes = http.del(
      `${BASE_URL}/api/v1/open-mats/${data.openMatId}/apply`,
      null,
      authHeaders(identity.token, identity.testUserId)
    );
    const cancelBody = parseJson(cancelRes);

    const cancelSucceeded = cancelRes.status === 200 && cancelBody && cancelBody.success === true;
    if (cancelSucceeded) {
      cancelSuccess.add(1);
    } else {
      cancelError.add(1);
    }

    check(cancelRes, {
      'cancel after apply succeeds': () => cancelSucceeded,
    });
  }

  sleep(SLEEP_SECONDS);
}
