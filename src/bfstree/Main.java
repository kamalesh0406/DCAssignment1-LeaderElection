package bfstree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args){
        String config = "/Users/vigneshthirunavukkarasu/Downloads/DCAssignment1-LeaderElection/config.txt";

        int uid = Integer.parseInt(args[0]);
        List<Integer> neighbours = new ArrayList<>();

        List<String> lines = getLinesFromConfigFile(config);
        int totalNodes = Integer.parseInt(lines.get(0));
        lines.remove(0);
        int nodePos = -1;

        for (int i=0; i< lines.size(); i++){
            System.out.println( i + " | " + lines.get(i));
        }


        for ( int i=0; i< lines.size(); i++) {
            if (lines.get(i).startsWith(args[0])) {
                nodePos = i;
                break;
            }
        }

        Map<Integer, Node> connectionMap = new HashMap<>();

        for ( String s : lines.subList(0, totalNodes-1)){
            String[] split = s.split(" ");
            connectionMap.put(Integer.parseInt(split[0]), new Node(split[1], Integer.parseInt(split[2])));
        }

        System.out.println("Node Pos : " + nodePos);
        String[] neig = lines.get(nodePos + totalNodes ).split(" ");

        neighbours = Arrays.stream(neig).map(Integer::valueOf).collect(Collectors.toList());
        /*System.out.println(Arrays.toString(neig));
        for ( String s: neig) {
            int n = Integer.parseInt(s);
            neighbours.add(n);
        }*/

        /*for (Map.Entry<Integer, Node> n: connectionMap.entrySet()) {
            int key = n.getKey();
            if (!neighbours.contains(key))
                connectionMap.remove(key);
        }*/

        List<Integer> finalNeighbours = neighbours;
        connectionMap.keySet().removeIf(k -> !finalNeighbours.contains(k));

        /*for ( Map.Entry<Integer, Node> n: connectionMap.entrySet())
            System.out.println(n.toString());*/


        connectionMap.forEach((key, value) -> System.out.println(value.toString()));

        BFSTree bfs = new BFSTree(uid, finalNeighbours, connectionMap);
    }

    public static List<String> getLinesFromConfigFile(String fileName){
        List<String> lines = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line = reader.readLine();

            while (line != null) {
                line = line.trim();
                if ( !line.startsWith("#") && !line.isEmpty())
                    lines.add(line);
                line = reader.readLine();

            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;
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
