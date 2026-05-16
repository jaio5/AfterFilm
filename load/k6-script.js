import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
  vus: 20,
  duration: '30s',
};

export default function () {
  const res = http.get(`${__ENV.TARGET_URL}/actuator/health`);
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
