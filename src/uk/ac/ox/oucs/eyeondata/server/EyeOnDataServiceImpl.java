package uk.ac.ox.oucs.eyeondata.server;

import uk.ac.ox.oucs.eyeondata.client.EyeOnDataService;
import uk.ac.ox.oucs.eyeondata.server.objectify.DAO;
import uk.ac.ox.oucs.eyeondata.server.objectify.PageId;
import uk.ac.ox.oucs.eyeondata.server.objectify.ReadOnlyPageId;
import uk.ac.ox.oucs.eyeondata.server.objectify.WebPage;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class EyeOnDataServiceImpl extends RemoteServiceServlet implements
	EyeOnDataService {

    public String[] saveWebPage(String html, String pageId) {
	String[] result = new String[3];
	// result[0] = read-write pageId
	// result[1] = read-only pageId if new (otherwise null)
	// result[2] = errors/warnings
	if (pageId == null) {
	    pageId = ServerUtilities.generateGUIDString();
	}
	result[0] = pageId;
	// not sure how this helps in this context
//	String safeHTML = escapeHTML(html);
	DAO dao = ServerUtilities.getDao();
	WebPage webPage = dao.getWebPage(pageId);
	if (webPage == null) {
	    webPage = new WebPage(pageId, html);
	    String readOnlyPageId = ServerUtilities.generateGUIDString();
	    ReadOnlyPageId readOnlyPageIdEntry = new ReadOnlyPageId(readOnlyPageId, pageId);
	    ServerUtilities.persistObject(readOnlyPageIdEntry);
	    PageId pageIdEntry = new PageId(readOnlyPageId, pageId);
	    ServerUtilities.persistObject(pageIdEntry);
	    result[1] = readOnlyPageId;
	} else {
	    webPage.setHtml(html);
	}
	ServerUtilities.persistObject(webPage);
	return result;
    }

    @Override
    public String[] fetchPreviousPageContents(String pageId) {
	String[] result = new String[3];
	// result[0] = page contents
	// result[1] = readOnlyPageId
	// result[2] = errors/warnings
	DAO dao = ServerUtilities.getDao();
	WebPage webPage = dao.getWebPage(pageId);
	if (webPage == null) {
	    result[2] = "Unable to find the contents of a page with id: " + pageId;
	} else {
	    result[0] = webPage.getHtml();
	    result[1] = dao.getReadOnlyPageId(pageId);
	}
	return result;
    }

    /**
     * Escape an html string. Escaping data received from the client helps to
     * prevent cross-site script vulnerabilities.
     * 
     * @param html the html string to escape
     * @return the escaped string
     */
//    private String escapeHTML(String html) {
//	if (html == null) {
//	    return null;
//	}
//	return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
//    }
    
}
