package uk.ac.ox.oucs.eyeondata.server;

import uk.ac.ox.oucs.eyeondata.client.EyeOnDataService;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class EyeOnDataServiceImpl extends RemoteServiceServlet implements
	EyeOnDataService {

    public String[] saveWebPage(String html, String pageId) throws IllegalArgumentException {
	String[] result = new String[2];
	if (pageId == null) {
	    pageId = ServerUtilities.generateGUIDString();
	}
	result[0] = pageId;
	return result;
    }

    /**
     * Escape an html string. Escaping data received from the client helps to
     * prevent cross-site script vulnerabilities.
     * 
     * @param html the html string to escape
     * @return the escaped string
     */
    private String escapeHtml(String html) {
	if (html == null) {
	    return null;
	}
	return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }
}