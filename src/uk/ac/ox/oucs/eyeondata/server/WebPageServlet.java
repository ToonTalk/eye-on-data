/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server;

import java.io.PrintWriter;
import java.util.HashMap;

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
    
    private static final String SEPERATOR = "\\+\\+\\+";
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
	    String rawHTML = webPage.getHtml();
	    String[] processedHTML = processHTML(rawHTML, request);
	    String body = processedHTML[0];
	    if (processedHTML[1] != null) {
		body += "<br>Errors encountered. " + processedHTML[1];
	    }
	    writer.print("<html><body>" + body + "</p></body></html>");
	} else {
	    writer.print("<html><body><p>Unable to process. URL does not end with .html: " + pathInfo + "</p></body></html>");
	}
    }

    private String[] processHTML(String rawHTML, HttpServletRequest request) {
	// result[0] is part of the HTML (or null)
	// result[1] is an error or warnings
	HashMap<String, String> bindings = new HashMap<String, String>();
	String[] parts = rawHTML.split(SEPERATOR);
	String result[] = new String[2];
	result[0] = parts[0];
	for (int i = 1; i < parts.length; i++) {
	    if (i%2 == 1) {
		String[] processedInstruction = processInstruction(parts[i], bindings, request);
		if (processedInstruction[0] != null) {
		    result[0] += processedInstruction[0];
		}
		if (processedInstruction[1] != null) {
		    result[1] += " " + processedInstruction[1];
		}
	    } else {
		result[0] += parts[i];
	    }
	}
	return result;
    }

    private String[] processInstruction(String instruction, HashMap<String, String> bindings, HttpServletRequest request) {
	// result[0] is part of the HTML (or null)
	// result[1] is an error or warnings
	String result[] = new String[2];
	int equalIndex = instruction.indexOf('=');
	if (equalIndex >= 0) {
	    result[1] = processEquation(instruction.substring(0, equalIndex), instruction.substring(equalIndex+1), bindings, request);
	    return result;
	}
	return evaluate(instruction, bindings);
    }

    private String[] evaluate(String expression, HashMap<String, String> bindings) {
	String result[] = new String[2];
	String[] parts = expression.split("/");
	if (parts.length > 1) {
	    String[] numbers = evaluateAllToNumbers(parts, bindings, result);
	    // TODO: divide them (and deal with errors)
	}
	parts = expression.split("\\*");
	if (parts.length > 1) {
	    String[] numbers = evaluateAllToNumbers(parts, bindings, result);
	    // TODO: multiply them (and deal with errors)
	}
	parts = expression.split("\\+");
	if (parts.length > 1) {
	    String[] numbers = evaluateAllToNumbers(parts, bindings, result);
	    // TODO: add them (and deal with errors)
	}
	parts = expression.split("\\-");
	if (parts.length > 1) {
	    String[] numbers = evaluateAllToNumbers(parts, bindings, result);
	    // TODO: subtract them (and deal with errors)
	}
	String value = bindings.get(expression.trim());
	if (value != null) {
	    result[0] = value;
	} else {
	    result[1] += " No value was found for this: " + expression.trim();
	}
	return result;
    }

    private String[] evaluateAllToNumbers(String[] parts, HashMap<String, String> bindings, String[] result) {
	// TODO Auto-generated method stub
	return null;
    }

    private String processEquation(String left, String right, HashMap<String, String> bindings, HttpServletRequest request) {
	// return error/warnings or null if none
	String[] urlContents = ServerUtilities.fetchURLContents(right.trim(), request);
	if (urlContents[0] != null) {
	    matchDataAndVariables(left.trim(), urlContents[0], bindings);
	    return urlContents[1];
	} else {
	    return "No contents found for " + left + ". " + urlContents[1];
	}
    }

    private String matchDataAndVariables(String variables, String data, HashMap<String, String> bindings) {
	// returns warning/error or null
	String[] variableNames = variables.split(",");
	String[] dataValues = data.split(",");
	if (dataValues.length < 2) {
	    dataValues = data.split("\r");
	}
	if (dataValues.length < 2) {
	    dataValues = data.split("\n");
	}
	if (dataValues.length < 2) {
	    dataValues = data.split("\t");
	}
	if (variableNames.length > dataValues.length) {
	    return "More variables (" + variableNames.length + ") than data values (" + dataValues.length + ")";
	}
	for (int i = 0; i < variableNames.length; i++) {
	    bindings.put(variableNames[i].trim(), dataValues[i].trim());
	}
	return null;
    }

}
