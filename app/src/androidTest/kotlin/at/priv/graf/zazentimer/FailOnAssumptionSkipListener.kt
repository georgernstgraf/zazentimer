package at.priv.graf.zazentimer

import android.os.Process
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

/**
 * Fail-fast guard that crashes the instrumentation process whenever a test is
 * skipped via `org.junit.Assume`.
 *
 * No `Assume` exists in the suite today; this listener is a future-regression
 * guard so a silently-skipped test crashes its phase instead of producing a
 * false-green `OK (N tests)`. The previous dot-parser in
 * `scripts/run-instrumentation.sh` was removed because GPU/SwiftShader log
 * lines interleaved into the per-class result dots on the API 34 emulator,
 * causing false assumption-skip positives (see #289, api34 log).
 *
 * Registered via `am instrument -e listener <FQCN>`; AndroidJUnitRunner
 * instantiates it reflectively, hence the public default constructor.
 */
class FailOnAssumptionSkipListener : RunListener() {
    override fun testAssumptionFailure(failure: Failure) {
        System.err.println("FAIL_ON_ASSUMPTION_SKIP: ${failure.description}")
        Process.killProcess(Process.myPid())
    }
}
