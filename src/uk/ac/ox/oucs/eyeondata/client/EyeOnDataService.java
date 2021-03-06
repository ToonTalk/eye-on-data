package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("EyeOnData")
public interface EyeOnDataService extends RemoteService {
    
    String[] saveWebPage(String html, String pageId);
    
    String[] fetchPreviousPageContents(String pageId);
}


