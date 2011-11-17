package uk.ac.ox.oucs.eyeondata.client;

import uk.ac.ox.oucs.eyeondata.shared.Utilities;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class EyeOnData implements EntryPoint {
    
    public static EyeOnDataConstants strings = (EyeOnDataConstants) GWT.create(EyeOnDataConstants.class);
    
    private static EyeOnData instance;
    
    private String pageId = null;
    
    private String readOnlyPageId = null;
    
    private String previousEditorContents = null;
    
    private HTML tryItNowHTML = new HTML();
    
    private HTML editAnotherTime = new HTML();
    
    private int heightOfAllButEditor = 340;
    
    private int widthOfAllButEditor = 50;

    /**
     * Create a remote service proxy to talk to the server-side service.
     */
    private final EyeOnDataServiceAsync eyeOnDataService = GWT.create(EyeOnDataService.class);

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
	instance = this;
	pageId = Location.getParameter("pageId");
	if (pageId == null) {
	    String url = Location.getHref();
	    int hashIndex = url.indexOf('#');
	    if (hashIndex >= 0) {
		pageId = url.substring(hashIndex+1);
		if (Utilities.isURL(pageId)) {
		    String redirectionURL = GWT.getHostPageBaseURL()+ "v/?url=" + pageId;
		    Location.replace(redirectionURL);
		    return;
		}
	    }
	}
	VerticalPanel contents = new VerticalPanel();
	contents.setSpacing(10);
	RootPanel.get().add(contents);
	HTML welcome = new HTML(strings.welcomeMessage());
	contents.add(welcome);
	final RichTextEntry richTextEntry = new RichTextEntry();
	if (pageId != null) {
	    fetchPreviousPageContents(richTextEntry);
	}
	previousEditorContents = richTextEntry.getHTML();
	ClickHandler addHandler = new ClickHandler() {

	    @Override
	    public void onClick(ClickEvent event) {
		tryItNowHTML.setHTML(strings.pleaseWait());
		AsyncCallback<String[]> callback = new AsyncCallback<String[]>() {

		    @Override
		    public void onFailure(Throwable caught) {
			ClientUtilities.popupMessage(strings.serverErrorMessage());
		    }

		    @Override
		    public void onSuccess(String[] result) {
			if (result[2] != null) {
			    ClientUtilities.popupMessage(result[1]);
			}
			pageId = result[0];
			if (result[1] != null) {
			    readOnlyPageId = result[1];
			}
			addTryItNowLink();
			addEditAnotherTimeURL();
		    }
		    
		};
		previousEditorContents = richTextEntry.getHTML();
		eyeOnDataService.saveWebPage(previousEditorContents, pageId, callback);
	    }
	    
	};
	richTextEntry.addSaveButtonClickHandler(addHandler);
	//	ClickHandler cancelHandler = new ClickHandler() {
//
//	    @Override
//	    public void onClick(ClickEvent event) {
//		richTextEntry.setHTML(previousEditorContents);
//	    }
//	    
//	};
//	richTextEntry.addCancelButtonClickHandler(cancelHandler);
	KeyPressHandler keyPressHandler = new KeyPressHandler() {

	    @Override
	    public void onKeyPress(KeyPressEvent event) {
		tryItNowHTML.setHTML("");		
	    }
	    
	};
	richTextEntry.addKeyPressHandler(keyPressHandler);
	contents.add(richTextEntry);
	HTML helpMessage = new HTML(strings.helpMessage());
	contents.add(helpMessage);
	contents.add(tryItNowHTML);
	contents.add(editAnotherTime);
    }
    
    private void fetchPreviousPageContents(final RichTextEntry richTextEntry) {
	editAnotherTime.setHTML(strings.fetchingPagePleaseWait());
	AsyncCallback<String[]> callback = new AsyncCallback<String[]>() {

	    @Override
	    public void onFailure(Throwable caught) {
		ClientUtilities.popupMessage(strings.serverErrorMessage());
	    }

	    @Override
	    public void onSuccess(String[] result) {
		if (result[0] != null) {
		    richTextEntry.setHTML(result[0]);
		    addEditAnotherTimeURL();
		    readOnlyPageId = result[1];
		} else {
		    editAnotherTime.setHTML("");
		}
		if (result[2] != null) {
		    ClientUtilities.popupMessage(result[2]);
		}
	    }
	    
	};
	eyeOnDataService.fetchPreviousPageContents(pageId, callback);
    }

    private void addTryItNowLink() {
	String url = GWT.getHostPageBaseURL() + "v/" + readOnlyPageId + ".html";
	String newHTML = strings.tryItNow().replace("***URL***", url);
	tryItNowHTML.setHTML(newHTML);
    }
    
    private void addEditAnotherTimeURL() {
	// see http://stackoverflow.com/questions/5402732/gwt-set-url-without-submit
	String newURL = Window.Location.createUrlBuilder().setHash(pageId).buildString();
	Window.Location.replace(newURL);
   	editAnotherTime.setHTML(strings.editAnotherTime());
    }

    public static EyeOnData instance() {
        return instance;
    }
    
    public int getEditorWidth() {
        return Window.getClientWidth()-widthOfAllButEditor;
    }

    public int getEditorHeight() {
        return Window.getClientHeight()-heightOfAllButEditor;
    }
}
