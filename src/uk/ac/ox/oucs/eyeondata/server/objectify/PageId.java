/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server.objectify;

import javax.persistence.Id;

/**
 * Provides access from a read-write page id to the read-only id
 * 
 * @author Ken Kahn
 *
 */
public class PageId {

    private String readOnlyPageId;
    @Id private String pageId;
    
    public PageId(String readOnlyPageId, String pageId) {
	this.readOnlyPageId = readOnlyPageId;
	this.pageId = pageId;
    }
    
    public PageId() {
	// for Objectify
    }

    public String getPageId() {
        return pageId;
    }

    public String getReadOnlyPageId() {
        return readOnlyPageId;
    }
}
