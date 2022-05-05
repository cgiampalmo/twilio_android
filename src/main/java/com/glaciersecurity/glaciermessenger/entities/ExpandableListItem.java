package com.glaciersecurity.glaciermessenger.entities;

public class ExpandableListItem {

    public int image;
    //public Drawable imageDrw;
    public String name;
    public boolean isExpandable = false;
    public boolean isMajorIssue = false;
    public boolean parent = false;
    public String description;
    // flag when item swiped
    public boolean swiped = false;

    public ExpandableListItem() {
    }

    public ExpandableListItem(int image, String name, String description) {
        this.image = image;
        this.name = name;
        this.description = description;
        this.isExpandable = false;
        this.isMajorIssue = false;
    }
    public ExpandableListItem(int image, String name, String description, boolean isMajorIssue) {
        this.image = image;
        this.name = name;
        this.description = description;
        this.isExpandable = false;
        this.isMajorIssue = isMajorIssue;
    }
    public ExpandableListItem(int image, String name, String description, boolean expanded, boolean isMajorIssue) {
        this.image = image;
        this.name = name;
        this.description = description;
        this.isExpandable = expanded;
        this.isMajorIssue = isMajorIssue;
    }
}
