/**
 * 
 */
package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;

/**
 * Useful client methods
 * 
 * @author Ken Kahn
 *
 */
public class Utilities {
    
    /**
     * Displays textOrHTML in a centered pop up with auto hide.
     * 
     * @param textOrHTML
     */
    public static void popupMessage(String textOrHTML) {
        DecoratedPopupPanel popup = new DecoratedPopupPanel(true);
        popup.setWidget(new HTML(textOrHTML));
        popup.show();
        popup.center();
    }

}
