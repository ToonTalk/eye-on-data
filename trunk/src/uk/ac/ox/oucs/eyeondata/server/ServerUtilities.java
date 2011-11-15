/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import uk.ac.ox.oucs.eyeondata.server.objectify.DAO;

/**
 * @author Ken Kahn
 *
 */

public class ServerUtilities {
    
    private static DAO dao = new DAO();
    
    private final static String codesForUUID = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";
    
    private final static byte[] non_breaking_space_regular_expression_bytes = {'[', (byte) 194, (byte) 160, ']'};
    private static final String NON_BREAKING_SPACE_REGULAR_EXPRESSION = new String(non_breaking_space_regular_expression_bytes);

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
    
    static public String removeHTMLMarkup(String html) {
	if (html == null) {
	    return html;
	} else {
	    String innerText = getInnerText(html);
	    // now replace quotes, >, and <
	    return removeHTMLTokens(innerText);
	}
    }
    
    static public String getInnerText(String html) {
	// removes everything between < and > at any level
	// E.g. <pre>foo<br><p>bar</p></pre> returns
	// foo bar
	// really find stuff between > and <
	// note that removeHTMLMarkup calls this and also
	// translates &nbsp; and the like
	StringBuilder text = new StringBuilder();
	int firstStart = html.indexOf('<');
	if (firstStart < 0) {
	    return html; // there were no tags
	}
	if (firstStart > 0) {
	    text.append(html.substring(0,firstStart));	    
	}
	int endTag;
	int start = 0;
	while ((endTag = html.indexOf('>', start)) >= 0) {
	    int startTag = html.indexOf('<', endTag);
	    if (startTag < 0) {
		text.append(html.substring(endTag + 1));
//		System.out.println(html + " doesn't have matching tags.");
		break;
	    }
	    text.append(html.substring(endTag + 1, startTag));
	    text.append(' ');
	    start = startTag;
	};
	return text.toString();
    }

    public static String removeHTMLTokens(String code) {
	return replaceNonBreakingSpaces(
		code.replaceAll("&nbsp;", " ")
	            .replaceAll("&quot;", "\"")
	            .replaceAll("&lt;", "<")
	            .replaceAll("&gt;", ">")
	            .replaceAll("\r","\r\n")
	            .replaceAll("&amp;", "&")
	            // replace non-breaking spaces with spaces
	            //	           .replace((char) 160, (char) 32) // happens on the Mac when images are part of the HTML
	            .replaceAll("\r\n\r\n","\r\n")) // remove blank lines
	            .trim();
    }

    public static String replaceNonBreakingSpaces(String s) {
	return s.replaceAll(NON_BREAKING_SPACE_REGULAR_EXPRESSION, " ");
    }

    public static boolean isURL(String expression) {
	// could this be better
	return expression.contains("://");
    }
}
