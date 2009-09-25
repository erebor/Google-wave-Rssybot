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

import java.util.Comparator;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Post implements Comparator<Post>{
	
	@SuppressWarnings("unused")
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String feedURL;
	
	@Persistent
	private String postTitle;
	
	@Persistent
	private String postAuthor;
	
	@Persistent
	private String postURL;
	
	@Persistent
	private String postDescription;
	
	@Persistent
	private Date postPublished;
	

	

	public Post(String feedURL, String postTitle, String postAuthor, String postURL, String postDescription,
			Date postPublished, Date postUpdated) {
		this.feedURL = feedURL;
		this.postTitle = postTitle;
		this.postAuthor = postAuthor;
		this.postURL = postURL;
		this.postDescription = postDescription;
		this.postPublished = postPublished;
	}

	public String getFeedURL() {
		return feedURL;
	}
	
	public String getPostTitle() {
		return postTitle;
	}

	public void setPostTitle(String postTitle) {
		this.postTitle = postTitle;
	}

	public String getPostDescription() {
		return postDescription;
	}

	public void setPostDescription(String postDescription) {
		this.postDescription = postDescription;
	}

	public Date getPostPublished() {
		return postPublished;
	}

	public void setPostPublished(Date postPublished) {
		this.postPublished = postPublished;
	}

	public String getPostURL() {
		return postURL;
	}

	public String getPostAuthor() {
		return postAuthor;
	}

	public void setPostAuthor(String postAuthor) {
		this.postAuthor = postAuthor;
	}
	
	
	
	
	public int compare(Post o1, Post o2) {
		return o1.getPostPublished().compareTo(o2.getPostPublished());
	}
	
	
	
	
}
