
public class Main {
    public static void main(String[] args) {
        ConfigParser config = new ConfigParser(args);

        System.out.println("UID " + config.UID);
        System.out.println("Host Name " + config.host);
        System.out.println("Port " + config.port);

        LeaderElection leaderElection = new LeaderElection(config);
        leaderElection.start();
    }
}
