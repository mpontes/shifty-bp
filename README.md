# Shifty - Java 6+ backport

## What for?
If you have a shady Java object, holding a bunch of methods you don't trust, where anything can go wrong. Instead of trying to wrap method calls in a bunch of try/catch blocks, separate thread, some timeout mechanism and a retry logic, you can use Shifty.
Shifty uses Hystrix internally to handle some of the work, extending these features to class level (instead of method level). This is specially usefull for class defined remote API.

This is a backport of the Java 8+ shifty library for legacy clients. You should really use the regular version if you can. This version makes the syntax a lot more verbose due to replacing lambdas with anonymous inner classes and adds a dependency on Guava in order to replace funcional classes that were introduced with Java 8, such as Supplier.

## Usage
Say you have an object, an instance of the class MyUnstableApi, in some variable called myApi, and you don't trust it the least.

First wrap your object with Shifty
```java
Shifty<MyUnstableApi> shifty = new Shifty<>(new Supplier<MyUnstableApi>() {
  @Override
  public MyUnstableApi get() {
	return myApi;
  }
});
```
Notice a Supplier is passed on, not the actual instance. This allows you add complex logic, like service discovery or connection pooling to the retrieval of the actual API instance. Or simply dynamic instantiation, example bellow.
```java
Shifty<MyUnstableApi> shifty = new Shifty<>(new Supplier<MyUnstableApi>() {
  @Override
  public MyUnstableApi get() {
	return new MyUnstableApi();
  }
});
```

Now you have a shifty object prepared, you can now call arbitrary code over the instance retrieved by given supplier in the following way.
```java
String got = shifty.call(new ShiftyMethod<MyUnstableApi, String, ExecutionException>() {
  @Override
  public String apply(MyUnstableApi api) {
	return api.myRemoteMethod(9001);
  }
});
```
In this example, the API has a method that toStrings any integer... in some remote server. If something goes wrong, you will simply get an exception, as you would normally so pretty much nothing interesting happened so far. But now let's suppose you want to add a timeout rule to this call. You could write this instead.
```java
String got = shifty.withTimeout(1000).call(new ShiftyMethod<MyUnstableApi, String, ExecutionException>() {
  @Override
  public String apply(MyUnstableApi api) {
	return api.myRemoteMethod(9001);
  }
});
```
This call would give up after 1 second.

