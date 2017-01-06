package com.mihal.flipcard;

/**
 * Created by Davidovich_M on 2017-01-06.
 */

interface Observable {
    void registerObserver(Observer o);
    void notifyObservers();
}
