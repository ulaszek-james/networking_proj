package project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class Server extends Thread {

    final private PeerConfiguration target;
    final private boolean passiveStart;
    final private Consumer<Message> messageQueue; // Call with messageQueue.accept(msg)
    final private PeerConfiguration localConfig; // The network configuration of this Server's Peer Process

    private Socket socket; // Used once the connection is established
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private static final int BACKLOG_SIZE = 10;
    private static final MessageFactory MESSAGE_FACTORY = new MessageFactory();
    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";

    /**
     * Constructor for the Server
     * @param target - the other peer which this server connects to
     * @param localConfig - a PeerConfiguration object capturing the network
     *                     configuration of this Server and its Peer process.
     * @param passiveStart - If true, the Server will simply wait for
     *                      connections on the proper port. If false,
     *                     the Server will create a new TCP connection.
     * @param messageQueue - a lambda expression passed by the peer which
     *                     creates this server, allowing the server to push
     *                     it messages
     */
    public Server(PeerConfiguration target,
                  PeerConfiguration localConfig,
                  boolean passiveStart,
                  Consumer<Message> messageQueue,
    ) {
        this.target = target;
        this.localConfig = localConfig;
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
        // Init Connections
        boolean setupSuccess = setupConnection(); // sets up this.socket
        if (!setupSuccess) {
            return; // Terminate and kill this thread
        }

        // Init streams
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            System.out.println("Exception thrown while initializing io streams");
            return;
        }

        boolean handshakeSuccess = doHandshake();
        if (!handshakeSuccess) {
            return; // Terminate and kill this thread
        }

        // Input listening loop
        try {
            while (!this.isInterrupted()) {
                String rawMessage = (String) in.readObject();
                messageQueue.accept(MESSAGE_FACTORY.makeMessage(rawMessage, target));
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

    /**
     * Runs the setup process to initialize
     * this Server's socket member. Either
     * calls passiveStart() or activeStart()
     * depending on the start mode of the server.
     * @return whether socket setup was successful
     */
    public boolean setupConnection() {
        if (passiveStart) {
            return passiveStart();
        }
        else {
            return activeStart();
        }
    }

    /**
     * Performs socket setup when the
     * server is in "passive start" mode.
     * Opens a server socket and waits for
     * a connection attempt from target.
     * @return whether socket setup was successful
     */
    public boolean passiveStart() {
        try {
            ServerSocket listener = new ServerSocket(localConfig.getPort(), BACKLOG_SIZE);
            socket = listener.accept(); // Blocks until successful
        }
        catch (IOException e) {
            System.out.println("Exception thrown while listening on" +
                    target.getPort());
            return false;
        }
        return true;
    }

    /**
     * Performs socket setup when the server is
     * in "active start" mode.
     * Finds the targets IP address and attempts
     * to open a connection
     * @return
     */
    public boolean activeStart() {
        InetAddress targetAddress;
        try {
            targetAddress = InetAddress.getByName(target.getHostname());
        }
        catch (UnknownHostException e) {
            System.out.println("Exception thrown while finding IP address with hostname"
                    + target.getHostname());
            return false;
        }

        try {
            socket = new Socket(targetAddress, target.getPort());
        }
        catch (IOException e) {
            System.out.println("Exception thrown while opening socket to " + target);
            return false;
        }
        return true;
    }

    public boolean sendMessage(Message message) {
        try {
            out.writeObject(message.serialize());
            out.flush();
        }
        catch (IOException e) {
            System.out.println("Exception caught while sending message to " + target);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean validateHandshake(String raw) {
        byte[] rawBytes = StringEncoder.stringToBytes(raw);

        // Check length of bytes is correct
        if (rawBytes.length < 32) {
            return false;
        }

        // Check header string is correct
        String header = raw.substring(0, 18);
        if (!header.equals(HANDSHAKE_HEADER)) {
            return false;
        }

        // Check zero bytes are correct
        byte zero = 0;
        for (int i = 18; i < 28; i++) {
            zero |= rawBytes[i];
        }
        if (zero != 0) {
            return false;
        }

        // Check that id is correct
        byte[] idBytes = {0,0,0,0};
        System.arraycopy(rawBytes, 28, idBytes, 0, 4);
        ByteBuffer buf = ByteBuffer.wrap(idBytes);
        int id = buf.getInt();
        return id == target.getId();
    }

    public boolean sendHandshake() {
        final byte[] zeroBytes = {0,0,0,0,0,0,0,0,0,0};
        byte[] idBytes = ByteBuffer.allocate(4).putInt(this.localConfig.getId()).array();
        String handshake = HANDSHAKE_HEADER
            + StringEncoder.bytesToString(zeroBytes)
            + StringEncoder.bytesToString(idBytes);
        try {
            out.writeObject(handshake);
            out.flush();
        }
        catch (IOException e) {
            System.out.println("IOException while sending handshake to " + target);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Perform the handshake with the target peer.
     * If this Server is in "passive start" mode,
     * it wait to receive a handshake from target
     * and then sends a handshake back.
     * If this Server is not in "active start" mode
     * (i.e. passiveStart=false), then it sends a
     * handshake first and then waits to receive
     * a handshake.
     * @return whether the handshake was successful
     */
    public boolean doHandshake() {
        try {
            if (passiveStart) {
                // Receive then send
                return validateHandshake((String)in.readObject()) && sendHandshake();
            }
            else {
                // Send then receive
                return sendHandshake() && validateHandshake((String)in.readObject());
            }
        }
        catch (IOException e) {
            System.out.println("IOException while reading handshake input with " + target);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            System.out.println("Class not found exception while handshaking with " + target);
        }
        return false;
    }
}
