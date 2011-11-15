/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server;

import java.io.PrintWriter;
import java.util.ArrayList;
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
    
    private static final String SEPERATOR_REGULAR_EXPRESSION = "\\+\\+\\+";
    private static final String IMAGE_TOKEN_SEPERATOR = "%%%";
    private static final int IMAGE_TOKEN_SEPERATOR_LENGTH = IMAGE_TOKEN_SEPERATOR.length();
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
	String[] parts = rawHTML.split(SEPERATOR_REGULAR_EXPRESSION);
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

    private String[] processInstruction(String instructionHTML, HashMap<String, String> bindings, HttpServletRequest request) {
	// result[0] is part of the HTML (or null)
	// result[1] is an error or warnings
	String result[] = new String[2];
	String instructionHTMLWithImageTokens = replaceImagesWithTokens(instructionHTML, bindings);
	String instruction = ServerUtilities.removeHTMLMarkup(instructionHTMLWithImageTokens);
	if (instruction.isEmpty()) {
	    return result;
	}
	String ifToken = "if ";
	int ifIndex = instruction.indexOf(ifToken);
	if (ifIndex >= 0) {
	    String thenToken = "then";
	    int thenIndex = instruction.indexOf(thenToken, ifIndex);
	    if (thenIndex > 0) {
		String condition = instruction.substring(ifIndex+ifToken.length(), thenIndex);
		String conditionResult[] = evaluateCondition(condition, bindings, request);
		addError(conditionResult[1], result);
		String trueBranch;
		String falseBranch = null;
		String elseToken = " else ";
		int elseTokenLength = elseToken.length();
		int elseIndex = instruction.indexOf(elseToken, thenIndex);
		if (elseIndex < 0) {
		    trueBranch = instruction.substring(thenIndex+thenToken.length());
		} else {
		    trueBranch = instruction.substring(thenIndex+elseTokenLength, elseIndex);
		    falseBranch = instruction.substring(elseIndex+elseTokenLength);
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
	int imageTokenIndex = instruction.indexOf(IMAGE_TOKEN_SEPERATOR);
	if (imageTokenIndex >= 0) {
	    int imageTokenStartIndex = imageTokenIndex+IMAGE_TOKEN_SEPERATOR_LENGTH;
	    int imageTokenEndIndex = instruction.indexOf(IMAGE_TOKEN_SEPERATOR, imageTokenStartIndex);
	    int equalIndex = instruction.indexOf("=");
	    if (equalIndex >= 0) {
		if (equalIndex > imageTokenStartIndex) {
		    // equations after the image
		    result[1] = processEquations(instruction.substring(imageTokenEndIndex+IMAGE_TOKEN_SEPERATOR_LENGTH, equalIndex).trim(), instruction.substring(equalIndex+1).trim(), bindings, request);
		} else {
		    result[1] = processEquations(instruction.substring(0, equalIndex).trim(), instruction.substring(equalIndex+1, imageTokenStartIndex).trim(), bindings, request);		    
		}
	    }
	    String imageToken = instruction.substring(imageTokenStartIndex, imageTokenEndIndex);
	    String imageHTML = bindings.get(imageToken);
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
	int equalIndex = instruction.indexOf("="); 
	if (equalIndex >= 0) {
	    result[1] = processEquations(instruction.substring(0, equalIndex).trim(), instruction.substring(equalIndex+1).trim(), bindings, request);
	    return result;
	}
	return evaluate(instruction.trim(), bindings, request);
    }
    
    /**
     * @param instructionHTML
     * @param bindings
     * @return the instructionHTML with IMG elements replaced by unique tokens
     * and the unique tokens added to bindings
     */
    private String replaceImagesWithTokens(String instructionHTML, HashMap<String, String> bindings) {
	String instructionWithTokens = "";
	int index = 0;
	int imageIndex;
	if ((imageIndex = instructionHTML.indexOf("<img src=", index)) >= 0) {
	    instructionWithTokens += instructionHTML.substring(index, imageIndex);
	    int closeImageIndex = instructionHTML.indexOf(">", imageIndex);
	    if (closeImageIndex > 0) {
		String imageHTML = instructionHTML.substring(imageIndex, closeImageIndex+1);
		String imageToken = ServerUtilities.generateGUIDString();
		bindings.put(imageToken, imageHTML);
		// temporarily add SEPERATOR tokens to for subsequent processing
		instructionWithTokens += IMAGE_TOKEN_SEPERATOR + imageToken + IMAGE_TOKEN_SEPERATOR;
		index = closeImageIndex+1;
//		if (imageHTML != null) {
//		    String remainingInstructionsHTML = instructionHTML.substring(closeImageIndex+1);
//		    String remainingInstructions = ServerUtilities.removeHTMLMarkup(remainingInstructionsHTML);
//		    String[] equationParts = remainingInstructions.split("=");
//		    if (equationParts.length > 1) {
//			// alternates between name value-and-next-name
//			String variableName = null;
//			for (int i = 0; i < equationParts.length; i++) {
//			    String part = equationParts[i].trim();
//			    if (i == 0) {
//				variableName = part;
//			    } else {
//				int lastSpaceIndex = part.lastIndexOf(' ');
//				String[] value;
//				if (lastSpaceIndex >= 0 && i+1 < equationParts.length) {
//				    // space in the last one doesn't matter
//				    String valueExpression = part.substring(0, lastSpaceIndex);
//				    value = evaluate(valueExpression, bindings);
//				    variableName = part.substring(lastSpaceIndex+1);
//				} else {
//				    value = evaluate(part, bindings);  
//				}
//				if (value[0] != null) {
//				    bindings.put(variableName, value[0]);
//				}
//				addError(value[1], result);
//			    }
//			}
//		    }
//		    String width = bindings.get("width");
//		    String height = bindings.get("height");
//		    if (width != null) {
//			imageHTML = addAttribute(imageHTML, "width", width);
//			bindings.remove("width");
//		    }
//		    if (height != null) {
//			imageHTML = addAttribute(imageHTML, "height", height);
//			bindings.remove("height");
//		    }
//		    result[0] = imageHTML;
//		    return result;
//		}
	    }
	}
	instructionWithTokens += instructionHTML.substring(index);
	return instructionWithTokens;
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

    private String[] evaluate(String expression, HashMap<String, String> bindings, HttpServletRequest request) {
	String result[] = new String[2];
	if (ServerUtilities.isURL(expression)) {
	    String[] urlContents = ServerUtilities.fetchURLContents(expression, request);
	    if (urlContents[0] != null) {
		toCSV(urlContents[0], result);
		return result;
	    } else {
		result[1] = "No contents found for " + expression + ". " + urlContents[1];
		return result;
	    }
	}
	int openParenIndex = expression.indexOf('(');
	if (openParenIndex >= 0) {
	    int closeParenIndex = expression.indexOf(')', openParenIndex);
	    if (closeParenIndex >= 0) {
		String insideParens = expression.substring(openParenIndex+1, closeParenIndex);
		String[] insideParensEvaluated = evaluate(insideParens, bindings, request);
		if (insideParensEvaluated[0] == null) {
		    return insideParensEvaluated;
		}
		// continue with the parenthesised expression replaced by its value
		addError(insideParensEvaluated[1], result);
		expression = expression.substring(0, openParenIndex) + 
			     insideParensEvaluated[0] + 
			     expression.substring(closeParenIndex+1);
	    }
	}
	ArrayList<String> parts;
	ArrayList<Double> numbers;
	parts = ServerUtilities.removeEmptyLines(expression.split("\\+"));
	if (parts.size() > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result, request);
	    Double sum = 0.0;
	    for (Double number : numbers) {
		if (number != null) {
		    sum += number;
		}			
	    }
	    result[0] = Double.toString(sum);
	    return result;
	}
	parts = ServerUtilities.removeEmptyLines(expression.split("\\*"));
	if (parts.size() > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result, request);
	    Double product = 1.0;
	    for (Double number : numbers) {
		if (number != null) {
		    product *= number;
		}			
		result[0] = Double.toString(product);
	    }
	    return result;
	}
	parts = ServerUtilities.removeEmptyLines(expression.split("/"));
	if (parts.size() > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result, request);
	    Double dividend = 1.0;
	    for (Double number : numbers) {
		if (number != null) {
		    if (number == 0) {
			result[0] += "Unable to divide by zero in " + expression;
		    } else {
			dividend /= number;
		    }
		}			
		result[0] = Double.toString(dividend);
	    }
	    return result;
	}
	parts = ServerUtilities.removeEmptyLines(expression.split("\\-"));
	if (parts.size() > 1) {
	    numbers = evaluateAllToNumbers(parts, bindings, result, request);
	    Double difference = 0.0;
	    for (Double number : numbers) {
		if (number != null) {
		    difference -= number;
		}			
		result[0] = Double.toString(difference);
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

    /**
     * @param data
     * @param result -- result[0] is comma separated version of data; result[1] any error messages
     */
    private void toCSV(String data, String[] result) {
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
	if (dataValues.length < 2) {
	    result[0] = data;
	} else {
	    result[0] = dataValues[0];
	    for (int i = 1; i < dataValues.length; i++) {
		result[0] += "," + dataValues[i];
	    }
	}	
    }

    private ArrayList<Double> evaluateAllToNumbers(ArrayList<String> parts, HashMap<String, String> bindings, String[] result, HttpServletRequest request) {
	ArrayList<Double> numbers = new ArrayList<Double>(parts.size());
	for (String part : parts) {
	    numbers.add(evaluateToNumber(part.trim(), bindings, result, request));
	}
	return numbers;
    }
    
    private Double evaluateToNumber(String expression, HashMap<String, String> bindings, String[] result, HttpServletRequest request) {
	String[] evaluation = evaluate(expression, bindings, request);
	String value = evaluation[0];
	if (value != null) {
	    int commaIndex = value.indexOf(",");
	    if (commaIndex > 0) {
		value = value.substring(0, commaIndex);
	    }
	    try {
		return Double.parseDouble(value);
	    } catch (NumberFormatException e) {
		addError(" The following is not a number: " + value + " while computing " + expression, result);
	    }
	} 
	addError(evaluation[1], result);
	return null;
    }
    
    private String[] evaluateCondition(String condition, HashMap<String, String> bindings, HttpServletRequest request) {
	// result[0] is either "true", "false", or null
	// result[1] any errors
	String result[] = new String[2];
	String[] parts;
	Relation relation;
	parts = condition.split(">=", 2);
	if (parts.length == 2) {
	    relation = Relation.GREATER_THAN_OR_EQUAL;
	} else {
	    parts = condition.split(">", 2);
	    if (parts.length == 2) {
		relation = Relation.GREATER_THAN;
	    } else {
		parts = condition.split("<", 2);
		if (parts.length == 2) {
		    relation = Relation.LESS_THAN_OR_EQUAL;
		} else {
		    parts = condition.split("<", 2);
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
	Double leftValue = evaluateToNumber(parts[0].trim(), bindings, result, request);
	Double rightValue = evaluateToNumber(parts[1].trim(), bindings, result, request);
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

    private String processEquations(String left, String right, HashMap<String, String> bindings, HttpServletRequest request) {
	// return error/warnings or null if none
	// right may have following equations
	int equalSignIndex = right.indexOf("=");
	if (equalSignIndex >= 0) {
	    int spaceBeforeEqualSignIndex = equalSignIndex;
	    while (spaceBeforeEqualSignIndex < right.length() && right.charAt(spaceBeforeEqualSignIndex-1) == ' ') {
		// ignore spaces before the equal sign
		spaceBeforeEqualSignIndex--;
	    }
	    int nextWordStartIndex = right.lastIndexOf(" ", spaceBeforeEqualSignIndex-1);
	    if (nextWordStartIndex >= 0) {
		String nextLeft = right.substring(nextWordStartIndex+1, spaceBeforeEqualSignIndex).trim();
		String nextRight = right.substring(equalSignIndex+1).trim();
		String errors = processEquations(nextLeft, nextRight, bindings, request);
		if (errors != null) {
		    return errors;
		}
		right = right.substring(0, nextWordStartIndex);
	    }
	} 
	String[] rightEvaluated = evaluate(right, bindings, request);
	if (rightEvaluated[0] != null) {
	    String rightValues = rightEvaluated[0];
	    return matchDataAndVariables(left, rightValues, bindings);
	} else {
	    return rightEvaluated[1];
	}
    }

    private String matchDataAndVariables(String variables, String data, HashMap<String, String> bindings) {
	// returns warning/error or null
	String[] variableNames = variables.split(",");
	String[] dataValues = data.split(",");
	if (variableNames.length > dataValues.length) {
	    return "More variables (" + variableNames.length + ") than data values (" + dataValues.length + ")";
	}
	for (int i = 0; i < variableNames.length; i++) {
	    bindings.put(variableNames[i].trim(), dataValues[i].trim());
	}
	return null;
    }

}
