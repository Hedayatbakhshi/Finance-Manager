package com.financemanager.app.ui.common;

import java.io.Serializable;

/** One selectable row in a picker bottom sheet. */
public class PickerItem implements Serializable {

    public long id;
    public String title = "";
    public String subtitle = "";
    public String iconKey = "cat_other";
    public String colorHex = "#778397";

    public PickerItem() {
    }

    public PickerItem(long id, String title, String subtitle, String iconKey, String colorHex) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.iconKey = iconKey;
        this.colorHex = colorHex;
    }
}
