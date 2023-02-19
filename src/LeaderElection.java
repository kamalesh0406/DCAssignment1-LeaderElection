import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class LeaderElection {
    public ConfigParser config;
    public BlockingQueue<Message> messageQueue;
    private Map<String, ObjectOutputStream> objectOutputStreams;
    public Integer currentRound;
    public Integer distance;
    public Integer maxDistance;
    public Integer maxUID;
    public Integer numRoundsWithSameUID;

    LeaderElection(ConfigParser config) {
        this.config = config;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.objectOutputStreams = new HashMap<>();
        this.distance = 0;
        this.maxDistance = 0;
        this.currentRound = 1;
        this.maxUID = config.UID;
        this.numRoundsWithSameUID = 0;
    }

    //Start the Leader Election Algorithm
    public void start() {   
        setupClientAndServerSockets();
        runLeaderElection();
    }

    public void setupClientAndServerSockets() {
        //We need a server to lisen to incoming messages.
        final SocketListener socketListener = new SocketListener(messageQueue, config.port);
        final Thread socketListenerThread = new Thread(socketListener);
        socketListenerThread.start();

        //We need to create clients for each neighbor to send messages to each of them.
        for (Entry<String, Integer> neighborDetails: config.neighbors.entrySet()) {
            Socket socketToNeighbor = null;

            // The neighbors server might not have started yet, so we perform a Retry storm with 
            // a 20ms delay until we get connected.
            while (true) {
                try {
                    socketToNeighbor = new Socket(neighborDetails.getKey(), neighborDetails.getValue());
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            try {
                final ObjectOutputStream neighborOutputStream = new ObjectOutputStream(socketToNeighbor.getOutputStream());
                objectOutputStreams.put(neighborDetails.getKey(), neighborOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runLeaderElection() {
        Boolean broadcastForCurrRound = false;
        Map<Integer, List<Message>> roundMessageBuffer = new HashMap<>();
        // We store the max UID we receive from neighbors here.
        Integer neighborMessagesCount = 0;
        Boolean maxUIDChanged = false;

        try {
            // We read messages from the messageQueue until we get the same distance value in three rounds.
            while (true) {
                if (broadcastForCurrRound == false) {
                    broadcastMessageToNeighbors(maxUID, currentRound, distance, maxDistance);
                    broadcastForCurrRound = true;
                }

                Message incomingMessage;
                // For the current round, check if we buffered messages for this round from previous rounds.
                // If we buffered messages, use them first otherwise choose messages from the message buffer.
                if (roundMessageBuffer.containsKey(currentRound) && roundMessageBuffer.get(currentRound).size()>0) {
                    incomingMessage = roundMessageBuffer.get(currentRound).remove(0);
                } else {
                    incomingMessage = messageQueue.poll();
                }

                if (incomingMessage == null) {
                    continue;
                }

                if (incomingMessage.round < currentRound) {
                    throw new Exception("The synchronizer does not work properly");
                // If the message is from a future round, we buffer it.
                } else if (incomingMessage.round > currentRound) {                    
                    List<Message> currentList = roundMessageBuffer.containsKey(incomingMessage.round) ? roundMessageBuffer.get(incomingMessage.round) : new ArrayList<>();
                    currentList.add(incomingMessage);

                    roundMessageBuffer.put(incomingMessage.round, currentList);
                } else {
                    neighborMessagesCount++;
                    if (incomingMessage.UID > maxUID) {
                        maxUID = incomingMessage.UID;
                        distance = incomingMessage.distance + 1;
                        maxDistance = incomingMessage.distance + 1;
                    } else {
                        // This is used to check if the maximum distance changed, i.e. increased in a given round.
                        if (maxDistance < incomingMessage.maxDistance) {
                            maxUIDChanged = true;
                        }
                        maxDistance = Math.max(maxDistance, incomingMessage.maxDistance);
                    }
                }

                if (neighborMessagesCount == config.neighbors.size()) {
                    if (!maxUIDChanged) {
                        numRoundsWithSameUID++;
                    } else {
                        //Reset this value to 0 since we need to wait for another 3 consecutive rounds to find the value.
                        numRoundsWithSameUID = 0;
                    }

                    // System.out.println("Completed Round " + currentRound);
                    // System.out.println("Max UID, Distance, Maximum Distance is " + maxUID + " " + distance + " " + maxDistance);

                    currentRound++;
                    broadcastForCurrRound = false;
                    neighborMessagesCount = 0;
                    maxUIDChanged = false;
                }

                if (numRoundsWithSameUID==3 && maxUID == config.UID) {
                    System.out.println("I am the leader with UID " + config.UID);
                    System.out.println("Completed Round " + currentRound);
                    System.out.println("Max UID, Distance, Maximum Distance is " + maxUID + " " + distance + " " + maxDistance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessageToNeighbors(Integer UID, Integer round, Integer distance, Integer maxDistance) {
        try {
            for (ObjectOutputStream outputStream: objectOutputStreams.values()) {
                outputStream.writeObject(new Message(UID, round, distance, maxDistance));
                outputStream.flush();
            }
        } catch (IOException e) {
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