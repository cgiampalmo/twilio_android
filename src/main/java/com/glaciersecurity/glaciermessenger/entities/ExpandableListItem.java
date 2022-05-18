package com.glaciersecurity.glaciermessenger.entities;

import android.graphics.drawable.Drawable;

public class ExpandableListItem {

    public int image;
    //public Drawable imageDrw;
    public String name;
    public boolean expanded = false;
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
    }
}
