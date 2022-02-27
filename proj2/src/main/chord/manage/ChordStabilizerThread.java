package main.chord.manage;

import main.chord.*;

public class ChordStabilizerThread implements Runnable {
    private Chord chord;

    public ChordStabilizerThread(Chord chord) {
        this.chord = chord;
    }

    @Override
    public void run() {}
}
