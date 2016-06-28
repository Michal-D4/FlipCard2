package com.mihal.flipcard;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.SparseArray;

/**
 * Created by Davidovich_M on 2016-06-28.
 */
class dictEntry implements Parcelable {
    private int word_id;
    private final String Ru;
    private final String En;
    private String Transcript;
    private final int file_id;
    private final boolean Learned;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(word_id);
        dest.writeInt(file_id);
        dest.writeInt(Learned ? 1 : 0);
        dest.writeString(Ru);
        dest.writeString(En);
        dest.writeString(Transcript);
    }

    public static final Creator<dictEntry> CREATOR =
            new Creator<dictEntry>() {
                @Override
                public dictEntry createFromParcel(Parcel source) {
                    return new dictEntry(source);
                }

                @Override
                public dictEntry[] newArray(int size) {
                    return new dictEntry[0];
                }
            };

    dictEntry(Parcel in) {
        word_id = in.readInt();
        file_id = in.readInt();
        Learned = (in.readInt() == 1);
        Ru = in.readString();
        En = in.readString();
        Transcript = in.readString();
    }

    // used when read from file
    public dictEntry(String[] ww, int fileId) {
        word_id = 0;
        file_id = fileId;
        Ru = ww[0];
        En = ww[1];
        if (ww.length == 3) Transcript = ww[2];
        else Transcript = "";
        Learned = false;
    }

    // used when read from DB
    public dictEntry(int wid, String ru, String en, String tr, boolean learned, int fid) {
        word_id = wid;
        Ru = ru;
        En = en;
        Transcript = tr;
        Learned = learned;
        file_id = fid;
    }

    public void setWord_id(int word_id) {
        this.word_id = word_id;
    }

    public boolean isLearned() {
        return Learned;
    }

    public int getWord_id() {
        return word_id;
    }

    public int getFile_id() {
        return file_id;
    }

    private CharSequence applySpans(String s) {
        class pair {
            int a;
            int b;

            public pair(int a, int b) {
                this.a = a;
                this.b = b;
            }
        }
        SparseArray<pair> ind = new SparseArray<>();
        SpannableStringBuilder ss = new SpannableStringBuilder();
        int beg;
        int end = -1;
        int i = 0;
        while ((beg = s.indexOf('{', end)) > -1) {
            ss.append(s.subSequence(end + 1, beg));
            if ((end = s.indexOf('}', beg)) == -1) {
                end = beg;
                break;
            }
            int i1 = ss.length();
            ss.append(s.subSequence(beg + 1, end));
            int i2 = ss.length();
            ind.append(i++, new pair(i1, i2));
        }
        ss.append(s.subSequence(end + 1, s.length()));

        for (int j = 0; j < ind.size(); j++) {
            pair be = ind.get(j);
            ss.setSpan(new RelativeSizeSpan(1.5f), be.a, be.b,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(Color.RED), be.a, be.b,
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return ss;
    }

    public CharSequence getRu() {
        return applySpans(Ru);
    }

    public CharSequence getEn() {
        return applySpans(En);
    }

    public CharSequence getTranscript() {
        return applySpans(Transcript);
    }

    public void setTranscript(String transcript) {
        Transcript = transcript;
    }

    public String getRuStr() {
        return Ru;
    }

    public String getEnStr() {
        return En;
    }

    public String getTranscriptStr() {
        return Transcript;
    }
}
