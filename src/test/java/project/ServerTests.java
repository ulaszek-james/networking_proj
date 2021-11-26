package project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class ServerTests {

    private static final PeerConfiguration PEER1 = new PeerConfiguration(1011, "lin114-00.cise.ufl.edu",6008,false);
    private static final PeerConfiguration PEER2 = new PeerConfiguration(1012, "lin114-01.cise.ufl.edu",6008,false);

    @Test
    void testMakeHandshakeMessage() {
        Server server = new Server(PEER1, PEER2, false, (Message m) -> {});

        String msg = server.makeHandshakeMessage();

        Assertions.assertEquals(msg.substring(0,18), "P2PFILESHARINGPROJ");
        byte[] zeros =  {0,0,0,0,0,0,0,0,0,0};
        Assertions.assertArrayEquals(StringEncoder.stringToBytes(msg.substring(18,28)), zeros);
        byte[] idBytes = {0,0,0,0};
        System.arraycopy(StringEncoder.stringToBytes(msg), 28, idBytes, 0, 4);
        Assertions.assertEquals(ByteBuffer.wrap(idBytes).getInt(), PEER1.getId());
    }

    @Test
    void testValidateHandshakeMessage() {
        Server server1 = new Server(PEER1, PEER2, false, (Message m) -> {});
        Server server2 = new Server(PEER2, PEER1, false, (Message m) -> {});
        String msg = server1.makeHandshakeMessage();

        // makeHandshakeMessage is correct by previous test, so
        // only need to test validation
        Assertions.assertTrue(server2.validateHandshake(msg));
    }
}
