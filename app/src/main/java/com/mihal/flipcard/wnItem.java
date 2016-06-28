package com.mihal.flipcard;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Davidovich_M on 2016-06-28.
 */
class wnItem implements Parcelable {
    private int cur;
    private int tot;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(cur);
        dest.writeInt(tot);
    }

    public static final Creator<wnItem> CREATOR =
            new Creator<wnItem>() {
                @Override
                public wnItem createFromParcel(Parcel source) {
                    return new wnItem(source);
                }

                @Override
                public wnItem[] newArray(int size) {
                    return new wnItem[0];
                }
            };

//        public wnItem() {
//        }

    public wnItem(Parcel in) {
        cur = in.readInt();
        tot = in.readInt();
    }

    public wnItem(int n2) {
        cur = 1;
        tot = n2;
    }

    @Override
    public String toString() {
        return String.format("%d/%d", cur, tot);
    }

    public int getCur() {
        return cur;
    }

    public void setCur(int cur) {
        if ((cur <= tot) && (cur > 0)) this.cur = cur;
    }

    public int getTot() {
        return tot;
    }
}
