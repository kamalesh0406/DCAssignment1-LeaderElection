package utils;

import java.io.Serializable;

public class LeaderElectionMessage implements Serializable {
    public Integer round;
    public Integer UID;
    public Integer distance;
    public Integer maxDistance;

    // used for Leader Election
    public LeaderElectionMessage(Integer UID, Integer round, Integer distance, Integer maxDistance) {
        this.round = round;
        this.UID = UID;
        this.distance = distance;
        this.maxDistance = maxDistance;
    }

}
