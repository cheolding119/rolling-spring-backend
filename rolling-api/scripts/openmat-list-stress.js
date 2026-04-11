import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PAGE_SIZE = Number(__ENV.SIZE || 20);
const MAX_PAGE = Number(__ENV.MAX_PAGE || 200);
const SLEEP_SECONDS = Number(__ENV.SLEEP_SECONDS || 0.5);

export const options = {
  stages: [
    { duration: __ENV.STAGE_1_DURATION || '2m', target: Number(__ENV.STAGE_1_TARGET || 10) },
    { duration: __ENV.STAGE_2_DURATION || '3m', target: Number(__ENV.STAGE_2_TARGET || 20) },
    { duration: __ENV.STAGE_3_DURATION || '3m', target: Number(__ENV.STAGE_3_TARGET || 40) },
    { duration: __ENV.STAGE_4_DURATION || '2m', target: Number(__ENV.STAGE_4_TARGET || 60) },
    { duration: __ENV.STAGE_5_DURATION || '2m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.10'],
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
    checks: ['rate>0.95'],
  },
};

function buildUrl() {
  const page = Math.floor(Math.random() * MAX_PAGE);
  const params = [
    `page=${page}`,
    `size=${PAGE_SIZE}`,
  ];

  if (__ENV.REGION) {
    params.push(`region=${encodeURIComponent(__ENV.REGION)}`);
  }

  if (__ENV.STATUS) {
    params.push(`status=${encodeURIComponent(__ENV.STATUS)}`);
  }

  if (__ENV.KEYWORD) {
    params.push(`q=${encodeURIComponent(__ENV.KEYWORD)}`);
  }

  return `${BASE_URL}/api/v1/open-mats?${params.join('&')}`;
}

export default function () {
  const res = http.get(buildUrl(), {
    tags: {
      name: 'openmat_list_stress',
    },
  });

  let body;
  let parsed = false;

  try {
    body = res.json();
    parsed = true;
  } catch (e) {
    body = null;
  }

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is json': () => parsed,
    'api success is true': () => parsed && body.success === true,
    'page content exists': () => parsed && body.data && Array.isArray(body.data.content),
  });

  sleep(SLEEP_SECONDS);
}
