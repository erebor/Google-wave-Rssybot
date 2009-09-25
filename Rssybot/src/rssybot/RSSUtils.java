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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import com.google.wave.api.RobotMessageBundle;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.ParsingFeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RSSUtils {
	
	/**
	 * Class that renders blips
	 */
	private final BlipRender render;
	
	public RSSUtils(BlipRender render) {
		this.render = render;
	}
	
	
	
	
	/**
	 * Retrieves the feed from the given url
	 * @param feedURL the location of the feed
	 * @return a SyndFeed object containing the feed
	 * @throws IOException thrown when the url could not be fetched
	 * @throws FeedException thrown when there is a problem with the URL
	 * @throws ParsingFeedException thrown when there is a problem with the url
	 */
	public SyndFeed retrieveFeed(String feedURL) throws IOException, FeedException, ParsingFeedException{
		
		URLConnection feedUrl = new URL(feedURL).openConnection();
		
		//Create the feed object
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed  = input.build(new XmlReader(feedUrl));
		
		return feed;
	}
	
	/**
	 * Parses a SyndFeed object into a feed object so that it can be used by rssybot
	 * @param rawFeed the SyndFeed representation of the feed
	 * @param feed reference to the feed object that the formatted feed will be placed into
	 */
	@SuppressWarnings({ "unchecked" })
	public ArrayList<Post> parseFeed(SyndFeed rawFeed, Feed feed) {
		ArrayList<Post> posts = new ArrayList<Post>();
		feed.setFeedTitle(rawFeed.getTitle());
		
		//Retrieve the entries
		List list = rawFeed.getEntries();
		
		//Place the entry into the feed
		for(int i = list.size() - 1; i >= 0 ; i--) {
			SyndEntry entry = (SyndEntry)list.get(i);
			
			posts.add(updatePost(entry, feed.getFeedURL()));
		}
		return posts;
	}
	
	/**
	 * Places the raw post into the feed
	 * @param entry syndEntry with the raw details
	 * @param feedURL the feed URL
	 * @return the newly created post
	 */
	private Post updatePost(SyndEntry entry, String feedURL) {
		//Filter out null authors
		String author;
		if(entry.getAuthor() == "") {
			author = "unknown";
		} else
			author = entry.getAuthor();
		//Filter out null descriptions
		String description;
		if(entry.getDescription() == null) {
			description = "None";
		} else {
			description = "" + entry.getDescription().getValue();
		}
		
		
		//Create the post and return it
		return new Post(feedURL, entry.getTitle(), author, entry.getLink(), description,
							entry.getPublishedDate(), entry.getUpdatedDate());
		
		
	}
	
	/**
	 * Updates all the feeds and updates the users too
	 */
	@SuppressWarnings("unchecked")
	public void updateFeeds(ArrayList<Feed> feeds, ArrayList<Post> posts, ArrayList<Subscriber> subscribers, RobotMessageBundle bundle) {
		//Run through each feed
		for(int i = 0; i < feeds.size(); i++) {
			Feed feed = feeds.get(i);
			String feedURL = feed.getFeedURL();
			
			try {
				//Grab all the posts from the RSS server for this feed
				SyndFeed rawFeed = retrieveFeed(feedURL);
				List serverPosts = rawFeed.getEntries();
				
				//Get posts from local copy
				ArrayList<Post> localPosts = new ArrayList<Post>();
				for(int j = 0; j < posts.size(); j++) {
					if(posts.get(j).getFeedURL().equals(feedURL)) {
						localPosts.add(posts.get(j));
					}
				}
				
				//Loop through server copy to see if we have it
				for(int j = serverPosts.size() -1; j >= 0; j--) {
					SyndEntry newEntry = (SyndEntry)serverPosts.get(j);
					boolean contains = false;
					for(int k = 0; k < localPosts.size(); k++) {
						Post localPost = localPosts.get(k);
						if(newEntry.getLink().equals(localPost.getPostURL()) 
								&& newEntry.getTitle().equals(localPost.getPostTitle())) {
							contains = true;
							break;
						}
					}
					
					//It is not there
					if(contains == false) {
						Post newPost = updatePost(newEntry, feedURL);
						posts.add(newPost);
						
						//Write to all subscribers
						for(int k = 0; k < subscribers.size(); k++) {
							if(subscribers.get(i).getFeedURL().equals(feedURL)) {
								Subscriber temp = subscribers.get(i);
								render.appendNewFeedPost(bundle.getWavelet(temp.getWaveID(), temp.getWaveletID()), newPost);
							}
						}
					}
				}
				
			}
			catch(Exception ex) {
				/*
				 * This should never be thrown. All links will have been checked already. If an error does occur then
				 * the next cron event will pick it up next time.
				 */
			}
		}
	}
}












