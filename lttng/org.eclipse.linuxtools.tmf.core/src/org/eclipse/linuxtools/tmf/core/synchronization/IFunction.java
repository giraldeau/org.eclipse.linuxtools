package org.eclipse.linuxtools.tmf.core.synchronization;

/**
 * @since 4.0
 */
public interface IFunction<T> {

    public void apply(T object);

}
