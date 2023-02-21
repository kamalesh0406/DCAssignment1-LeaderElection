package bfstree;

import java.io.Serializable;

public class BFSMessage implements Serializable {


    int uid;
    int round;
//    int parent=-2;

    // ACK = 1, Acknowledge
    // ACK = 0, NACK
    int ack;
    int treeLevel;
    boolean tree = false;
    boolean emptyMessage = false;

    // 1 = Message from Parent
    // -1 = Message from Child
    int msgType;
    public boolean completion;

    public BFSMessage(int uid, boolean tree, int treeLevel){
        this.uid = uid;
        this.tree = tree;
        this.treeLevel = treeLevel;
    }

    public BFSMessage(int uid, boolean tree, boolean completion){
        this.uid = uid;
        this.tree = tree;
        this.completion = completion;
    }

    public BFSMessage(int uid, int round, int treeLevel, boolean tree){
        this.uid = uid;
        this.round = round;
//        this.parent = parent;
//        this.ack = ack;
        this.treeLevel = treeLevel;
        this.tree = tree;
    }

    public BFSMessage(int uid, int round, int ack, int treeLevel, boolean tree){
        this.uid = uid;
        this.round = round;
//        this.parent = parent;
        this.ack = ack;
        this.treeLevel = treeLevel;
        this.tree = tree;
    }

    @Override
    public String toString() {
        return "BFSMessage{" +
                "uid=" + uid +
                ", round=" + round +
                ", ack=" + ack +
                ", treeLevel=" + treeLevel +
                ", tree=" + tree +
                ", emptyMessage=" + emptyMessage +
                ", msgType=" + msgType +
                ", completion=" + completion +
                '}';
    }
}
