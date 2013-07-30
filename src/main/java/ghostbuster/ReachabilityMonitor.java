package ghostbuster;

import java.lang.ref.Reference;

/**
 * Monitors the reachability of objects and invokes cleanup commands after those object become
 * either weakly reachable or completely unreachable, depending on the caller's preference.
 *
 * <p>Callers should keep a strong reference to this monitor so long as they wish for cleanup
 * commands to run. If the monitor itself is reclaimed by the garbage collector, the monitor will
 * shut down, and cleanup commands will no longer run.</p>
 *
 * <h3>Weakly Reachable vs. Unreachable</h3>
 *
 * <p>Weak and phantom (unreachable) references are very similar, and it isn't immediately obvious
 * when one type of reference should be used over the other. Java supports both types instead of
 * just one for one simple reason: finalizers.</p>
 *
 * <p>Finalizers existed before references, and they had significant design flaws and drawbacks.
 * References are meant to supplant finalizers, but they also need to co-exist with finalizers.
 * Therefore, we have two types of references, one that the garbage collector enqueues before
 * finalizers run (weak) and one after (phantom).</p>
 *
 * <p>When should you use one vs. the other? In a nutshell, triggering cleanup immediately after an
 * object becomes weakly reachable is fast and reliable. Waiting for complete unreachability,
 * after the finalizers have run, is more secure but it also suffers some of the same drawbacks as
 * finalizers (but not all).</p>
 *
 * <p>If the caller requests cleanup when an object is weakly reachable, i.e. after no strong or
 * soft references remain, the cleanup command may run before, during, or after Java's built-in
 * finalization. Therefore, callers should ensure that their object behaves gracefully if other
 * objects access it after cleanup. If a Java finalizer resurrects the referent––by creating a new
 * strong reference to it––the cleanup command will run just once.</p>
 *
 * <p>If the caller requests that cleanup occur only after an object is completely unreachable,
 * the cleanup command will run after Java's finalizer, and there will be no possibility of
 * post-cleanup access or resurrection. This means callers can safely do things like free native
 * memory without worrying about other objects accessing it afterwords.</p>
 *
 * <p>The main problem with waiting until an object becomes completely unreachable is we must
 * wait until Java finalization completes first, and finalization has fundamental flaws. Namely,
 * a finalizer may run late or never, which in turn means this monitor may run cleanup late or
 * never.</p>
 *
 * <p>For example, a completely unrelated object could include code in its
 * {@link Object#finalize()} method that blocks the finalizer thread indefinitely. If that object
 * or a different object that's enqueued for finalization afterwords references your object,
 * your object's cleanup command may never run, and your application may run out of memory.</p>
 *
 * <h3>Reference Objects</h3>
 *
 * <p>In cases where the monitor returns a reference object to the caller, cleanup will run only if
 * the caller keeps a strong reference to the reference object. If the caller dereferences a
 * reference object, the cleanup command may or may not run (depending on the garbage collector).
 * The caller should be prepared for both possibilities.</p>
 *
 * <p>This feature is useful in cases where cleanup may not need to take place. For example,
 * if you have a collection of references and you clean up the collection's internal state after
 * a reference is cleared, this cleanup might not be necessary if the collection itself is no
 * longer reachable.</p>
 *
 * <p>The references returned by this monitor override {@code equals()} and {@code hashCode()}.
 * If two references are of the same type and point to the exact same referent, they will return
 * the same hash code, and {@code equals()} will return {@code true} when you compare the reference
 * objects to each other.</p>
 *
 * @see References for creation methods
 *
 * @author Bob Lee (bob@squareup.com)
 */
public interface ReachabilityMonitor {

  /**
   * Registers a cleanup command to run after no strong or soft references to the given object
   * remain.
   *
   * @throws NullPointerException is o or cleanup is null
   */
  public void whenWeaklyReachable(Object o, Runnable cleanup);

  /**
   * Registers a cleanup command to run after no references of any kind to the given object
   * remain.
   *
   * @throws NullPointerException is o or cleanup is null
   */
  public void whenUnreachable(Object o, Runnable cleanup);

  /**
   * Creates a <i>weak</i> reference to the given object and registers a cleanup command to run
   * after no strong or soft references to the object remain. If the reference object is
   * reclaimed, the cleanup command will not run.
   *
   * @throws NullPointerException is referent or cleanup is null
   */
  public <T> Reference<T> weakReference(T referent, Runnable cleanup);

  /**
   * Creates a <i>phantom</i> reference to the given object and registers a cleanup command to run
   * after no references of any kind to the object remain. If the reference object is
   * reclaimed, the cleanup command will not run.
   *
   * <p>In contrast to normal phantom references, the {@code get()} method on this phantom
   * reference will return the referent (instead of {@code null}) so long as the referent is still
   * strongly or softly reachable. Also, this monitor will automatically clear the
   * phantom reference before running the cleanup command.</p>
   *
   * @throws NullPointerException is referent or cleanup is null
   */
  public <T> Reference<T> phantomReference(T referent, Runnable cleanup);

  /**
   * Requests that this monitor shut itself down. If the monitor shuts down, cleanup commands will
   * no longer run. This method is optional to call and to implement. A monitor should shut down
   * automatically if it is no longer strongly referenced itself. Requesting shutdown may
   * accelerate this process (shutting down threads without waiting on the garbage collector,
   * for example).
   */
  public void requestShutdown();
}
