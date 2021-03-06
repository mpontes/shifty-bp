package org.irenical.shifty;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Shifty<API> {

  private ExecutorService executorService;

  private Supplier<API> supplier;

  public Shifty(Supplier<API> supplier) {
    setSupplier(supplier);
  }

  public void setSupplier(Supplier<API> supplier) {
    if (supplier == null) {
      throw new ShiftyException("This shifty instance has no provider. Set one before calling call() or async()");
    }
    this.supplier = supplier;
  }

  public Supplier<API> getSupplier() {
    return supplier;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public ExecutorService getExecutorService() {
    return executorService;
  }

  protected String getName() {
    return supplier.getClass().getName();
  }

  public <RETURN> ShiftyCall<API, RETURN> withAutoClose() {
    return new ShiftyCall<API, RETURN>(this, new ShiftyConfiguration<RETURN>()).withAutoClose();
  }

  public <RETURN> ShiftyCall<API, RETURN> withFallback(Supplier<RETURN> fallback) {
    return new ShiftyCall<API, RETURN>(this, new ShiftyConfiguration<RETURN>()).withFallback(fallback);
  }

  public <RETURN> ShiftyCall<API, RETURN> withTimeout(long timeoutMillis) {
    return new ShiftyCall<API, RETURN>(this, new ShiftyConfiguration<RETURN>()).withTimeout(timeoutMillis);
  }

  public <RETURN, ERROR extends Exception> RETURN call(ShiftyMethod<API, RETURN, ERROR> call) throws ERROR {
    return new ShiftyCall<API, RETURN>(this, new ShiftyConfiguration<RETURN>()).call(call);
  }

  public <RETURN, ERROR extends Exception> Future<RETURN> async(ShiftyMethod<API, RETURN, ERROR> call) throws ERROR {
    return new ShiftyCall<API, RETURN>(this, new ShiftyConfiguration<RETURN>()).async(call);
  }

  public void async(Predicate<API> call) {
    new ShiftyCall<API, Object>(this, new ShiftyConfiguration<Object>()).async(call);
  }

  protected API getAPI() {
    return supplier.get();
  }

  protected ExecutorService getExecutor() {
    if (executorService == null) {
      executorService = Executors.newCachedThreadPool();
    }
    return getExecutorService();
  }

}
