package com.mihal.flipcard;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Created by Davidovich_M on 2016-01-22.
 */
public abstract class MyTTS extends TextToSpeech{

    MyTTS(Context context, TextToSpeech.OnInitListener listener) {
        super(context, listener);
    }

    abstract public String getCurrentLanguage();
    abstract public Locale getCurrentLocale();
    abstract public int mySpeak(CharSequence text, int queueMode);
}
