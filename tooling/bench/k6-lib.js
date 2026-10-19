/**
 * Shared k6 options for tooling/bench/run.sh: an open model. Requests start at a fixed arrival rate whether or not
 * earlier ones have finished, so a slow server shows up as latency and dropped iterations instead of quietly lowering
 * the load (the coordinated omission of a closed model with a fixed number of virtual users).
 *
 * Environment: RATE (requests per second), DURATION, PRE_ALLOCATED_VUS, MAX_VUS, TIMEOUT (per request).
 */
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        open: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 100),
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 200),
            maxVUs: Number(__ENV.MAX_VUS || 5000),
        },
    },
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(99)', 'max'],
    discardResponseBodies: true,
};

export function callTarget() {
    const response = http.get(`${__ENV.BASE_URL}${__ENV.TARGET_PATH}`, { timeout: __ENV.TIMEOUT || '10s' });
    check(response, { 'status is 200': (r) => r.status === 200 });
}
