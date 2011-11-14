/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server.objectify;

import javax.persistence.Id;

/**
 * @author Ken Kahn
 *
 */
public class WebPage {

    @Id private String pageId;
    private String html;
    
    public WebPage(String pageId, String html) {
	this.pageId = pageId;
	this.html = html;
    }
    
    public WebPage() {
	// for Objectify
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getPageId() {
        return pageId;
    }
}
