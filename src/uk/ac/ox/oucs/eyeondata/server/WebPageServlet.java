/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server;

import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ox.oucs.eyeondata.server.objectify.DAO;
import uk.ac.ox.oucs.eyeondata.server.objectify.WebPage;

/**
 * Implements a servlet for responding to page requests
 * 
 * @author Ken Kahn
 *
 */
public class WebPageServlet extends HttpServlet {
    
    private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
              throws ServletException, java.io.IOException {
	response.setContentType(HTML_CONTENT_TYPE);
	PrintWriter writer = response.getWriter();
	String pathInfo = request.getPathInfo();
	pathInfo = pathInfo.substring(1); // strip off the initial /
	int extensionStart = pathInfo.indexOf(".html");
	if (extensionStart > 0) {
	    String readOnlyPageId = pathInfo.substring(0, extensionStart);
	    DAO dao = ServerUtilities.getDao();
	    String pageId = dao.getPageId(readOnlyPageId);
	    if (pageId == null) {
		writer.print("<html><body><p>Could not find a web page with id: " + readOnlyPageId + "</p></body></html>");
		return;
	    }
	    WebPage webPage = dao.getWebPage(pageId);
	    if (webPage == null) {
		writer.print("<html><body><p>Id known but no web page found. id: " + readOnlyPageId + "</p></body></html>");
		// TODO: add this to server logs
		return;
	    }
	    writer.print("<html><body>" + webPage.getHtml() + "</p></body></html>");
	} else {
	    writer.print("<html><body><p>Unable to process. URL does not end with .html: " + pathInfo + "</p></body></html>");
	}
    }

}
