package com.mihal.flipcard;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by Davidovich_M on 2016-01-22.
 * Use for API levels < 21
 */
public class MyTTSbefore21 extends MyTTS {

    static final String TAG_1 = "logTTS";

    MyTTSbefore21(Context context, TextToSpeech.OnInitListener listener) {
        super(context, listener);
        Log.i(TAG_1, "MyTTSbefore21");
    }

    @Override
    public String getCurrentLanguage() {
        return super.getLanguage().getDisplayLanguage();
    }

    @Override
    public Locale getCurrentLocale() {
        return super.getLanguage();
    }

    @Override
    public int mySpeak(CharSequence text, int queueMode) {
        String toSpeak = String.valueOf(text);
        Log.i(TAG_1, "MyTTSbefore21.speak -> " + toSpeak);
        return super.speak(toSpeak, queueMode, null);
    }
}
