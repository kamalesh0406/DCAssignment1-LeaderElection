package bfstree;


//import leaderelection.ConfigParser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BFSTree {

    //    public ConfigParser config;
    public BlockingQueue<BFSMessage> messageQueue;
    private Map<Integer, ObjectOutputStream> objectOutputStreams;
    public Integer currentRound;
    public Integer distance;
    public Integer maxDistance;
    public Integer maxUID;
    public Integer numRoundsWithSameUID;
    public int uid;
    List<Integer> neighbors;
    Map<Integer, Node> connectionStringMap;

    int nodePort;
    int distNode;

    public boolean leaderElected = false;

    /*BFSTree(ConfigParser config) {
        this.config = config;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.objectOutputStreams = new HashMap<>();
        this.distance = 0;
        this.maxDistance = 0;
        this.currentRound = 1;
        this.maxUID = config.UID;
        this.numRoundsWithSameUID = 0;
    }*/


    public BFSTree(int uid, List<Integer> finalNeighbors, Map<Integer, Node> connectionStringMap, int distNode, int nodePort) {
        this.uid = uid;
        this.neighbors = finalNeighbors;
        this.connectionStringMap = connectionStringMap;
        this.objectOutputStreams = new HashMap<>();
        this.currentRound = 1;
        this.distNode = distNode;
        this.nodePort = nodePort;
        this.messageQueue = new LinkedBlockingQueue<>();
    }

    public void start() {
        setupClientAndServerSockets();
        runAlgo(distNode);
    }

    public void setupClientAndServerSockets() {
        //We need a server to listen to incoming messages.
        final SocketListener socketListener = new SocketListener(messageQueue, nodePort);
        final Thread socketListenerThread = new Thread(socketListener);
        socketListenerThread.start();

        //We need to create clients for each neighbor to send messages to each of them.
        for (Map.Entry<Integer, Node> con : connectionStringMap.entrySet()) {

            Socket socketToNeighbor = null;

            // The neighbors server might not have started yet, so we perform a Retry storm with
            // a 20ms delay until we get connected.
            while (true) {
                try {
                    socketToNeighbor = new Socket(con.getValue().hostName, con.getValue().port);
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            System.out.println("Established connection with " + con.getKey());
            // from the config.txt file, get the neighbors, and it's corresponding output streams
            try {
                final ObjectOutputStream neighborOutputStream = new ObjectOutputStream(socketToNeighbor.getOutputStream());
                objectOutputStreams.put(con.getKey(), neighborOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Established connection with all neighbors");
        try {
            Thread.sleep(20);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

    }


    public void runAlgo(int distNode) {
        System.out.println("Initiating algorithm...");

        int parent = -2;
        Set<Integer> childNodes = new HashSet<>(Collections.emptyList());
        int currentNodeTreeLevel = 0;
        boolean parentFound = false;
//        int neighborMessagesCount = 0;
        // Hashmap to keeps track of the messages from each neighboring node
        // Key = node uid, value is the count of messages.
        // At the end, for every key value should be 2 , since each node would have received a message from parent and have received a message from neighboring node for ACK / NACK
        Map<Integer, Integer> ngbrMsg = new HashMap<>();
        int completedNodesCount = 0;


        // Only For distinguished node - broadcast the message
        // rest of the nodes don't broadcast any message. they will only do it after receiving the first message from parent
        if (distNode == uid) {
            System.out.println("I am distinguished Node. Broadcasting message to all.");
            parentFound = true;
            parent = -1;
            BFSMessage m = new BFSMessage(uid, currentRound, currentNodeTreeLevel, true);
            m.msgType = 1;
            System.out.println("Broadcasting message :" + m.toString());
            broadCastBFSMessage(m);
        }

        try {
            while (true) {

                BFSMessage incomingBFSMessage;

                incomingBFSMessage = messageQueue.poll();

                if (incomingBFSMessage == null) {
                    continue;
                }

                System.out.println("Processing Message :" + incomingBFSMessage);

                if (!incomingBFSMessage.completion) {

                    // incrementing the count of messages received from neighboring nodes
                    ngbrMsg.put(incomingBFSMessage.uid, ngbrMsg.getOrDefault(incomingBFSMessage.uid, 0) + 1);
                    currentNodeTreeLevel = incomingBFSMessage.treeLevel + 1;
                    BFSMessage replyBFSMessage = new BFSMessage(uid, true, currentNodeTreeLevel);
                    replyBFSMessage.round = currentRound;

                    // When a node receives a message,
                    // - if parent is not found, it accepts it as parent and sends ACK notification to that node
                    // - Also, once receiving the first message from parent, it will broadcast message to all the neighboring nodes
                    // - if parent is already found, it just sends ACK notification to that node.
                    if (!parentFound) {
                        parent = incomingBFSMessage.uid;
                        replyBFSMessage.ack = 1;
                        System.out.println("Accepting " + parent + " as parent.");

                        // broadcasting should ignore that parent node, as it will send ACK message to that node
                        System.out.println("Broadcasting Parent Message to neighbor nodes");
                        sendMessageToNode(replyBFSMessage, incomingBFSMessage.uid);
                        BFSMessage brdMsg = new BFSMessage(uid, true, currentNodeTreeLevel);
//                            currentRound++;
                        brdMsg.round = currentRound;
                        broadCastBFSMessage(brdMsg);
                        parentFound = true;
                    } else {
                        replyBFSMessage.ack = 0;
                        sendMessageToNode(replyBFSMessage, incomingBFSMessage.uid);
                    }


                    if (incomingBFSMessage.ack == 1) {
                        childNodes.add(incomingBFSMessage.uid);
                    }

                } else {
                    System.out.println("Child Termination notification received from " + incomingBFSMessage.uid);
                    completedNodesCount++;

                    // Once a node has received terminating message from all it's child
                    // send completion message
                    if (completedNodesCount == childNodes.size()) {
                        System.out.println("Sending termination message to parent");
                        BFSMessage complMsg = new BFSMessage(uid, true, true);
                        complMsg.round = currentRound;
                        // Root node will not send terminating message
                        if ( parent != -1)
                            sendMessageToNode(complMsg, parent);
                        break;
                    }

                }

                // for leaf node terminating message sending criteria
                // Before sending the terminating message, the node should have received 2 message from all it's neighboring nodes
                if (ngbrMsg.size() == objectOutputStreams.size()
                        && ngbrMsg.values().stream().reduce(0, Integer::sum) == 2 * objectOutputStreams.size()) {
                    System.out.println("Received message from all child nodes");

                    // If there are No child nodes, then initiate the termination
                    if (childNodes.size() == 0) {
                        System.out.println("Sending Terminating message to parent : " + parent);

                        BFSMessage complMsg = new BFSMessage(uid, true, true);
                        complMsg.round = currentRound;

                        sendMessageToNode(complMsg, parent);
                        break;
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (parent == -1)
            System.out.println("I am the root node");
        else
            System.out.println("Parent Node " + parent);
        System.out.println("Child Nodes : " + childNodes);

        System.out.println("End of program");
    }

    // used to send reply (ACK / NACK) specifically to the parent node
    // Also used to send completion status to parent
    public void sendMessageToNode(BFSMessage m, int uid) {

        try {
            ObjectOutputStream os = objectOutputStreams.get(uid);
            os.writeObject(m);
            os.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    // Will broadcast the message to all the neighbor nodes
    public void broadCastBFSMessage(BFSMessage m) {
        try {
            for (ObjectOutputStream outputStream : objectOutputStreams.values()) {
                outputStream.writeObject(m);
                outputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


class SocketListener implements Runnable {
    BlockingQueue<BFSMessage> queue;
    Integer port;

    SocketListener(BlockingQueue<BFSMessage> queue, Integer port) {
        this.queue = queue;
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(queue, socket);
                Thread clientHandlerThread = new Thread(clientHandler);
                clientHandlerThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

class ClientHandler implements Runnable {
    BlockingQueue<BFSMessage> queue;
    Socket socket;

    ClientHandler(BlockingQueue<BFSMessage> queue, Socket socket) {
        this.queue = queue;
        this.socket = socket;
    }

    @Override
    public void run() {
        ObjectInputStream inputStream = null;

        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Keep accepting message and add it to the Blocking Queue
        while (true) {
            try {
//                assert inputStream != null;
                Object inputObject = inputStream.readObject();
                BFSMessage inMessage = (BFSMessage) inputObject;
//                System.out.println("Received message " + inMessage.toString());
                queue.offer(inMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
