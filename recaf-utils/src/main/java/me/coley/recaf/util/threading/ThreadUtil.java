package me.coley.recaf.util.threading;

import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.concurrent.*;

import static me.coley.recaf.util.threading.ThreadPoolFactory.newScheduledThreadPool;

/**
 * Common threading utility. Used for <i>"miscellaneous"</i> threads.
 * Larger thread operations should create their own pools using {@link ThreadPoolFactory}.
 *
 * @author Matt Coley
 */
public class ThreadUtil {
	private static final Logger logger = Logging.get(ThreadUtil.class);
	private static final ScheduledExecutorService scheduledService = newScheduledThreadPool("Recaf misc");

	/**
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Thread future.
	 */
	public static Future<?> run(Runnable action) {
		return scheduledService.submit(wrap(action));
	}

	/**
	 * @param delayMs
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Scheduled future.
	 */
	public static Future<?> runDelayed(long delayMs, Runnable action) {
		return scheduledService.schedule(wrap(action), delayMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * Run a given action with a timeout.
	 *
	 * @param millis
	 * 		Timeout in milliseconds.
	 * @param action
	 * 		Runnable to execute.
	 *
	 * @return {@code true} When thread completed before time.
	 */
	public static boolean timeout(int millis, Runnable action) {
		try {
			Future<?> future = run(action);
			return timeout(millis, future);
		} catch (Throwable t) {
			// Can be thrown by execution timeout
			return false;
		}
	}

	/**
	 * Give a thread future a time limit.
	 *
	 * @param millis
	 * 		Timeout in milliseconds.
	 * @param future
	 * 		Thread future being run.
	 *
	 * @return {@code true} When thread completed before time.
	 */
	public static boolean timeout(int millis, Future<?> future) {
		try {
			future.get(millis, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException e) {
			// Expected: Timeout
			return false;
		} catch (Throwable t) {
			// Other error
			return true;
		}
	}


	/**
	 * Give a thread pool a time limit to finish all of its threads.
	 *
	 * @param millis
	 * 		Timeout in milliseconds.
	 * @param service
	 * 		Thread pool being used.
	 *
	 * @return {@code true} when thread pool completed before time.
	 * {@code false} when the thread pool did not finish, or was interrupted.
	 */
	public static boolean timeout(int millis, ExecutorService service) {
		try {
			// Shutdown so no new tasks are completed, but existing ones will finish.
			service.shutdown();
			// Wait until they finish. The prior shutdown request is required.
			// Calling 'awaitTermination' without calling shutdown will hang forever.
			return service.awaitTermination(millis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// A thread was interrupted so operation did not complete.
			return false;
		} catch (Throwable t) {
			// Other error
			return true;
		}
	}

	/**
	 * @param future
	 * 		Thread future being run.
	 *
	 * @return {@code true} on completion. {@code false} for interruption.
	 */
	public static boolean blockUntilComplete(Future<?> future) {
		return timeout(Integer.MAX_VALUE, future);
	}

	/**
	 * @param service
	 * 		Thread pool being used.
	 *
	 * @return {@code true} on completion. {@code false} for interruption.
	 */
	public static boolean blockUntilComplete(ExecutorService service) {
		return timeout(Integer.MAX_VALUE, service);
	}

	/**
	 * Submits a periodic action that becomes enabled first after the given initial delay,
	 * and subsequently with the given period.
	 *
	 * @param task
	 * 		Task to execute.
	 * @param initialDelay
	 * 		The time to delay first execution.
	 * @param period
	 * 		The period between successive executions.
	 * @param unit
	 * 		The time unit of the initialDelay
	 * 		and period parameters.
	 *
	 * @return future representing completion of the tasks.
	 *
	 * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
	 */
	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay,
														 long period, TimeUnit unit) {
		return scheduledService.scheduleAtFixedRate(task, initialDelay, period, unit);
	}

	/**
	 * Wrap action to handle error logging.
	 *
	 * @param action
	 * 		Action to run.
	 *
	 * @return Wrapper runnable.
	 */
	public static Runnable wrap(Runnable action) {
		return () -> {
			try {
				action.run();
			} catch (Throwable t) {
				logger.error("Unhandled exception on thread: " + Thread.currentThread().getName(), t);
			}
		};
	}

	/**
	 * Shutdowns executors.
	 */
	public static void shutdown() {
		logger.trace("Shutting misc executors");
		scheduledService.shutdown();
	}
}
