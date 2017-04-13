package com.foobnix.pdf.info.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Doc implements Serializable{
	private String docname;
	private String username;
	private String documentId;

	private String title;
	private String description;
	private int pagecount;
	private String photo;
	private double rating; 
    private boolean isExplicit;

    public static Doc BUILD(final Content content) {
        final Doc doc = new Doc();
        doc.documentId = content.getRevisionId() + "-" + content.getPublicationId();
        doc.docname = content.getPublicationName();
        doc.title = content.getTitle();
        doc.pagecount = content.getPageCount();
        doc.username = content.getOwnerUsername();
        doc.isExplicit = content.isExplicit();
        return doc;
    }

    public Doc() {

    }
	public Doc(final JSONObject obj) {
		docname = obj.optString("docname");
		username = obj.optString("username");
		documentId = obj.optString("documentId");
		photo = obj.optString("photo");

		title = obj.optString("title");
		description = obj.optString("description");
		pagecount = obj.optInt("pagecount");
		
		rating = obj.optDouble("rating");

	}
	public JSONObject toJSON() throws JSONException{
		final JSONObject obj = new JSONObject();
		obj.put("docname", docname);
		obj.put("username", username);
		obj.put("documentId", documentId);
		obj.put("photo", photo);
		obj.put("title", title);
		obj.put("description", description);
		obj.put("pagecount", pagecount);
		return obj;
	}
	
	@Override
	public boolean equals(final Object o) {
		return o instanceof Doc && documentId.equals(((Doc)o).documentId);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public int getPagecount() {
		return pagecount;
	}

	public void setPagecount(final int pagecount) {
		this.pagecount = pagecount;
	}

	public String getDocname() {
		return docname;
	}

	public void setDocname(final String docname) {
		this.docname = docname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(final String documentId) {
		this.documentId = documentId;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(final String photo) {
		this.photo = photo;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(final double rating) {
		this.rating = rating;
	}

    public boolean isExplicit() {
        return isExplicit;
    }

}


