package com.pervasivecode.utils.concurrent.chute;

/**
 * A Chute is a closable conduit between producers of elements and consumers of objects of a given
 * type.
 * 
 * A Chute provides put, take, and close operations. After being closed, a Chute will not accept new
 * elements, but will allow consumers to take all of the remaining elements.
 * 
 * @param <E> The type of object that can be sent through the chute.
 */
public interface Chute<E> extends ChuteEntrance<E>, ChuteExit<E> {
  // This interface is just composed of ChuteEntrance and ChuteExit.
}
