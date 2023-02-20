package bfstree;

import java.io.Serializable;

public class Message implements Serializable {

    int uid;
    int round;
    int parent=-2;

    // ACK = 1, Acknowledge
    // ACK = 0, NACK
    int ack;
    int treeLevel;
    boolean tree = false;
    boolean emptyMessage = false;

    // 1 = Message from Parent
    // -1 = Message from Child
    int msgType;

    public Message(int uid, boolean tree, int treeLevel){
        this.uid = uid;
        this.tree = tree;
        this.treeLevel = treeLevel;
    }
    public Message(int uid, int round, int treeLevel, boolean tree){
        this.uid = uid;
        this.round = round;
//        this.parent = parent;
//        this.ack = ack;
        this.treeLevel = treeLevel;
        this.tree = tree;
    }

    public Message(int uid, int round, int ack, int treeLevel, boolean tree){
        this.uid = uid;
        this.round = round;
//        this.parent = parent;
        this.ack = ack;
        this.treeLevel = treeLevel;
        this.tree = tree;
    }
}
