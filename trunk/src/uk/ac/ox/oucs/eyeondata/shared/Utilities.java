package uk.ac.ox.oucs.eyeondata.shared;

/**
 * Utilities common to client and server
 * 
 * @author Ken Kahn
 *
 */
public class Utilities {

    public static boolean isURL(String expression) {
        // could this be better
        return expression.contains("://");
    }

}
