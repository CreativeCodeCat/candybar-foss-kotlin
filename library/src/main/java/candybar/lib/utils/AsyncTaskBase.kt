package candybar.lib.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

abstract class AsyncTaskBase {
    private var mCancelled = false
    private val handler = Handler(Looper.getMainLooper())
    private var mFuture: Future<*>? = null

    protected open fun preRun() {}

    protected abstract fun run(): Boolean

    protected open fun postRun(ok: Boolean) {}

    protected fun runOnUiThread(runnable: Runnable) {
        handler.post(runnable)
    }

    val isCancelled: Boolean
        get() = mCancelled

    open fun cancel(mayInterruptIfRunning: Boolean) {
        mCancelled = true
        mFuture?.cancel(mayInterruptIfRunning)
    }

    protected open fun execute(executorService: ExecutorService): AsyncTaskBase {
        preRun()
        mFuture = executorService.submit {
            if (!mCancelled) {
                val result = run()
                handler.post {
                    if (!mCancelled) postRun(result)
                }
            }
        }
        return this
    }

    fun execute(): AsyncTaskBase {
        return execute(Executors.newSingleThreadExecutor())
    }

    fun executeOnThreadPool(): AsyncTaskBase {
        return execute(THREAD_POOL)
    }

    companion object {
        private val THREAD_POOL: ExecutorService = ThreadPoolExecutor(
            5, 128, 1, TimeUnit.SECONDS,
            LinkedBlockingQueue()
        )
    }
}
