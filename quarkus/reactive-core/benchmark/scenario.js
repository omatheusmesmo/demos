/**
 * Every scenario calls one endpoint; run.sh passes which one through TARGET_PATH.
 */
import { callTarget } from '../../../tooling/bench/k6-lib.js';

export { options } from '../../../tooling/bench/k6-lib.js';

export default callTarget;
