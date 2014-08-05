package org.eclipse.linuxtools.tmf.core.event.matching;

import java.util.HashMap;

/**
 * Keep unique integer ID for an object. The ids are not reused.
 *
 * @author francis
 * @since 4.0
 *
 */
public class InternalizeMap {

    HashMap<Object, Integer> map = new HashMap<>();
    int nextId = 0;

    public int get(Object obj) {
        return map.get(obj);
    }

    public void put(Object obj) {
        if (!map.containsKey(obj)) {
            map.put(obj, nextId++);
        }
    }

    public Object remove(Object obj) {
        return map.remove(obj);
    }

    public int size() {
        return nextId;
    }

}
