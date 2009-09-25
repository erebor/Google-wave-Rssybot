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

//TODO store data locally, sync up to server at time intervals. Too higher server requests


package rssybot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.FormView;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.Wavelet;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;

public class RssybotServlet extends AbstractRobotServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Class that renders blips
	 */
	private final BlipRender render;
	
	/**
	 * Class that provides RSS reading utilities
	 */
	private final RSSUtils rssUtils;
	
	/**
	 * Location of the viewer gadget
	 */
	public final String GADGET_URL = "http://rssybot.appspot.com/_wave/viewer.xml";
	
	/**
	 * Key that the viewer gadget looks for in the url
	 */
	public final String GADGET_FEED_KEY = "rssposturl";
	
	/**
	 * ID for the unsubscribe button
	 */
	public final String ID_BUTT_UNSUBSCRIBE = "unsubscribeB";
	
	/**
	 * ID for the subscribe button
	 */
	public final String ID_BUTT_SUBSCRIBE = "subscribeB";
	
	/**
	 * ID for the subscription link
	 */
	public final String ID_INPUT_SUBSCRIBE = "subscribeF";
	
	/**
	 * ID for the feed url in the wavelet data document
	 */
	public final String ID_DD_FEEDURL = "feedurl";
	
	/**
	 * ID for the open button
	 */
	public final String ID_BUTT_OPEN_PART = "open_rss_feed_butt_part";
	
	/**
	 * ID for the open button
	 */
	public final String ID_BUTT_CLOSE_PART = "close_rss_feed_butt_part";
	
	/**
	 * Persistent manager for communication with the datastore
	 */
	private PersistenceManager pm;
	
	/**
	 * Local copy of feeds
	 */
	private ArrayList<Feed> feedsLocal;
	
	/**
	 * Local copy of Posts
	 */
	private ArrayList<Post> postsLocal;
	
	/**
	 * Local copy of subscribers
	 */
	private ArrayList<Subscriber> subscribersLocal;
	
	
	
	
	
	
	/**
	 * Constructor
	 */
	public RssybotServlet() {
		loadRecords();
		this.render = new BlipRender(this);
		this.rssUtils = new RSSUtils(render);
	}
	
	
	
	
	
	/**
	 * Processes all incoming events
	 * @param bundle variable containing all incoming events and waves
	 * @Override
	 */
	public void processEvents(RobotMessageBundle bundle) {
		Wavelet wavelet = bundle.getWavelet();
		
		/*
		 * Robot added to new wave
		 * Render the header for a new wave-- this should request the feed url from the user
		 */
		if(bundle.wasSelfAdded()) {
			wavelet.appendBlip().getDocument().append("19th Sept 09 - A few updates to Google Wave have broken" +
					" this robot. It downloads your feed correctly when you subscribe however due to a bug with " +
					"Google Wave you will not see updates to the feed. I hope this will be fixed soon. For more " +
					"information please visit http://wave.to");
			render.nonSubscribedFeedHeader(wavelet);
		}
		
		/*
		 * Cron event
		 */
		if(bundle.getEvents().size() == 0) {
			rssUtils.updateFeeds(feedsLocal, postsLocal, subscribersLocal, bundle);
			writeRecords();
			loadRecords();
		}
		/*
		 * User interaction event
		 * Check that it is a button that has been clicked and pass the event on to a 
		 * method that will process each button accordingly
		 */
		else {
			for(Event ev : bundle.getEvents()) {
				if(ev.getType() == EventType.FORM_BUTTON_CLICKED) {
					processFormButtonClicked(bundle, ev);
				}
			}
		}
	}
	
	
	
	
	
	
	
	/**
	 * Find out which form button has been clicked and run appropriate code
	 * @param bundle variable containing all incoming events and waves
	 * @param ev the event that the user created
	 */
	private void processFormButtonClicked(RobotMessageBundle bundle, Event ev) {
		//Find out which button has been pressed
		if(ev.getButtonName().equals(ID_BUTT_SUBSCRIBE)) {
			if(ev.getModifiedBy().equals(ev.getWavelet().getCreator())) {
				subscribeUser(bundle, ev);
			}
		} else if(ev.getButtonName().equals(ID_BUTT_UNSUBSCRIBE)) {
			if(ev.getModifiedBy().equals(ev.getWavelet().getCreator())) {
				unsubscribeUser(ev);
			}
		} else if(ev.getButtonName().contains(ID_BUTT_OPEN_PART)) {
			render.addViewerGadget(ev.getBlip(), ev.getButtonName());
		} else if(ev.getButtonName().contains(ID_BUTT_CLOSE_PART)) {
			render.removeViewerGadget(ev.getBlip(), ev.getButtonName());
		}
	}
	
	
	
	
	
	
	/**
	 * Subscribe a user to a new feed
	 * @param bundle variable containing all incoming events and waves
	 * @param ev the event that the user created
	 */
	private void subscribeUser(RobotMessageBundle bundle, Event ev) {
		//Grab and declare a few variables
		Wavelet wavelet = ev.getWavelet();
		
		//Get the feed url from the form
		FormView formView = ev.getBlip().getDocument().getFormView();
		String feedURL = formView.getFormElement(ID_INPUT_SUBSCRIBE).getValue();
		
		//Attempt to fix the URL in case it is badly formed
		feedURL = fixURL(feedURL);
		
		Subscriber subscriber = new Subscriber(feedURL, wavelet.getCreator(), wavelet.getWaveId(), wavelet.getWaveletId());
		Feed feed = fetchFeed(feedURL);
		
		/*
		 * Feed doesn't exist. We need to create it
		 */
		if(feed == null) {
			SyndFeed rawFeed = null;
			try{
				
				rawFeed = rssUtils.retrieveFeed(feedURL);
				feed = new Feed(feedURL);
				
				//Save the data to disk
				postsLocal.addAll(rssUtils.parseFeed(rawFeed, feed));
				feedsLocal.add(feed);

			} catch (ParsingFeedException e) {
				render.awwSnapRootBlip(wavelet.getRootBlip(), "Couldn't find any feeds.", "Check that the URL points to a valid feed.");
				return;
			} catch (IOException e) {
				render.awwSnapRootBlip(wavelet.getRootBlip(), "The URL is not valid", "Check that you typed the URL correctly.");
				return;
			} catch (FeedException e) {
				render.awwSnapRootBlip(wavelet.getRootBlip(), "Couldn't find any feeds.", "Check that the URL points to a valid feed.");
				return;
			}
		} 
		
		
		/*
		 * Now the feed does exist, we need to add the new subscriber to it
		 */
		//Save data to disk and wave
		subscribersLocal.add(subscriber);
		wavelet.setDataDocument(ID_DD_FEEDURL, encodeStringToASCII(feedURL, ','));
		
		//Update the user
		render.subscribedFeedHeader(wavelet, feed.getFeedTitle());
		for(int i = 0; i < postsLocal.size(); i++) {
			Post temp = postsLocal.get(i);
			if(temp.getFeedURL().equals(feedURL)) {
				render.appendNewFeedPost(wavelet, temp);
			}
		}		
	}
	
	
	/**
	 * Attempts to fix the url in case it is broken. Works very simply by ensuring url is well
	 * structured and fixed common errors such as not including http
	 * @param URL the url to be fixed
	 * @return the fixed url
	 */
	private String fixURL(String URL) {
		if(URL.startsWith("feed://")) {
			char[] c = URL.toCharArray();
			c[0] = 'h'; c[1] = 't'; c[2] = 't'; c[3] = 'p';
			URL = new String(c);
		}else if(URL.startsWith("http://") == false) {
			URL = "http://" + URL;
		}
		
		return URL;
	}
	
	
	/**
	 * Unsubscribes a user from a feed
	 * @param ev the event that the user created
	 */
	private void unsubscribeUser(Event ev) {
		//Grab the required variables
		Wavelet wavelet = ev.getWavelet();
		String feedURL = decodeStringFromASCII(wavelet.getDataDocument(ID_DD_FEEDURL), ',');
		String creator = wavelet.getCreator();
		
		
		for(int i = 0; i < subscribersLocal.size(); i++) {
			Subscriber temp = subscribersLocal.get(i);
			if(temp.getUID().equals(creator) && temp.getFeedURL().equals(feedURL)) {
				subscribersLocal.remove(i);
				i--;
			}
		}
		render.unsubscribeNotification(wavelet);
	}
	
	
	/**
	 * Encodes a string into an ascii representation of itself
	 * @param text string to be encoded
	 * @param split character to split the ascii codes
	 * @return the ascii representation
	 */
	private String encodeStringToASCII(String text, char split) {
		//Text has no length, just return it
		if(text.length() == 0) {
			return text;
		}
		
		//Convert to output string
		char[] c = text.toCharArray();
		StringBuilder builder = new StringBuilder();
		
		builder.append((int)c[0]);
		for(int i = 1; i < c.length; i++) {
			builder.append(split + "" + (int)c[i]);
		}
		
		return builder.toString();
	}
	
	
	/**
	 * Decodes a string from ascii back into human readable text
	 * @param text string to be decoded
	 * @param split character that was used to split the ascii codes
	 * @return the human readable representation
	 */
	private String decodeStringFromASCII(String text, char split) {
		//Text has no length, just return it
		if(text.length() == 0) {
			return text;
		}
		
		String[] s = text.split(new String(""+split));
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < s.length; i++) {
			builder.append((char)Integer.parseInt(s[i]));
		}
		
		return builder.toString();
	}
	
	
	/**
	 * Searches feeds arrayList for a feedURL
	 * @param feedURL the url of the feed
	 * @return the found feed. Null if none is found
	 */
	private Feed fetchFeed(String feedURL) {
		for(int i = 0; i < feedsLocal.size(); i++) {
			if(feedsLocal.get(i).getFeedURL().equals(feedURL)) {
				return feedsLocal.get(i);
			}
		}
		
		return null;
	}
	
	/**
	 * Attempts to load data from the google datastore. If none exists creates new data sets
	 */
	@SuppressWarnings("unchecked")
	private void loadRecords() {
		pm = PMF.get().getPersistenceManager();
		try {
			//Retrieve data
			/*
			 * Feeds
			 */
			try {
				Query feedQuery = pm.newQuery(Feed.class);
				List<Feed> feedsResult = (List<Feed>) feedQuery.execute();
				
				//Copy data
				if(feedsResult.size() == 0) {
					feedsLocal = new ArrayList<Feed>();
				} else {
					feedsLocal = new ArrayList<Feed>();
					for(int i = 0; i < feedsResult.size(); i++) {
						feedsLocal.add(feedsResult.get(i));
					}
				}
			} catch(JDOObjectNotFoundException ex) {
				feedsLocal = new ArrayList<Feed>();
			}
			/*
			 * Posts
			 */
			try {
				Query postQuery = pm.newQuery(Post.class);
				List<Post> postsResult = (List<Post>) postQuery.execute();
				
				//Copy data
				if(postsResult.size() == 0) {
					postsLocal = new ArrayList<Post>();
				} else {
					postsLocal = new ArrayList<Post>();
					for(int i = 0; i < postsResult.size(); i++) {
						postsLocal.add(postsResult.get(i));
					}
				}
			} catch(JDOObjectNotFoundException ex) {
				postsLocal = new ArrayList<Post>();
			}
			/*
			 * Subscribers
			 */
			try {
				Query subscribersQuery = pm.newQuery(Subscriber.class);
				List<Subscriber> subscriberResult = (List<Subscriber>) subscribersQuery.execute();
				
				//Copy data
				if(subscriberResult.size() == 0) {
					subscribersLocal = new ArrayList<Subscriber>();
				} else {
					subscribersLocal = new ArrayList<Subscriber>();
					for(int i = 0; i < subscriberResult.size(); i++) {
						subscribersLocal.add(subscriberResult.get(i));
					}
				}
			} catch(JDOObjectNotFoundException ex) {
				subscribersLocal = new ArrayList<Subscriber>();
			}
		}finally {
			pm.close();
		}
	}
	
	
	/**
	 * Writes data to the google data store
	 */
	private void writeRecords() {
		pm = PMF.get().getPersistenceManager();
		try {
			/*
			 * Feeds
			 */
			Query feedQuery = pm.newQuery(Feed.class);
			feedQuery.deletePersistentAll();
			
			//Write new records
			pm.makePersistentAll(feedsLocal);
			
			/*
			 * Posts
			 */
			Query postQuery = pm.newQuery(Post.class);
			postQuery.deletePersistentAll();
			
			//Write new records
			pm.makePersistentAll(postsLocal);
			
			/*
			 * Subscribers
			 */
			Query subscribersQuery = pm.newQuery(Subscriber.class);
			subscribersQuery.deletePersistentAll();
			
			//Write new records
			pm.makePersistentAll(subscribersLocal);
		} finally {
			pm.close();
		}
	}
	
	
}



















