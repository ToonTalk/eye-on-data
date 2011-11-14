/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.server.objectify;

import javax.persistence.Id;

/**
 * Provides access from a read-only page id to the read-write id
 * 
 * @author Ken Kahn
 *
 */
public class ReadOnlyPageId {

    @Id private String readOnlyPageId;
    private String pageId;
    
    public ReadOnlyPageId(String readOnlyPageId, String pageId) {
	this.readOnlyPageId = readOnlyPageId;
	this.pageId = pageId;
    }
    
    public ReadOnlyPageId() {
	// for Objectify
    }

    public String getPageId() {
        return pageId;
    }

    public String getReadOnlyPageId() {
        return readOnlyPageId;
    }
}
