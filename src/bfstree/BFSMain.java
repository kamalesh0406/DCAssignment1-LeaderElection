package bfstree;

import java.util.*;

// CLI Arguments
// BFSMain.java <port> <uid> <neiguid_host:port,neiguid_host:port> <dist_node>
// BFSMain.java 3999 3 4_localhost:4500,5_localhost:4501 4
public class BFSMain {
    public static void main(String[] args){

        Map<Integer, Node> connectionMap = new HashMap<>();
        List<Integer> neighbours = new ArrayList<>();

        int port = Integer.parseInt(args[0]);
        int uid = Integer.parseInt(args[1]);

        List<String> connections = Arrays.asList(args[2].split(","));
        int distNode = Integer.parseInt(args[3]);


        System.out.println(connections);
        for ( String c : connections){
            int id = Integer.parseInt(c.split("_")[0]);
            String[] s = c.split("_")[1].split(":");
            String host = s[0];

            int p = Integer.parseInt(s[1]);

            neighbours.add(id);
            connectionMap.put(id, new Node(host, p));
        }

        System.out.println("Port : " + port);
        System.out.println("Node uid : " + uid);

        System.out.println("Neighbor nodes : " + neighbours);


        BFSTree bfs = new BFSTree(uid, neighbours, connectionMap, distNode, port);
        bfs.start();
    }

}


class Node {

    int uid;
    String hostName;
    int port;

    Node(String hostName, int port){
        this.hostName = hostName;
        this.port = port;
    }

    @Override
    public String toString() {
        return "Node{" +
                "uid=" + uid +
                ", hostName='" + hostName + '\'' +
                ", port=" + port +
                '}';
    }
}
