package org.irenical.shifty;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ShiftyCall<API, RETURN> {

  private static final Class<?> AUTOCLOSEABLE_CLASS;
  private static final Method AUTOCLOSEABLE_CLOSE;

  static {
    Class<?> autoCloseable;
    Method closeMethod;
    try {
      autoCloseable = Class.forName("java.lang.AutoCloseable");
      closeMethod = autoCloseable.getMethod("close");
    } catch (ClassNotFoundException e) {
      autoCloseable = null;
      closeMethod = null;
    } catch (NoSuchMethodException e) {
      autoCloseable = null;
      closeMethod = null;
    }

    AUTOCLOSEABLE_CLASS = autoCloseable;
    AUTOCLOSEABLE_CLOSE = closeMethod;
  }


  private Shifty<API> shifty;

  private ShiftyConfiguration<RETURN> conf;

  protected ShiftyCall(Shifty<API> shifty, ShiftyConfiguration<RETURN> conf) {
    this.conf = conf;
    this.shifty = shifty;
  }

  public ShiftyCall<API, RETURN> withFallback(Supplier<RETURN> fallback) {
    ShiftyConfiguration<RETURN> conf = new ShiftyConfiguration<RETURN>(this.conf);
    conf.setFallback(fallback);
    return new ShiftyCall<API, RETURN>(shifty, conf);
  }

  public ShiftyCall<API, RETURN> withTimeout(long timeoutMillis) {
    ShiftyConfiguration<RETURN> conf = new ShiftyConfiguration<RETURN>(this.conf);
    conf.setTimeoutMillis(timeoutMillis);
    return new ShiftyCall<API, RETURN>(shifty, conf);
  }

  public ShiftyCall<API, RETURN> withAutoClose() {
    ShiftyConfiguration<RETURN> conf = new ShiftyConfiguration<RETURN>(this.conf);
    conf.setAutoClose(true);
    return new ShiftyCall<API, RETURN>(shifty, conf);
  }

  @SuppressWarnings("unchecked")
  public <ERROR extends Exception> RETURN call(ShiftyMethod<API, RETURN, ERROR> call) throws ERROR {
    try {
      return getHystrixCommand(call).execute();
    } catch (RuntimeException e) {
      if (e instanceof HystrixBadRequestException || e instanceof HystrixRuntimeException) {
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        } else if (e.getCause() != null) {
          throw (ERROR) e.getCause();
        }
      }

      throw e;
    }
  }

  private <ERROR extends Exception> HystrixCommand<RETURN> getHystrixCommand(final ShiftyMethod<API, RETURN, ERROR> call) {
    int timeout = (int) conf.getTimeoutMillis();
    Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(shifty.getName()));
    if (timeout > 0) {
      setter = setter.andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutEnabled(true).withExecutionTimeoutInMilliseconds(timeout));
    }
    return new HystrixCommand<RETURN>(setter) {

      private volatile Exception blewUp;

      @Override
      protected RETURN run() throws Exception {
        API got = shifty.getAPI();
        try {
          return call.apply(got);
        } catch (Exception e) {
          blewUp = e;
          throw e;
        } finally {
          if (conf.isAutoClose()) {
            autoClose(got);
          }
        }
      }

      @Override
      protected RETURN getFallback() {
        if (conf.getFallback() != null) {
          return conf.getFallback().get();
        } else if (blewUp instanceof RuntimeException) {
          throw (RuntimeException) blewUp;
        } else {
          throw new RuntimeException(blewUp);
        }
      }

      private void autoClose(API got) throws Exception {
        if (AUTOCLOSEABLE_CLASS == null) {
          throw new RuntimeException("AutoCloseable not supported for this JVM");
        }

        if (AUTOCLOSEABLE_CLASS.isAssignableFrom(got.getClass())) {
          AUTOCLOSEABLE_CLOSE.invoke(got);
        } else {
          throw new RuntimeException("Could not close instance of " + got.getClass() + ", as it does not implement AutoCloseable");
        }
      }
    };
  }

  public <ERROR extends Exception> Future<RETURN> async(final ShiftyMethod<API, RETURN, ERROR> call) {
    return shifty.getExecutor().submit(new Callable<RETURN>() {
      @Override
      public RETURN call() throws ERROR {
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
