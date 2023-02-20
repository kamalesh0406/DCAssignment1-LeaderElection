package leaderelection;

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    public Integer round;
    public Integer UID;
    public Integer distance;
    public Integer maxDistance;

    public List<Integer> childNodes;
    public int treeLevel = 0;
    public int parentNode;

    // 0 = NACK, 1 = ACK
    public int ack;

    // default = false, so that all the messages will be processed as Tree
    public boolean tree = false;

    // used for Leader Election
    Message(Integer UID, Integer round, Integer distance, Integer maxDistance) {
        this.round = round;
        this.UID = UID;
        this.distance = distance;
        this.maxDistance = maxDistance;
    }

    // used for BFS Tree
    Message(Integer UID, Integer round, Integer distance, Integer maxDistance, int parent, int treeLevel, int ack) {
        this.round = round;
        this.UID = UID;
        this.distance = distance;
        this.maxDistance = maxDistance;
        this.parentNode = parent;
        this.treeLevel = treeLevel;
        this.ack = ack;
        this.tree = true;
    }

}
