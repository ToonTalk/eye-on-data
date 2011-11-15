package uk.ac.ox.oucs.eyeondata.server.objectify;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.DAOBase;

public class DAO extends DAOBase {

    static {
        ObjectifyService.register(WebPage.class);
        ObjectifyService.register(ReadOnlyPageId.class);
        ObjectifyService.register(PageId.class);
    }

    public void persistObject(Object object) {
	ofy().put(object);
    }
    
    public WebPage getWebPage(String pageId) {
	Objectify ofy = ofy();
	return ofy.find(WebPage.class, pageId);
    }
    
    public String getPageId(String readOnlyPageId) {
	Objectify ofy = ofy();
	ReadOnlyPageId readOnlyPageIdEntry = ofy.find(ReadOnlyPageId.class, readOnlyPageId);
	if (readOnlyPageIdEntry == null) {
	    return null;
	} else {
	    return readOnlyPageIdEntry.getPageId();
	}
    }
    
    public String getReadOnlyPageId(String pageId) {
	Objectify ofy = ofy();
	PageId pageIdEntry = ofy.find(PageId.class, pageId);
	if (pageIdEntry == null) {
	    return null;
	} else {
	    return pageIdEntry.getReadOnlyPageId();
	}
    }

}
