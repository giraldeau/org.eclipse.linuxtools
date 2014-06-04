package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;

/**
 * Type casting for getting field values
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class Field {

	/**
	 * Get long field
	 *
	 * @param event the event
	 * @param name the field name
	 * @return the long value
	 */
	public static Long getLong(ITmfEvent event, String name) {
		return (Long) event.getContent().getField(name).getValue();
	}

    /**
     * Get string field
     *
     * @param event the event
     * @param name the field name
     * @return the string value
     */
	public static String getString(ITmfEvent event, String name) {
		return (String) event.getContent().getField(name).getValue();
	}

	/**
     * Get float field
     *
     * @param event the event
     * @param name the field name
     * @return the float value
     */
	public static double getFloat(ITmfEvent event, String name) {
		return (Double) event.getContent().getField(name).getValue();
	}

    /**
     * Get string field with default value
     *
     * @param event the event
     * @param name the field name
     * @param def the default value to return if the field does not exists
     * @return the long value
     */
	public static String getOrDefault(ITmfEvent event, String name, String def) {
	    if (event.getContent().getField(name) == null) {
	        return def;
	    }
        return (String) event.getContent().getField(name).getValue();
    }
}
