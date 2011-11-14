package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>EyeOnDataService</code>.
 */
public interface EyeOnDataServiceAsync {
    void saveWebPage(String html, String pageId, AsyncCallback<String[]> callback);
}
