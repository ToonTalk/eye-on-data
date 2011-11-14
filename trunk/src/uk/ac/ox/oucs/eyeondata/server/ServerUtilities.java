/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server;

import java.util.UUID;

/**
 * @author Ken Kahn
 *
 */
public class ServerUtilities {
    
final static String codesForUUID = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";
       
    public static String encodeUUID(UUID uuid) {
	StringBuilder encoding = new StringBuilder();
	long part1 = uuid.getLeastSignificantBits();
	long part2 = uuid.getMostSignificantBits();
	for (int i = 0; i < 11; i++) {
	    int index = (int) (0x3F & part1);
	    encoding.append(codesForUUID.charAt(index));
	    index = (int) (0x3F & part2);
	    encoding.append(codesForUUID.charAt(index));
	    part1 = (part1 >> 6);
	    part2 = (part2 >> 6);
	}
	return encoding.toString();
    }
    
    public static String generateGUIDString() {
	return encodeUUID(UUID.randomUUID());
    }

}
