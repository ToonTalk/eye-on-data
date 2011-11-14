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
    
    private enum Relation { GREATER_THAN, LESS_THAN, EQUAL, NOT_EQUAL, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL};
    
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
		addError(processedInstruction[1], result);
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
	int ifIndex = instruction.indexOf("if");
	if (ifIndex >= 0) {
	    int thenIndex = instruction.indexOf("then", ifIndex);
	    if (thenIndex > 0) {
		String condition = instruction.substring(ifIndex+2, thenIndex);
		String conditionResult[] = evaluateCondition(condition, bindings);
		addError(conditionResult[1], result);
		String trueBranch;
		String falseBranch = null;
		int elseIndex = instruction.indexOf("else", thenIndex);
		if (elseIndex < 0) {
		    trueBranch = instruction.substring(thenIndex+4);
		} else {
		    trueBranch = instruction.substring(thenIndex+4, elseIndex);
		    falseBranch = instruction.substring(elseIndex+4);
		}
		if ("true".equals(conditionResult[0])) {
		   return processInstruction(trueBranch, bindings, request);
		} else if ("false".equals(conditionResult[0])) {
		    if (falseBranch != null) {
			return processInstruction(falseBranch, bindings, request);
		    } else {
			return result;
		    }
		} else {
		    addError("Condition did not evaluate to true or false: " + condition, result);
		    return result;
		}
	    } else {
		    addError("Found <i>if</i> but not <i>then</i>: " + instruction, result);
		    return result;
	    }
	}
	String imageHTML = null;
	int imageIndex = instruction.indexOf("<img src=");
	if (imageIndex >= 0) {
	    int closeImageIndex = instruction.indexOf(">", imageIndex);
	    if (closeImageIndex > 0) {
		imageHTML = instruction.substring(0, closeImageIndex+1);
		if (imageHTML != null) {
		    String remainingInstructions = instruction.substring(closeImageIndex+1);
		    String[] equationParts = remainingInstructions.split("=");
		    if (equationParts.length > 1) {
			// alternates between name value-and-next-name
			String variableName = null;
			for (int i = 0; i < equationParts.length; i++) {
			    String part = equationParts[i].trim();
			    if (i == 0) {
				variableName = part;
			    } else {
				int lastSpaceIndex = part.lastIndexOf(' ');
				String[] value;
				if (lastSpaceIndex >= 0 && i+1 < equationParts.length) {
				    // space in the last one doesn't matter
				    String valueExpression = part.substring(0, lastSpaceIndex);
				    value = evaluate(valueExpression, bindings);
				    variableName = part.substring(lastSpaceIndex+1);
				} else {
				    value = evaluate(part, bindings);  
				}
				if (value[0] != null) {
				    bindings.put(variableName, value[0]);
				}
				addError(value[1], result);
			    }
			}
		    }
		    String width = bindings.get("width");
		    String height = bindings.get("height");
		    if (width != null) {
			imageHTML = addAttribute(imageHTML, "width", width);
			bindings.remove("width");
		    }
		    if (height != null) {
			imageHTML = addAttribute(imageHTML, "height", height);
			bindings.remove("height");
		    }
		    result[0] = imageHTML;
		    return result;
		}
	    }
	}
	// for now spaces on both sides since HTML could have = inside of elements
	// but with some effort could ignore those
	int equalIndex = instruction.indexOf(" = "); 
	if (equalIndex >= 0) {
	    // +3 to skip over " = "
	    result[1] = processEquation(instruction.substring(0, equalIndex).trim(), instruction.substring(equalIndex+3).trim(), bindings, request);
	    return result;
	}
	return evaluate(instruction.trim(), bindings);
    }

    private void addError(String errorMessage, String[] result) {
	if (errorMessage == null) {
	    return;
	}
	if (result[1] == null) {
	    result[1] = errorMessage;
	} else {
	    result[1] += " " + errorMessage;
	}
    }

    private String addAttribute(String html, String attribute, String value) {
	int attributeIndex = html.indexOf(attribute + "=");
	if (attributeIndex < 0) {
	    return html.substring(0, html.length()-1) + " " + attribute + "=" + value + ">";
	} else {
	    return html.substring(0, attributeIndex) + value + ">";
	}
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
	// Don't pick up </element ...>
	parts = expression.split(" / ");
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
		addError(" The following is neither a number nor the value of a variable: " + expression, result);
	    }
	}
	return result;
    }

    private Double[] evaluateAllToNumbers(String[] parts, HashMap<String, String> bindings, String[] result) {
	Double[] numbers = new Double[parts.length];
	for (int i = 0; i < parts.length; i++) {
	    numbers[i] = evaluateToNumber(parts[i].trim(), bindings, result);
	}
	return numbers;
    }
    
    private Double evaluateToNumber(String expression, HashMap<String, String> bindings, String[] result) {
	String[] evaluation = evaluate(expression, bindings);
	if (evaluation[0] != null) {
	    try {
		return Double.parseDouble(evaluation[0]);
	    } catch (NumberFormatException e) {
		addError(" The following is not a number: " + evaluation[0] + " while computing " + expression, result);
		// null value is left in numbers[i]
	    }
	} 
	addError(evaluation[1], result);
	return null;
    }
    
    private String[] evaluateCondition(String condition, HashMap<String, String> bindings) {
	// result[0] is either "true", "false", or null
	// result[1] any errors
	String result[] = new String[2];
	String[] parts;
	Relation relation;
	parts = condition.split("&gt;=", 2);
	if (parts.length == 2) {
	    relation = Relation.GREATER_THAN_OR_EQUAL;
	} else {
	    parts = condition.split("&gt;", 2);
	    if (parts.length == 2) {
		relation = Relation.GREATER_THAN;
	    } else {
		parts = condition.split("&lt;=", 2);
		if (parts.length == 2) {
		    relation = Relation.LESS_THAN_OR_EQUAL;
		} else {
		    parts = condition.split("&lt;", 2);
		    if (parts.length == 2) {
			relation = Relation.LESS_THAN;
		    } else {
			parts = condition.split("=", 2);
			if (parts.length == 2) {
			    relation = Relation.EQUAL;
			} else {
			    parts = condition.split("!=", 2);
			    if (parts.length == 2) {
				relation = Relation.NOT_EQUAL;
			    } else {
				result[1] = "Between the <i>if</i> and the <i>then</i> did not find any of the following >, >=, <, <=, =, or != in " + condition;
				return result;
			    }
			}
		    }
		}
	    }
	}
	Double leftValue = evaluateToNumber(parts[0].trim(), bindings, result);
	Double rightValue = evaluateToNumber(parts[1].trim(), bindings, result);
	if (leftValue == null || rightValue == null) {
	    return result;
	}
	switch (relation) {
	case GREATER_THAN:
	    result[0] = Boolean.toString(leftValue>rightValue);
	    break;
	case GREATER_THAN_OR_EQUAL:
	    result[0] = Boolean.toString(leftValue>=rightValue);
	    break;
	case LESS_THAN:
	    result[0] = Boolean.toString(leftValue<rightValue);
	    break;
	case LESS_THAN_OR_EQUAL:
	    result[0] = Boolean.toString(leftValue<=rightValue);
	    break;
	case EQUAL:
	    result[0] = Boolean.toString(leftValue==rightValue);
	    break;
	case NOT_EQUAL:
	    result[0] = Boolean.toString(leftValue!=rightValue);
	    break;
	}
	return result;
    }

    private String processEquation(String left, String right, HashMap<String, String> bindings, HttpServletRequest request) {
	// return error/warnings or null if none
	try {
	    Double.parseDouble(right);
	    matchDataAndVariables(left, right, bindings);
	    return null;
	} catch (NumberFormatException e) {
	    String[] urlContents = ServerUtilities.fetchURLContents(right, request);
	    if (urlContents[0] != null) {
		matchDataAndVariables(left, urlContents[0], bindings);
		return urlContents[1];
	    } else {
		return "No contents found for " + left + ". " + urlContents[1];
	    }
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
