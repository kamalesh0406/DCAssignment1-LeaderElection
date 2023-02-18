import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class LeaderElection {
    public ConfigParser config;
    public BlockingQueue<Message> messageQueue;
    private Map<String, ObjectOutputStream> objectOutputStreams;

    LeaderElection(ConfigParser config) {
        this.config = config;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.objectOutputStreams = new HashMap<>();
    }

    //Start the Leader Election Algorithm
    public void start() {   
        //We need a server to lisen to incoming messages.
        SocketListener socketListener = new SocketListener(messageQueue, config.port);
        Thread socketListenerThread = new Thread(socketListener);
        socketListenerThread.start();

        //We need to create clients for each neighbor to send messages to each of them.
        for (Entry<String, Integer> neighborDetails: config.neighbors.entrySet()) {
            Socket socketToNeighbor = null;

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
            
            ObjectOutputStream neighborOutputStream;

            try {
                neighborOutputStream = new ObjectOutputStream(socketToNeighbor.getOutputStream());
                objectOutputStreams.put(neighborDetails.getKey(), neighborOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            for (Entry<String, ObjectOutputStream> entry: objectOutputStreams.entrySet()) {
                ObjectOutputStream outputStream = entry.getValue();
                System.out.println("Sending Message to " + entry.getKey());
                outputStream.writeObject(new Message(config.host));
                outputStream.flush();
            }

            System.out.println("Sent all messages");

            while (true) {
                Message incomingMessage = messageQueue.take();
                System.out.println("Read message from " + incomingMessage.sender);
            }
        } catch (Exception e) {
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

                System.out.println("Creating Client Connections");
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

                System.out.println("Message Sent From " + message.sender + " Added to Queue.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}