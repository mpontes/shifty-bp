package org.irenical.shifty;

import com.google.common.base.Supplier;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ShiftyTest {

  @Test(expected = ShiftyException.class)
  public void testNoConnector() {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(null);
    shifty.call(new ShiftyMethod<MyUnstableApi, String, RuntimeException>() {
      @Override
      public String apply(MyUnstableApi api) {
        return api.myRemoteMethod(9001);
      }
    });
  }

  @Test
  public void testSimpleCall() throws InterruptedException, ExecutionException {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(3000);
      }
    });

    String got = shifty.call(new ShiftyMethod<MyUnstableApi, String, ExecutionException>() {
      @Override
      public String apply(MyUnstableApi api) {
        return api.myRemoteMethod(9001);
      }
    });
    Assert.assertEquals(got, "9001");
  }

  @Test(expected = TimeoutException.class)
  public void testSlowCall() {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(3000);
      }
    });

    ShiftyCall<MyUnstableApi, String> call = shifty.withTimeout(1000);
    call.call(new ShiftyMethod<MyUnstableApi, String, RuntimeException>() {
      @Override
      public String apply(MyUnstableApi api) {
        return api.mySlowRemoteMethod(9001);
      }
    });
  }

  @Test(expected = ContactSystemAdministratorException.class)
  public void testBrokenCall() throws ContactSystemAdministratorException {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(3000);
      }
    });
    shifty.call(new ShiftyMethod<MyUnstableApi, Object, ContactSystemAdministratorException>() {
      @Override
      public Object apply(MyUnstableApi api) throws ContactSystemAdministratorException {
        return api.myBrokenRemoteMethod(9001);
      }
    });
  }

  @Test
  public void testBrokenCallFallback() throws ContactSystemAdministratorException {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(3000);
      }
    });
    String got = shifty.withFallback(new Supplier<String>() {
      @Override
      public String get() {
        return "8999";
      }
    }).call(new ShiftyMethod<MyUnstableApi, String, ContactSystemAdministratorException>() {
      @Override
      public String apply(MyUnstableApi api) throws ContactSystemAdministratorException {
        return api.myBrokenRemoteMethod(9001);
      }
    });
    Assert.assertEquals(got, "8999");
  }

  @Test
  public void testSlowCallFallback() throws ContactSystemAdministratorException {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(3000);
      }
    });

    String got = shifty.withFallback(new Supplier<String>() {
      @Override
      public String get() {
        return "8999";
      }
    }).withTimeout(1000).call(new ShiftyMethod<MyUnstableApi, String, RuntimeException>() {
      @Override
      public String apply(MyUnstableApi api) {
        return api.mySlowRemoteMethod(9001);
      }
    });

    Assert.assertEquals(got, "8999");
  }

  @Test
  public void testSlowCallWentOK() throws ContactSystemAdministratorException {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(100);
      }
    });
    String got = shifty.withFallback(new Supplier<String>() {
      @Override
      public String get() {
        return "8999";
      }
    }).withTimeout(1000).call(new ShiftyMethod<MyUnstableApi, String, RuntimeException>() {
      @Override
      public String apply(MyUnstableApi api) {
        return api.mySlowRemoteMethod(9001);
      }
    });
    Assert.assertEquals(got, "9001");
  }

  @Test
  public void testAutoClose() {
    final MyAutoCloseableUnstableApi myApi = new MyAutoCloseableUnstableApi(100);
    Shifty<MyAutoCloseableUnstableApi> shifty = new Shifty<MyAutoCloseableUnstableApi>(new Supplier<MyAutoCloseableUnstableApi>() {
      @Override
      public MyAutoCloseableUnstableApi get() {
        return myApi;
      }
    });
    ShiftyCall<MyAutoCloseableUnstableApi, String> call = shifty.withAutoClose();
    String got = call.call(new ShiftyMethod<MyAutoCloseableUnstableApi, String, RuntimeException>() {
      @Override
      public String apply(MyAutoCloseableUnstableApi api) {
        Assert.assertTrue(myApi.isOpen());
        return api.myRemoteMethod(9001);
      }
    });
    Assert.assertEquals(got, "9001");
    Assert.assertFalse(myApi.isOpen());
  }

  @Test(expected = RuntimeException.class)
  public void testInvalidAutoClose() {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(100);
      }
    });
    ShiftyCall<MyUnstableApi, String> call = shifty.withAutoClose();
    call.call(new ShiftyMethod<MyUnstableApi, String, RuntimeException>() {
      @Override
      public String apply(MyUnstableApi api) {
        return api.myRemoteMethod(9001);
      }
    });
  }

}
