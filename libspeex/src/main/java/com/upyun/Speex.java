package com.upyun;

public class Speex {

    /* quality 
     * 1 : 4kbps (very noticeable artifacts, usually intelligible) 
     * 2 : 6kbps (very noticeable artifacts, good intelligibility) 
     * 4 : 8kbps (noticeable artifacts sometimes) 
     * 6 : 11kpbs (artifacts usually only noticeable with headphones) 
     * 8 : 15kbps (artifacts not usually noticeable) 
     */
    private static final int DEFAULT_COMPRESSION = 8;

    public Speex() {
    }

    public void init(int size) {
        load();
        open(DEFAULT_COMPRESSION,size);
    }

    private void load() {
        try {
            System.loadLibrary("speex");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public native int open(int compression,int size);

    public native int getFrameSize();

//    public native int process(short lin[], int offset, int size);

    public native int process(byte lin[], int offset, int size);

    public native void close();

}