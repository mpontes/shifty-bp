package org.irenical.shifty;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class ShiftyCall<API, RETURN> {

  private Shifty<API> shifty;

  private ShiftyConfiguration<RETURN> conf;

  public ShiftyCall(Shifty<API> shifty, ShiftyConfiguration<RETURN> conf) {
    this.conf = conf;
    this.shifty = shifty;
  }

  public ShiftyCall<API, RETURN> withFallback(Supplier<RETURN> fallback) {
    ShiftyConfiguration<RETURN> conf = new ShiftyConfiguration<RETURN>(this.conf);
    conf.setFallback(fallback);
    return new ShiftyCall<>(shifty, conf);
  }

  public ShiftyCall<API, RETURN> withTimeout(long timeoutMillis) {
    ShiftyConfiguration<RETURN> conf = new ShiftyConfiguration<RETURN>(this.conf);
    conf.setTimeoutMillis(timeoutMillis);
    return new ShiftyCall<>(shifty, conf);
  }

  public <ERROR extends Exception> RETURN call(ShiftyMethod<API, RETURN, ERROR> call) throws ERROR {
    return getHystrixCommand(call).execute();
  }

  private <ERROR extends Exception> HystrixCommand<RETURN> getHystrixCommand(final ShiftyMethod<API, RETURN, ERROR> call) {
    int timeout = (int) conf.getTimeoutMillis();
    timeout = timeout == 0 ? 100000 : timeout;
    return new HystrixCommand<RETURN>(HystrixCommandGroupKey.Factory.asKey(shifty.getName()),timeout) {

      @Override
      protected RETURN run() throws Exception {
        API got = shifty.getAPI();
        return call.apply(got);
      }

      @Override
      protected RETURN getFallback() {
        if (conf.getFallback() != null) {
          return conf.getFallback().get();
        } else {
          return super.getFallback();
        }
      }
    };
  }

  public <ERROR extends Exception> Future<RETURN> async(final ShiftyMethod<API, RETURN, ERROR> call) {
    return shifty.getExecutor().submit(new Callable<RETURN>() {
      @Override
      public RETURN call() throws Exception {
        return call.apply(shifty.getAPI());
      }
    });
  }

  public void async(final Predicate<API> call) {
    shifty.getExecutor().execute(new Runnable() {
      @Override
      public void run() {
        call.apply(shifty.getAPI());
      }
    });
  }
}
