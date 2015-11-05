package org.irenical.shifty;

import com.google.common.base.Supplier;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class ShiftyTest {

  @Test(expected = ShiftyException.class)
  public void testNoConnector() throws Exception {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(null);
    shifty.call(new ShiftyMethod<MyUnstableApi, String, Exception>() {
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
  
  @Test
  public void testSlowCall() throws InterruptedException, ExecutionException {
    Shifty<MyUnstableApi> shifty = new Shifty<MyUnstableApi>(new Supplier<MyUnstableApi>() {
      @Override
      public MyUnstableApi get() {
        return new MyUnstableApi(3000);
      }
    });

    ShiftyCall<MyUnstableApi,String> call = shifty.withTimeout(1000);
    call.call(new ShiftyMethod<MyUnstableApi, String, InterruptedException>() {
      @Override
      public String apply(MyUnstableApi api) throws InterruptedException {
        return api.mySlowRemoteMethod(9001);
      }
    });
  }
  
}
