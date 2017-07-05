package com.upyun;

public class Speex {

    public Speex() {
    }

    public void init(int size, int samplerate) {
        load();
        init(-8, size, samplerate);
    }

    private void load() {
        try {
            System.loadLibrary("speex");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public native void init(int level, int size, int samplerate);

    //    public native byte[] process(byte lin[], int offset, int size);
    public native int process(byte lin[], int offset, int size);

    public native void close();
}