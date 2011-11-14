/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import uk.ac.ox.oucs.eyeondata.server.objectify.DAO;

/**
 * @author Ken Kahn
 *
 */

public class ServerUtilities {
    
    static private DAO dao = new DAO();
    
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
    
    public static void persistObject(Object object) {
	getDao().persistObject(object);	
    }
    
    public static DAO getDao() {
        return dao;
    }
    
    public static String[] fetchURLContents(String urlString, HttpServletRequest request) {
	// returns String[2] where [0] is the contents of the url
	// and [1] is any warnings or error messages to be sent to the client
	urlString = urlString.replaceAll("&amp;", "&");
	String result[] = new String[2];
	try {
	    URL url = new URL(urlString);
	    URLConnection connection = url.openConnection();
	    // set timeout to GAE of 10 seconds
	    connection.setConnectTimeout(10000); 
	    connection.setReadTimeout(10000);
	    String userAgent = request.getHeader("user-agent");
	    connection.setRequestProperty("User-Agent", userAgent);
	    InputStream inputStream = connection.getInputStream();
	    result[0] = inputStreamToString(inputStream);
	    if (result[0].contains("404 Not Found")) {
		// should be caught below and cache used
		result[1] = "404 Not found returned from " + urlString;		
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    result[1] = e.toString();
	}
	return result;
    }

    public static String inputStreamToString(InputStream inputStream)
	    throws IOException {
	BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
	StringBuilder contents = new StringBuilder();
	String line;
	while ((line = in.readLine()) != null) {
	    contents.append(line + "\r");
	}
	in.close();
	return contents.toString();
    }

}
