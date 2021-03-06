package project;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class Server {


    final private PeerConfiguration target;
    final private boolean passiveStart;
    final private Consumer<Message> messageSink; // Destination of incoming messages - call with messageSink.accept(msg)
    final private PeerConfiguration self; // The network configuration of this Server's Peer Process

    private Socket socket; // Used once the connection is established
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private static final int BACKLOG_SIZE = 10;
    private static final MessageFactory MESSAGE_FACTORY = new MessageFactory();
    private static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
    // BLOCKING_MESSAGE_SEND determines whether calls to Server::sendMessage are blocking to the caller
    private static final boolean BLOCKING_SEND_MESSAGE = false;


    private InHandler inputReader;
    private MessageLogger sLog;

    /**
     * Constructor for the Server
     * @param self - a PeerConfiguration object capturing the network
     *                     configuration of this Server and its Peer process.
     * @param target - the other peer which this server connects to
     * @param passiveStart - If true, the Server will simply wait for
     *                      connections on the proper port. If false,
     *                     the Server will create a new TCP connection.
     * @param messageSink - a lambda expression passed by the peer which
     *                     creates this server, allowing the server to push
     *                     it messages
     */
    public Server(PeerConfiguration self,
                  PeerConfiguration target,
                  boolean passiveStart,
                  Consumer<Message> messageSink
    ) {
        this.target = target;
        this.self = self;
        this.passiveStart = passiveStart;
        this.messageSink = messageSink;
        this.socket = null;
        sLog = new MessageLogger(self.getId());
    }

    public Server(PeerConfiguration self,
                  PeerConfiguration target,
                  boolean passiveStart,
                  MessageLogger logger,
                  Consumer<Message> messageSink
    ) {
        this.target = target;
        this.self = self;
        this.passiveStart = passiveStart;
        this.messageSink = messageSink;
        this.socket = null;
        sLog = logger;
    }

    public Server(PeerConfiguration self,
                  PeerConfiguration target,
                  Socket socket,
                  boolean passiveStart,
                  MessageLogger logger,
                  Consumer<Message> messageSink
    ) {
        this.self = self;
        this.target = target;
        this.socket = socket;
        this.passiveStart = passiveStart;
        this.sLog = logger;
        this.messageSink = messageSink;
    }

    /**
     * The start method which performs connection
     * setup and launches the input handling thread.
     */
    public boolean start() {
        // Init Connections
        if (!passiveStart) {
            boolean setupSuccess = setupConnection(); // sets up this.socket
            if (!setupSuccess) {
                System.out.printf("Could not setup connection from self (%d) to target %d%n", self.getId(), target.getId());
                return false; // Terminate and kill this thread
            }
            else {
                //LOG -- proper TCP connection
                sLog.logTCP(self.getId(), target.getId());
            }
        }
        else {
            assert socket != null; // Should use the constructor to set it!
        }

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            System.out.println("Failure setting up input streams with target " + target);
        }

        if (!doHandshake()) {
            System.out.printf("Could not successfully complete handshake from self (%d) to target %d%n",
                    self.getId(), target.getId());
            return false; // Terminate and kill this thread
        }

        this.inputReader = new InHandler(in, target, messageSink);
        inputReader.start(); // Starts background process
        return true;
    }

    public boolean stop() {
        if (inputReader != null && inputReader.isAlive()) {
            inputReader.interrupt();
        }

        try {
            in.close();
            out.close();
            socket.close();
        }
        catch (IOException e) {
            System.out.println("Server::shutdown: Could not close socket successfully");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static class InHandler extends Thread {

        private final ObjectInputStream in;
        private final PeerConfiguration target;
        private final Consumer<Message> messageSink;

        public InHandler(ObjectInputStream in,
                       PeerConfiguration target,
                       Consumer<Message> messageSink
        ) {
            this.in = in;
            this.target = target;
            this.messageSink = messageSink;
        }

        public void run() {
            try {
                while (!this.isInterrupted()) {
                    //String rawMessage = (String) in.readObject();
                    byte[] lenBytes = {0,0,0,0};
                    if (in.read(lenBytes, 0, 4) == -1) {
                        break;
                    }
                    int len = ByteBuffer.wrap(lenBytes).getInt();
                    byte[] content = new byte[len];
                    System.arraycopy(lenBytes, 0, content, 0, 4);
                    if (in.read(content, 4, len-4) == -1) {
                        break;
                    }
                    String rawMessage = StringEncoder.bytesToString(content);
                    messageSink.accept(MESSAGE_FACTORY.makeMessage(rawMessage, target));
                }
            }
            catch (EOFException e) {
                System.out.println("Server::InHandler::run EOFException thrown. Stopping input from " + target);
                e.printStackTrace();
            }
            catch (IOException e) {
                System.out.println("Server::InHandler::run IOException thrown. Stopping input from " + target);
                e.printStackTrace();
            }
//            catch (ClassNotFoundException e) {
//                System.out.println("Server::InHandler::run ClassNotFoundException thrown. Stopping input from " + target);
//            }
            this.interrupt();
            /*
             * Do not close this.in because it is owned by Server
             */
        }
    }

    private static class OutHandler extends Thread {

        private final Message message;
        private final ObjectOutputStream out;

        public OutHandler(Message message, ObjectOutputStream out) {
            this.message = message;
            this.out = out;
        }

        public void run() {
            try {
                //out.writeObject(message.serialize());
                out.write(message.serializeToBytes());
                out.flush();
            }
            catch (IOException e) {
                System.out.println("Server::OutHandler::run IOException thrown. Message delivery failed");
                e.printStackTrace();
                this.interrupt(); // Interrupts itself to signal caller
            }
        }
    }

    public boolean sendMessage(Message message) {
        OutHandler handler = new OutHandler(message, out);
        handler.start();
        if (BLOCKING_SEND_MESSAGE) {
            // Block the caller until the send process completes
            try {
                handler.join();
            }
            catch (InterruptedException e) {
                System.out.println("Server::OutHandler::run interrupted; Server::sendMessage may have failed");
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Runs the setup process to initialize
     * this Server's socket member. Either
     * calls passiveConnect() or activeConnect()
     * depending on the start mode of the server.
     * @return whether socket setup was successful
     */
    private boolean setupConnection() {
        Socket conn = null;
        if (!passiveStart) {
            InetAddress address = getTargetAddress();
            if (address != null) {
                conn = activeConnect(address, target.getPort());
            }
        }
        else {
            throw new UnsupportedOperationException("Do not call Server::setupConnection when in passive start mode");
        }

        if (conn == null) {
            return false;
        }
        else {
            socket = conn;
            return true;
        }
    }

    /**
     * Performs socket setup when the
     * server is in "passive start" mode.
     * Opens a server socket and waits for
     * a connection attempt from target.
     * @param port -- the port for this server to listen on
     * @return a socket to the target, or null
     */
    // WARNING - THIS IS NOW DEPRECATED
    public static Socket passiveConnect(int port) {
        Socket conn = null;
        try {
            ServerSocket listener = new ServerSocket();
            listener.setReuseAddress(true);
            listener.bind(new InetSocketAddress(port), BACKLOG_SIZE);
            conn = listener.accept(); // Blocks until successful
        }
        catch (IOException e) {
            System.out.println("Exception thrown while listening on " +
                    port);
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * Performs socket setup when the server is
     * in "active start" mode.
     * Finds the targets IP address and attempts
     * to open a connection
     * @return a socket to the target, or null
     */
    public static Socket activeConnect(InetAddress address, int port) {
        Socket conn = null;
        try {
            conn = new Socket(address, port);
        }
        catch (IOException e) {
            System.out.println("Exception thrown while opening socket to " + address + ":" + port);
        }
        return conn;
    }

    private InetAddress getTargetAddress() {
        InetAddress targetAddress = null;
        try {
            targetAddress = InetAddress.getByName(target.getHostname());
        }
        catch (UnknownHostException e) {
            System.out.println("Exception thrown while finding IP address with hostname"
                    + target.getHostname());
        }
        return targetAddress;
    }

    public boolean validateHandshake(String raw) {
        byte[] rawBytes = StringEncoder.stringToBytes(raw);

        // Check length of bytes is correct
        if (rawBytes.length < 32) {
            System.out.printf("Bad handshake received from %d - bad length%n", target.getId());
            return false;
        }

        // Check header string is correct
        String header = raw.substring(0, 18);
        if (!header.equals(HANDSHAKE_HEADER)) {
            System.out.printf("Bad handshake received from %d - bad header%n", target.getId());
            return false;
        }

        // Check zero bytes are correct
        byte zero = 0;
        for (int i = 18; i < 28; i++) {
            zero |= rawBytes[i];
        }
        if (zero != 0) {
            System.out.printf("Bad handshake received from %d - bad zero bytes%n", target.getId());
            return false;
        }

        // Check that id is correct
        byte[] idBytes = {0,0,0,0};
        System.arraycopy(rawBytes, 28, idBytes, 0, 4);
        ByteBuffer buf = ByteBuffer.wrap(idBytes);
        int id = buf.getInt();
        if (id == target.getId()) {
            return true;
        }
        else {
            System.out.printf("Bad handshake received from %d - bad id of %d%n", target.getId(), id);
            return false;
        }
    }

    private boolean sendHandshake() {
        try {
            //out.writeObject(makeHandshakeMessage());
            out.write(StringEncoder.stringToBytes(makeHandshakeMessage()));
            out.flush();
        }
        catch (IOException e) {
            System.out.println("IOException while sending handshake to " + target);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String makeHandshakeMessage() {
        final byte[] zeroBytes = {0,0,0,0,0,0,0,0,0,0};
        byte[] idBytes = ByteBuffer.allocate(4).putInt(this.self.getId()).array();
        return HANDSHAKE_HEADER
                + StringEncoder.bytesToString(zeroBytes)
                + StringEncoder.bytesToString(idBytes);
    }

    /**
     * Perform the handshake with the target peer.
     * If this Server is in "passive start" mode,
     * it waits to receive a handshake from target
     * and then sends a handshake back.
     * If this Server is not in "active start" mode
     * (i.e. passiveStart=false), then it sends a
     * handshake first and then waits to receive
     * a handshake.
     * @return whether the handshake was successful
     */
    private boolean doHandshake() {
        try {
            if (passiveStart) {
                // Receive then send
                byte[] handshakeBytes = new byte[32];
                if (in.read(handshakeBytes, 0, 32) == -1) {
                    return false;
                }
                if (!validateHandshake(StringEncoder.bytesToString(handshakeBytes))) {
                    return false;
                }
                return sendHandshake();
            }
            else {
                // Send then receive
                if (!sendHandshake()) {
                    return false;
                }
                byte[] handshakeBytes = new byte[32];
                if (in.read(handshakeBytes, 0, 32) == -1) {
                    return false;
                }
                return validateHandshake(StringEncoder.bytesToString(handshakeBytes));
            }
        }
        catch (IOException e) {
            System.out.println("IOException while reading handshake input with " + target);
            e.printStackTrace();
        }
//        catch (ClassNotFoundException e) {
//            System.out.println("Class not found exception while handshaking with " + target);
//        }
        return false;
    }
}
