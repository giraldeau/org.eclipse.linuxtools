package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import com.google.common.collect.HashBasedTable;

/**
 * POJO object to hold benchmark context
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class BenchContext {

    public static final String GLOBAL = "__global__";
    public static final String TAG_TASK_NAME = "task";
    public static final String TAG_REPEAT = "repeat";
    public static final String TAG_SIZE = "size";

    HashBasedTable<Class<?>, String, Object> fProperties = HashBasedTable.create();

    public <T> T put(Class<T> klass, Object obj) {
        return klass.cast(fProperties.put(klass, GLOBAL, obj));
    }

    public <T> T get(Class<T> klass) {
        return klass.cast(fProperties.get(klass, GLOBAL));
    }

    public <T> T put(Class<T> klass, String name, Object obj) {
        return klass.cast(fProperties.put(klass, name, obj));
    }

    public <T> T get(Class<T> klass, String name) {
        return klass.cast(fProperties.get(klass, name));
    }

}
