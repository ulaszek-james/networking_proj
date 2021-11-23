package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.function.Consumer;

public class Server extends Thread {

    final private PeerConfiguration target;
    final private boolean passiveStart;
    final private Consumer<Message> messageQueue; // Call with messageQueue.accept(msg)

    private Socket socket; // Used once the connection is established
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static MessageFactory messageFactory = new MessageFactory();

    /**
     * Constructor for the Server
     * @param target - the other peer which this server connects to
     * @param passiveStart - If true, the Server will simply wait for
     *                      connections on the proper port. If false,
     *                     the Server will create a new TCP connection.
     * @param messageQueue - a lambda expression passed by the peer which
     *                     creates this server, allowing the server to push
     *                     it messages
     */
    public Server(PeerConfiguration target, boolean passiveStart, Consumer<Message> messageQueue) {
        this.target = target;
        this.passiveStart = passiveStart;
        this.messageQueue = messageQueue;
    }

    /**
     * The run method invoked by the JVM when
     * Server.start() is called.
     *
     * Note: DO NOT LAUNCH THE SERVER THREAD BY
     * DIRECTLY INVOKING Server.run(). Instead,
     * construct the Server object and call
     * Server.start().
     */
    public void run() {
        boolean setupSuccess = setupConnection(); // sets up this.socket
        if (!setupSuccess) {
            return; // Terminate and kill this thread
        }

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            System.out.println("Exception thrown while initializing io streams");
            return;
        }

        try {
            while (true) {
                String rawMessage = (String) in.readObject();
                messageQueue.accept(messageFactory.makeMessage(rawMessage, target));
            }
        }
        catch (IOException e) {
            System.out.println("IOException thrown in input loop; disconnecting with " + target);
        }
        catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException thrown in input loop");

        }
        finally {
            try {
                in.close();
                out.close();
                socket.close();
            }
            catch (IOException e) {
                System.out.println("IOException thrown while closing connections");
                System.out.println("Disconnecting with " + target);
            }
        }
    }

    public boolean setupConnection() {
        if (passiveStart) {
            return passiveStart();
        }
        else {
            return activeStart();
        }
    }

    public boolean passiveStart() {
        try {
            ServerSocket listener = new ServerSocket(target.getPort(), BACKLOG_SIZE);
            socket = listener.accept(); // Blocks until successful
        }
        catch (IOException e) {
            System.out.println("Exception thrown while listening on" +
                    target.getPort());
            return false;
        }
        return true;
    }

    public boolean activeStart() {
        InetAddress targetAddress = null;
        try {
            targetAddress = InetAddress.getByName(target.getHostname());
        }
        catch (UnknownHostException e) {
            System.out.println("Exception thrown while finding host with hostname"
                    + target.getHostname());
            return false;
        }

        try {
            socket = new Socket(targetAddress, target.getPort());
        }
        catch (IOException e) {
            System.out.println("Exception thrown while opening socket to " + target.toString());
            return false;
        }
        return true;
    }
}
