package org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath;

import java.util.HashMap;
import java.util.Map;

/**
 * Register algorithm
 *
 * FIXME: is there already a facility in Eclipse to replace this class?
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class AlgorithmManager {

    private static AlgorithmManager self;
    private Map<String, Class<? extends ICriticalPathAlgorithm>> map;

    private AlgorithmManager() {
        map = new HashMap<>();
    }

    /**
     * Get the singleton instance
     *
     * @return the instance
     */
    public static AlgorithmManager getInstance() {
        if (null == self) {
            self = new AlgorithmManager();
            self.register(CriticalPathAlgorithmBounded.class);
            //self.register(CriticalPathAlgorithmUnbounded.class); // FIXME: this algorithm does not work properly
            self.register(ConnectedPathAlgorithm.class);
        }
        return self;
    }

    /**
     * Register a type in the manager
     *
     * @param type the class to register
     */
    public void register(Class<? extends ICriticalPathAlgorithm> type) {
        map.put(type.getSimpleName(), type);
    }

    /**
     * Return registered types
     * @return the types
     */
    public Map<String, Class<? extends ICriticalPathAlgorithm>> registeredTypes() {
        return map;
    }

}
