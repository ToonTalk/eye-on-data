package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.user.client.ui.Button;

/**
 * A button that looks better than the default
 * 
 * @author Ken Kahn
 *
 */
public class EyeOnDataButton extends Button {
    public EyeOnDataButton(String html) {
	super(html);
	addStyleName("eye-on-data-button");
    }
    
    public void onLoad() {
	setWidth("100%");
	super.onLoad();	
    }

}
