package com.mihal.flipcard;

import android.content.Context;

/**
 * Created by Davidovich_M on 2016-01-26.
 */
public class MyPersist23 extends PersistFragment {
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            super.mCallback = (DataExchange) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement PersistExchange");
        }
    }
}
