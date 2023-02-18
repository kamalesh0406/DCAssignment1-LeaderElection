import java.io.Serializable;

public class Message implements Serializable {
    public String sender;

    Message(String sender) {
        this.sender = sender;
    }
}
