package com.foobnix.pdf.info.model;

import org.json.JSONObject;

public class Content {

    private String publicationId;
    private boolean isEditorsPick;
    private String description;
    private boolean isExplicit;
    private int pageCount;
    private String ownerUsername;
    private String revisionId;
    private String title;
    private boolean isPreview;
    private int height;
    private int width;
    private String publicationName;
    private int impressions;
    private String ownerDisplayName;

    public Content(final JSONObject obj) {
        publicationId = obj.optString("publicationId");
        revisionId = obj.optString("revisionId");
        publicationName = obj.optString("publicationName");
        pageCount = obj.optInt("pageCount");
        title = obj.optString("title");
        ownerUsername = obj.optString("ownerUsername");

        if (obj.has("isExplicit")) {
            isExplicit = obj.optBoolean("isExplicit");
        } else {
            isExplicit = true;
        }
    }

    public String getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(final String publicationId) {
        this.publicationId = publicationId;
    }

    public boolean isEditorsPick() {
        return isEditorsPick;
    }

    public void setEditorsPick(final boolean isEditorsPick) {
        this.isEditorsPick = isEditorsPick;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isExplicit() {
        return isExplicit;
    }

    public void setExplicit(final boolean isExplicit) {
        this.isExplicit = isExplicit;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(final int pageCount) {
        this.pageCount = pageCount;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(final String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(final String revisionId) {
        this.revisionId = revisionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public boolean isPreview() {
        return isPreview;
    }

    public void setPreview(final boolean isPreview) {
        this.isPreview = isPreview;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public String getPublicationName() {
        return publicationName;
    }

    public void setPublicationName(final String publicationName) {
        this.publicationName = publicationName;
    }

    public int getImpressions() {
        return impressions;
    }

    public void setImpressions(final int impressions) {
        this.impressions = impressions;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(final String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

}
