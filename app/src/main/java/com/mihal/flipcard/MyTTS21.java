package com.mihal.flipcard;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by Davidovich_M on 2016-01-22.
 * Use for API level 21 and later.
 */
public class MyTTS21 extends MyTTS {

    static final String TAG_1 = "logTTS";

    @Override
    public String getCurrentLanguage() {
        return super.getVoice().getLocale().getDisplayLanguage();
    }

    @Override
    public Locale getCurrentLocale() {
        return super.getVoice().getLocale();
    }

    MyTTS21(Context context, TextToSpeech.OnInitListener listener) {
        super(context, listener);
    }

    @Override
    public int mySpeak(CharSequence text, int queueMode) {
        String toSpeak = String.valueOf(text);
        return super.speak(toSpeak, queueMode, null, "speak");
    }

}
