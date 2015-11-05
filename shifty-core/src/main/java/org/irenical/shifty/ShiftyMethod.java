package org.irenical.shifty;

public interface ShiftyMethod<API, RETURN, ERROR extends Exception> {

  RETURN apply(API api) throws ERROR;

}
