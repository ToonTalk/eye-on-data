package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;

public class RichTextEntry extends Composite {
    private RichTextArea richTextArea = new RichTextArea();
    private RichTextToolbar toolBar = new RichTextToolbar(richTextArea);
    private Button saveButton = new EyeOnDataButton(EyeOnData.strings.saveButtonLabel());
    private Button cancelButton = new EyeOnDataButton(EyeOnData.strings.cancelButtonLabel());
    
    public RichTextEntry() {
	this(null);
    }
       
    public RichTextEntry(String initialHTML) {
	VerticalPanel panel = new VerticalPanel();
	panel.add(toolBar);
	DOM.setStyleAttribute(panel.getElement(), "marginRight", "4px");
	if (initialHTML != null) {
	    richTextArea.setHTML(initialHTML);
	} else {
	    richTextArea.setHTML(EyeOnData.strings.defaultEditorContents());
	}
	panel.add(richTextArea);
	HorizontalPanel buttonPanel = new HorizontalPanel();
	saveButton.setWidth("50px");
	cancelButton.setWidth("80px");
	buttonPanel.add(saveButton);
	buttonPanel.add(cancelButton);
	buttonPanel.setSpacing(6);
	panel.add(buttonPanel);
	// following looks nicer but buttons can be hard to reach if horizontally scrolling
	// is going on
//	panel.setCellHorizontalAlignment(buttonPanel, HasHorizontalAlignment.ALIGN_CENTER);
	richTextArea.setFocus(true);
	// All composites must call initWidget() in their constructors.
	initWidget(panel);
	// add 50 to have a bit of a margin for growth and to minimise the need for
	// scroll bars but don't get too large
	int windowWidth = Window.getClientWidth();
	int richTextWidth = windowWidth;
	richTextArea.setWidth(richTextWidth-30 + "px");
	int richTextHeight = EyeOnData.instance().getEditorHeight();
	richTextArea.setHeight(richTextHeight + "px");
	KeyPressHandler keyPressHandler = new KeyPressHandler() {

	    @Override
	    public void onKeyPress(KeyPressEvent event) {
		// if tab then don't add the tab but select the save button
		// enables accessibility without a mouse
		if (event.getCharCode() == '\t') {
		    richTextArea.setFocus(false);
		    saveButton.setFocus(true);
		    event.preventDefault();
		}	
	    }
	    
	};
	richTextArea.addKeyPressHandler(keyPressHandler);
    }
    
    public void addSaveButtonClickHandler(ClickHandler addHandler) {
	saveButton.addClickHandler(addHandler);	
    }
    
    public void addCancelButtonClickHandler(ClickHandler cancelHandler) {
	cancelButton.addClickHandler(cancelHandler);	
    }

    public RichTextArea getRichTextArea() {
        return richTextArea;
    }
    
    public String getHTML() {
	return richTextArea.getHTML();
    }
    
    public void setHTML(String html) {
	richTextArea.setHTML(html);
    }

};
