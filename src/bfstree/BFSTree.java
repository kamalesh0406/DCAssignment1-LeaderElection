package bfstree;


import leaderelection.ConfigParser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BFSTree {

    public ConfigParser config;
    public BlockingQueue<Message> messageQueue;
    private Map<Integer, ObjectOutputStream> objectOutputStreams;
    public Integer currentRound;
    public Integer distance;
    public Integer maxDistance;
    public Integer maxUID;
    public Integer numRoundsWithSameUID;
    public int uid;
    List<Integer> neighbors;
    Map<Integer, Node> connectionStringMap;

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

    BFSTree(){

    }

    public BFSTree(int uid, List<Integer> finalNeighbors, Map<Integer, Node> connectionStringMap) {
        this.uid = uid;
        this.neighbors = finalNeighbors;
        this.connectionStringMap = connectionStringMap;
        this.objectOutputStreams = new HashMap<>();
        this.currentRound = 1;
    }

    public void start(){
        setupClientAndServerSockets();
    }

    public void setupClientAndServerSockets() {
        //We need a server to listen to incoming messages.
        final SocketListener socketListener = new SocketListener(messageQueue, config.port);
        final Thread socketListenerThread = new Thread(socketListener);
        socketListenerThread.start();

        //We need to create clients for each neighbor to send messages to each of them.
        for (Map.Entry<Integer, Node> con: connectionStringMap.entrySet()) {

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

            // from the config.txt file, get the neighbors and it's corresponding output streams
            try {
                final ObjectOutputStream neighborOutputStream = new ObjectOutputStream(socketToNeighbor.getOutputStream());
                objectOutputStreams.put(con.getKey(), neighborOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void runAlgo(int distNode){

        boolean broadcastForCurrRound = false;

        // key = round_no, Value - message queue list
        Map<Integer, List<Message>> roundMessageBuffer = new HashMap<>();
        int parent = -2;
        List<Integer> childNodes = Collections.emptyList();
        int currentNodeTreeLevel = 0;
        boolean parentFound = false;
        int neighborMessagesCount = 0;


        // For distinguished node - broadcast the message, parentFound = true, parent = -1
        // rest of the nodes don't broadcast any message.
        // they will only do it after receiving the first message
        if ( distNode == uid ) {
            parentFound = true;
            parent = -1;
            Message m = new Message(uid, currentRound, currentNodeTreeLevel, true);
            m.msgType = 1;
            broadCastBFSMessage(m);
        }

        try{
            while (true){

//                if (!broadcastForCurrRound) {
//                    Message m = new Message(uid, currentRound, currentNodeTreeLevel, true);
//                    m.msgType = 1;
//                    broadCastBFSMessage(m);
//                    broadcastForCurrRound = true;
//                }

                Message incomingMessage;

                // For the current round, check if we buffered messages for this round from previous rounds.
                // If we buffered messages, use them first otherwise choose messages from the message buffer.
                if (roundMessageBuffer.containsKey(currentRound) && roundMessageBuffer.get(currentRound).size()>0) {
                    incomingMessage = roundMessageBuffer.get(currentRound).remove(0);
                } else {
                    incomingMessage = messageQueue.poll();
                }

                if ( incomingMessage == null) {
                    System.out.println("Null message. skipping and reading next message");
                    continue;
                }

                if (incomingMessage.round < currentRound) {
                    throw new Exception("The synchronizer does not work properly.");
                    // If the message is from a future round, we buffer it.
                } else if (incomingMessage.round > currentRound) {
                    List<Message> currentList = roundMessageBuffer.containsKey(incomingMessage.round) ? roundMessageBuffer.get(incomingMessage.round) : new ArrayList<>();
                    currentList.add(incomingMessage);

                    roundMessageBuffer.put(incomingMessage.round, currentList);
                }else{

                    // process the message, can be from
                    // - non-child
                    // - child - ACK / NACK
                    if ( !incomingMessage.emptyMessage) {

                        // do not increment the count for empty message
                        // empty messages are sent just to have the round count
                        neighborMessagesCount++;
                        currentNodeTreeLevel = incomingMessage.treeLevel + 1;
                        Message replyMessage = new Message(uid, true, currentNodeTreeLevel);

                        if ( !parentFound ){
                            parent = incomingMessage.uid;
                            replyMessage.ack = 0;
                            System.out.println("Accepting " + parent + " as parent.");
                            System.out.println("Broadcasting Message to neighbor nodes");
                            Message brdMsg = new Message(uid, true, currentNodeTreeLevel);
                            broadCastBFSMessage(brdMsg);
                        }else{
                            replyMessage.ack = 1;
                        }

                        sendACKMessage(replyMessage, incomingMessage.uid);
                    }
                }

                if ( neighborMessagesCount == objectOutputStreams.size()){
                    System.out.println("Received message from all child nodes");
                    System.out.println("Parent Node " + parent);
                    System.out.println("Child Nodes : " + childNodes);
                    currentRound++;
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // used to send reply (ACK / NACK) specifically to the parent node
    public void sendACKMessage(Message m, int uid){
        try{
            ObjectOutputStream os = objectOutputStreams.get(uid);
            os.writeObject(m);
            os.flush();
            os.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }

    // Will broadcast the message to all the neighbor nodes
    public void broadCastBFSMessage(Message m){
        try{
            for (ObjectOutputStream outputStream: objectOutputStreams.values()) {
                outputStream.writeObject(m);
                outputStream.flush();
                outputStream.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}









class SocketListener implements Runnable {
    BlockingQueue<Message> queue;
    Integer port;

    SocketListener(BlockingQueue<Message> queue, Integer port) {
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
    BlockingQueue<Message> queue;
    Socket socket;

    ClientHandler(BlockingQueue<Message> queue, Socket socket) {
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
                Object inputObject = inputStream.readObject();
                Message message = (Message) inputObject;
                queue.offer(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
