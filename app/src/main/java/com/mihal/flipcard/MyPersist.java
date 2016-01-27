package com.mihal.flipcard;

/**
 * Created by Davidovich_M on 2016-01-22.
 */
class MyPersist {
    private String path;
    private String other;
    private boolean tabs_used;
    private boolean show_learned;
    private boolean preview;

    public String getDelimiter() {
        if (tabs_used) return "\t";
        else return other;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public boolean isTabs_used() {
        return tabs_used;
    }

    public void setTabs_used(boolean tabs_used) {
        this.tabs_used = tabs_used;
    }

    public boolean isShow_learned() {
        return show_learned;
    }

    public void setShow_learned(boolean show_learned) {
        this.show_learned = show_learned;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }
}
