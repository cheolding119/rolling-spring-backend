import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PAGE_SIZE = Number(__ENV.SIZE || 20);
const SAMPLE_PAGES = Number(__ENV.SAMPLE_PAGES || 10);
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

function buildListUrl(page) {
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

export function setup() {
  const ids = [];

  for (let page = 0; page < SAMPLE_PAGES; page += 1) {
    const res = http.get(buildListUrl(page), {
      tags: {
        name: 'openmat_detail_stress_seed_ids',
      },
    });

    check(res, {
      'seed list status is 200': (r) => r.status === 200,
    });

    let body;

    try {
      body = res.json();
    } catch (e) {
      body = null;
    }

    const content = body && body.data && Array.isArray(body.data.content)
      ? body.data.content
      : [];

    for (const item of content) {
      if (item && item.id) {
        ids.push(item.id);
      }
    }
  }

  const uniqueIds = [...new Set(ids)];

  if (uniqueIds.length === 0) {
    throw new Error('No openmat ids collected from list API during setup.');
  }

  return { ids: uniqueIds };
}

export default function (data) {
  const id = data.ids[Math.floor(Math.random() * data.ids.length)];
  const res = http.get(`${BASE_URL}/api/v1/open-mats/${id}`, {
    tags: {
      name: 'openmat_detail_stress',
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
    'detail contains id': () => parsed && body.data && body.data.id === id,
    'detail contains title': () => parsed && body.data && typeof body.data.title === 'string',
  });

  sleep(SLEEP_SECONDS);
}
