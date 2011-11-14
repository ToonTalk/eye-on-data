package uk.ac.ox.oucs.eyeondata.server.objectify;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.DAOBase;

public class DAO extends DAOBase {

    static {
        ObjectifyService.register(WebPage.class);
    }

    public void persistObject(Object object) {
	ofy().put(object);
    } 

}
