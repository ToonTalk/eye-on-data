package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
    
    private String previousEditorContents = null;
    
    private int editorHeight = 300;

    /**
     * Create a remote service proxy to talk to the server-side service.
     */
    private final EyeOnDataServiceAsync eyeOnDataService = GWT.create(EyeOnDataService.class);

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
	instance = this;
	VerticalPanel contents = new VerticalPanel();
	contents.setSpacing(10);
	RootPanel.get().add(contents);
	HTML welcome = new HTML(strings.welcomeMessage());
	contents.add(welcome);
	final RichTextEntry richTextEntry = new RichTextEntry();
	previousEditorContents = richTextEntry.getHTML();
	ClickHandler addHandler = new ClickHandler() {

	    @Override
	    public void onClick(ClickEvent event) {
		AsyncCallback<String[]> callback = new AsyncCallback<String[]>() {

		    @Override
		    public void onFailure(Throwable caught) {
			Utilities.popupMessage(strings.serverErrorMessage());
		    }

		    @Override
		    public void onSuccess(String[] result) {
			if (result[1] != null) {
			    Utilities.popupMessage(result[1]);
			}
			pageId = result[0];			
		    }
		    
		};
		previousEditorContents = richTextEntry.getHTML();
		eyeOnDataService.saveWebPage(previousEditorContents, pageId, callback);
	    }
	    
	};
	richTextEntry.addSaveButtonClickHandler(addHandler);
	ClickHandler cancelHandler = new ClickHandler() {

	    @Override
	    public void onClick(ClickEvent event) {
		richTextEntry.setHTML(previousEditorContents);
		
	    }
	    
	};
	richTextEntry.addCancelButtonClickHandler(cancelHandler);
	contents.add(richTextEntry);
	HTML helpMessage = new HTML(strings.helpMessage());
	contents.add(helpMessage);

//
//	    /**
//	     * Send the name from the nameField to the server and wait for a response.
//	     */
//	    private void sendNameToServer() {
//		// First, we validate the input.
//		errorLabel.setText("");
//		String textToServer = nameField.getText();
//		if (!FieldVerifier.isValidName(textToServer)) {
//		    errorLabel.setText("Please enter at least four characters");
//		    return;
//		}
//
//		// Then, we send the input to the server.
//		sendButton.setEnabled(false);
//		textToServerLabel.setText(textToServer);
//		serverResponseLabel.setText("");
//		eyeOnDataService.saveWebPage(textToServer, null,
//			new AsyncCallback<String>() {
//			    public void onFailure(Throwable caught) {
//				// Show the RPC error message to the user
//				dialogBox
//					.setText("Remote Procedure Call - Failure");
//				serverResponseLabel
//					.addStyleName("serverResponseLabelError");
//				serverResponseLabel.setHTML(SERVER_ERROR);
//				dialogBox.center();
//				closeButton.setFocus(true);
//			    }
//
//			    public void onSuccess(String result) {
//				dialogBox.setText("Remote Procedure Call");
//				serverResponseLabel
//					.removeStyleName("serverResponseLabelError");
//				serverResponseLabel.setHTML(result);
//				dialogBox.center();
//				closeButton.setFocus(true);
//			    }
//			});
//	    }
//	}
//
    }

    public static EyeOnData instance() {
        return instance;
    }

    public int getEditorHeight() {
        return editorHeight;
    }
}
