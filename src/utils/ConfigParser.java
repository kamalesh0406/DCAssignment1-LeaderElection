package utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigParser{
    public Integer UID;
    public String host;
    public Integer port;
    public Map<String, Integer> neighbors = new HashMap<>();
    public Map<Integer, String> uidHostMap = new HashMap<>();

    public ConfigParser(String[] args) {
        UID = Integer.parseInt(args[0]);
        host = args[1];
        port = Integer.parseInt(args[2]);

        List<String> neighborsDetails = Arrays.asList(args[3].split("_"));
        neighborsDetails = neighborsDetails.subList(1, neighborsDetails.size());

        for (String detail: neighborsDetails) {
            String[] detailArray = detail.split(",");
            System.out.println(detail);
            neighbors.put(detailArray[1], Integer.parseInt(detailArray[2]));
            uidHostMap.put(Integer.parseInt(detailArray[0]), detailArray[1]);
        }
    }

}