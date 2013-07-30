package ghostbuster;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Java reference utility methods.
 *
 * @author Bob Lee (bob@squareup.com)
 */
public class References {

  private References() {}

  /**
   * Creates a new reachability monitor that runs cleanup commands in the caller's thread when the
   * client registers new cleanup commands. This should only be used in environments where the
   * caller isn't allowed to spawn background threads.
   */
  public static ReachabilityMonitor foregroundMonitor() {
    return new ForegroundReachabilityMonitor();
  }

  /**
   * Convenience method, equivalent to {@code backgroundWatcher(1)}.
   */
  public static ReachabilityMonitor backgroundMonitor() {
    return backgroundMonitor(1);
  }

  /**
   * Creates a new reachability monitor that runs cleanup commands in the specified number of
   * background threads.
   */
  public static ReachabilityMonitor backgroundMonitor(int workers) {
    return new BackgroundReachabilityMonitor(workers);
  }

  /**
   * Creates a strong reference to the given referent. This reference is equivalent to a normal
   * object reference and is useful in code that supports different reference strengths,
   * including strong references.
   *
   * <p>The returned reference overrides {@code equals()} and {@code hashCode()}. If two strong
   * references point to the exact same referent, they will return the same hash code, and
   * {@code equals()} will return {@code true} when you compare the reference objects to each
   * other.</p>
   *
   * @throws NullPointerException if referent is null
   */
  public static <T> Reference<T> strongReference(final T referent) {
    if (referent == null) throw new NullPointerException("referent");
    return new StrongReference<T>(referent);
  }

  /**
   * The Java references API doesn't support strong references natively. We hack around this
   * limitation using a WeakReference. Because we keep a strong reference to the referent,
   * the garbage collector will never clear the weak reference.
   */
  private static class StrongReference<T> extends WeakReference<T> {

    private T referent;

    private StrongReference(T referent) {
      super(referent);
      this.referent = referent;
    }

    @Override public void clear() {
      super.clear();
      this.referent = null;
    }

    @Override public int hashCode() {
      return System.identityHashCode(referent);
    }

    @Override public boolean equals(Object obj) {
      return super.equals(obj);
    }
  }
}
