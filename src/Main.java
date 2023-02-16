
public class Main {
    public static void main(String[] args) {
        ConfigParser config = new ConfigParser(args);

        System.out.println("UID " + config.uid);
        System.out.println("Host Name " + config.host);
        System.out.println("Port " + config.port);
    }
}
