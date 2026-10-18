/**
 * Shared k6 options for tooling/bench/run.sh: a constant number of virtual users for a fixed duration,
 * configured from the environment so scenarios only describe what they call.
 */
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: Number(__ENV.VUS || 100),
    duration: __ENV.DURATION || '60s',
    summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

export function callTarget() {
    const response = http.get(`${__ENV.BASE_URL}${__ENV.TARGET_PATH}`);
    check(response, { 'status is 200': (r) => r.status === 200 });
}
