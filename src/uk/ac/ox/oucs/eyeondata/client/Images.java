package uk.ac.ox.oucs.eyeondata.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

/**
 * This {@link ClientBundle} is used for all the button icons. Using an image
 * bundle allows all of these images to be packed into a single image, which
 * saves a lot of HTTP requests, drastically improving startup time.
 */
public interface Images extends ClientBundle {
    
    // For the RichTextEditor:

    @Source("uk/ac/ox/oucs/eyeondata/client/images/bold.gif")
    ImageResource bold();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/createLink.gif")
    ImageResource createLink();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/hr.gif")
    ImageResource hr();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/indent.gif")
    ImageResource indent();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/insertImage.gif")
    ImageResource insertImage();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/italic.gif")
    ImageResource italic();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/justifyCenter.gif")
    ImageResource justifyCenter();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/justifyLeft.gif")
    ImageResource justifyLeft();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/justifyRight.gif")
    ImageResource justifyRight();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/ol.gif")
    ImageResource ol();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/outdent.gif")
    ImageResource outdent();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/removeFormat.gif")
    ImageResource removeFormat();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/removeLink.gif")
    ImageResource removeLink();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/strikeThrough.gif")
    ImageResource strikeThrough();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/subscript.gif")
    ImageResource subscript();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/superscript.gif")
    ImageResource superscript();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/ul.gif")
    ImageResource ul();

    @Source("uk/ac/ox/oucs/eyeondata/client/images/underline.gif")
    ImageResource underline();

}
