// Copyright 2009, Acknack Ltd. All rights reserved.
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.


package rssybot;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import com.google.wave.api.Blip;
import com.google.wave.api.ElementType;
import com.google.wave.api.FormElement;
import com.google.wave.api.FormView;
import com.google.wave.api.Gadget;
import com.google.wave.api.GadgetView;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;

public class BlipRender {

	/**
	 * Link to the parent calling class, used for accessing some public variables
	 */
	private final RssybotServlet root;
	
	
	
	/**
	 * Constructor
	 * @param root reference to calling class so variables can be used
	 */
	public BlipRender(RssybotServlet root) {
		this.root = root;
	}
	
	
	
	
	
	/**
	 * Renders the root blip of the feed. This will include such things as the title, subscribe and un-subscribe
	 * @param wavelet the wavelet of the wave
	 */
	public void nonSubscribedFeedHeader(Wavelet wavelet) {
		wavelet.setTitle("New Feed");
		
		TextView textView = wavelet.getRootBlip().getDocument();
		textView.append("\nEnter the URL of the feed that you want to subscribe to, then press subscribe.\n");
		textView.appendElement(new FormElement(ElementType.INPUT, root.ID_INPUT_SUBSCRIBE));
		textView.appendElement(new FormElement(ElementType.BUTTON, root.ID_BUTT_SUBSCRIBE, "Subscribe"));
	}
	
	/**
	 * Renders the root blip of a feed. This will include such things as the title, subscribe and un-subscribe
	 * @param wavelet the wavelet of the wave
	 * @param feedTitle the title of the feed
	 */
	public void subscribedFeedHeader(Wavelet wavelet, String feedTitle) {
		//Clear the root blip
		TextView textView = wavelet.getRootBlip().getDocument();
		textView.delete();
		
		//Paint the new blip
		wavelet.setTitle(feedTitle);
		textView.append("\n");
		textView.appendElement(new FormElement(ElementType.BUTTON, root.ID_BUTT_UNSUBSCRIBE, "Unsubscribe from this thread"));
	}
	
	/**
	 * Renders an error message as a child to the root blip
	 * @param blip the blip that the child error should be appended to
	 * @param error details about the error
	 * @param advice details about what the user should do next
	 */
	public void awwSnapRootBlip(Blip blip, String error, String advice) {
		blip.createChild().getDocument().append("\nAwww snap, something went wrong...\n" + error +"\n" + advice);
	}
	
	/**
	 * Renders a post as a child to a root blip
	 * @param wavelet the wavelet that the feed is situated in
	 * @param post the post to be appended to the wavelet
	 */
	public void appendNewFeedPost(Wavelet wavelet, Post post) {
		//Protect against a null date
		String postUpdated;
		if(post.getPostPublished() == null) {
			postUpdated = "unknown";
		}
		else {
			Format formatter = new SimpleDateFormat("HH:mm dd MMM yyyy");
			postUpdated = formatter.format(post.getPostPublished());
		}
		
		TextView textView = wavelet.appendBlip().getDocument();
		textView.append(post.getPostTitle() +
						"\nBy: " + post.getPostAuthor() + " At: " + postUpdated + 
						"\nPost URL: " + post.getPostURL() + 
						"\nDescription: " + post.getPostDescription() + "\n");
		
		textView.appendElement(new FormElement(ElementType.BUTTON, root.ID_BUTT_OPEN_PART + post.getPostURL(), "open"));
	}
	
	/**
	 * Informs the user they have been unsubscribed by appending data to root blip
	 * @param wavelet the wavelet that the feed is situated in
	 */
	public void unsubscribeNotification(Wavelet wavelet) {
		wavelet.getRootBlip().getDocument().delete();
		wavelet.getRootBlip().getDocument().append("You are no longer subscribed to this feed");
	}
	
	/**
	 * Adds a viewer gadget to the blip with the specified address. Also sorts button out
	 * @param url string containing address page to be loaded
	 * @param blip the blip to place the gadget it
	 */
	public void addViewerGadget(Blip blip, String buttonName) {
		//Sort the button out first
		String url = buttonName.replace(root.ID_BUTT_OPEN_PART, "");
		
		FormView formView = blip.getDocument().getFormView();
		formView.delete(buttonName);
		formView.append(new FormElement(ElementType.BUTTON, root.ID_BUTT_CLOSE_PART + url, "close"));
		
		
		//Add the gadget
		GadgetView gadgetView = blip.getDocument().getGadgetView();
		gadgetView.append(new Gadget(root.GADGET_URL + "?" + root.GADGET_FEED_KEY + "=" + url));
	}
	
	/**
	 * Removes the gadget from the blip and sorts the button out
	 * @param blip the blip with the gadget within
	 */
	public void removeViewerGadget(Blip blip, String buttonName) {
		//Sort out the button
		String url = buttonName.replace(root.ID_BUTT_CLOSE_PART, "");
		
		FormView formView = blip.getDocument().getFormView();
		formView.delete(buttonName);
		formView.append(new FormElement(ElementType.BUTTON, root.ID_BUTT_OPEN_PART + url, "open"));
		
		//Remove the gadget
		GadgetView gadgetView = blip.getDocument().getGadgetView();
		List<Gadget> gadgets = gadgetView.getGadgets();
		Iterator<Gadget> iter = gadgets.iterator();
		while(iter.hasNext()) {
			gadgetView.delete(iter.next());
		}
	}
	
	
	/**
	 * Removes all the child posts below the root blip
	 * @param wavelet the wavelet where you want to remove everything from
	 */
	public void clearWave(Wavelet wavelet) {
		//Remove all child blips(clear the wave)
		Iterator<Blip> childrenIter = wavelet.getRootBlip().getChildren().iterator();
		while(childrenIter.hasNext()) {
			childrenIter.next().delete();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
