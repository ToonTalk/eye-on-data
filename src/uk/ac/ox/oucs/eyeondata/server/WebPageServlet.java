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
	return evaluate(instruction.trim(), bindings);
    }

    private String[] evaluate(String expression, HashMap<String, String> bindings) {
	String result[] = new String[2];
	String[] parts;
	Double[] numbers;
	parts = expression.split("\\+");
	if (parts.length > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result);
	    if (numbers[0] != null) {
		Double sum = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
		    if (numbers[i] != null) {
			sum += numbers[i];
		    }			
		}
		result[0] = Double.toString(sum);
	    }
	    return result;
	}
	parts = expression.split("\\-");
	if (parts.length > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result);
	    if (numbers[0] != null) {
		Double difference = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
		    if (numbers[i] != null) {
			difference -= numbers[i];
		    }			
		}
		result[0] = Double.toString(difference);
	    }
	    return result;
	}
	parts = expression.split("\\*");
	if (parts.length > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result);
	    if (numbers[0] != null) {
		Double product = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
		    if (numbers[i] != null) {
			product *= numbers[i];
		    }			
		}
		result[0] = Double.toString(product);
	    }
	    return result;
	}
	parts = expression.split("/");
	if (parts.length > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result);
	    if (numbers[0] != null) {
		Double dividend = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
		    if (numbers[i] != null) {
			if (numbers[i] != 0.0) {
			    dividend /= numbers[i];
			} else {
			    if (result[1] == null) {
				result[1] = "";
			    }
			    result[0] += "Unable to divide by zero in " + expression;
			}
		    }			
		}
		result[0] = Double.toString(dividend);
	    }
	    return result;
	}
	String value = bindings.get(expression);
	if (value != null) {
	    result[0] = value;
	} else {
	    // can be either a double or long
	    try {
		Double.parseDouble(expression);
		result[0] = expression;
	    } catch (NumberFormatException e) {
		if (result[1] == null) {
		    result[1] = "";
		}
		result[1] += " The following is neither a number nor the value of a variable: " + expression;
	    }
	}
	return result;
    }

    private Double[] evaluateAllToNumbers(String[] parts, HashMap<String, String> bindings, String[] result) {
	Double[] numbers = new Double[parts.length];
	for (int i = 0; i < parts.length; i++) {
	    String[] evaluation = evaluate(parts[i], bindings);
	    if (evaluation[0] != null) {
		try {
		    numbers[i] = Double.parseDouble(evaluation[0]);
		} catch (NumberFormatException e) {
		    if (result[1] == null) {
			result[1] = "";
		    }
		    result[1] += " The following is not a number: " + evaluation[0] + " while computing " + parts[i];
		    // null value is left in numbers[i]
		}
	    } 
	    if (evaluation[1] != null) {
		if (result[1] == null) {
		    result[1] = "";
		}
		result[1] += evaluation[1];
	    }
	}
	return numbers;
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
