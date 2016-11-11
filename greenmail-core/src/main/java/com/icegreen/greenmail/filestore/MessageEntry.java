package com.icegreen.greenmail.filestore;

/**
 * Created by saladin on 11/10/16.
 */
public class MessageEntry {
    public static final int MSG_ENTRY_SIZE = 36;

    private long uid;
    private int msgNum;
    private int flagBitSet;
    private long recDateMillis;

    // Instance variables used for SingleMboxFileForMultipleMessages
    private long positionInMboxFile;
    private int  lenInMboxFile;
    // Instance variables used for MultipleElmFilesForMultipleMessages
    private String shortFilename = null;

    protected MessageEntry() {
    }

    protected MessageEntry(long theUID) {
        this.uid = theUID;
    }


    public long getUid() {
        return this.uid;
    }
    public void setUid(long theUID) {
        this.uid = theUID;
    }

    public int getMsgNum() {
        return this.msgNum;
    }
    public void setMsgNum(int theNum) {
        this.msgNum = theNum;
    }

    public int getFlagBitSet() {
        return this.flagBitSet;
    }
    public void setFlagBitSet(int theFlagBitSet) {
        this.flagBitSet = theFlagBitSet;
    }

    public long getRecDateMillis() {
        return this.recDateMillis;
    }
    public void setRecDateMillis(long theRecDate) {
        this.recDateMillis = theRecDate;
    }


    public long getPositionInMboxFile() {
        return this.positionInMboxFile;
    }
    public void setPositionInMboxFile(long thePos) {
        this.positionInMboxFile = thePos;
    }

    public int getLenInMboxFile() {
        return this.lenInMboxFile;
    }
    public void setLenInMboxFile(int theLen) {
        this.lenInMboxFile = theLen;
    }

    public String getShortFileName() {
        return this.shortFilename;
    }
    public void setShortFileName(String theFilename) {
        this.shortFilename = theFilename;
    }

}
