package project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MessageTests {

    private static final PeerConfiguration PEER1 = new PeerConfiguration(1011, "lin114-00.cise.ufl.edu",6008,false);
    private static final PeerConfiguration PEER2 = new PeerConfiguration(1012, "lin114-01.cise.ufl.edu",6008,false);

    private Message messageFromBytes(byte[] bytes, PeerConfiguration peer) {
        String raw = StringEncoder.bytesToString(bytes);
        MessageFactory factory = new MessageFactory();
        return factory.makeMessage(raw, peer);
    }

    @Test
    void testChokeMessageEquals() {
        ChokeMessage m1 = new ChokeMessage(PEER1);
        ChokeMessage m2 = new ChokeMessage(PEER1);
        ChokeMessage m3 = new ChokeMessage(PEER2);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
    }

    @Test
    void testMessageFactoryChokeMessage() {
        byte[] bytes = {0,0,0,5,0};
        Message received = messageFromBytes(bytes, PEER1);
        ChokeMessage expected = new ChokeMessage(PEER1);
        Assertions.assertTrue(received instanceof ChokeMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testChokeMessageSerialization() {
        byte[] bytes = {0,0,0,5,0};
        String expected = new String(bytes);
        ChokeMessage msg = new ChokeMessage(PEER1);
        String received = msg.serialize();
        Assertions.assertEquals(expected,received);

        MessageFactory factory = new MessageFactory();
        Message expectedMsg = factory.makeMessage(received, PEER1);
        Assertions.assertEquals(msg,expectedMsg);
    }

    @Test
    void testUnchokeMessageEquals() {
        UnchokeMessage m1 = new UnchokeMessage(PEER1);
        UnchokeMessage m2 = new UnchokeMessage(PEER1);
        UnchokeMessage m3 = new UnchokeMessage(PEER2);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
    }

    @Test
    void testMessageFactoryUnchokeMessage() {
        byte[] bytes = {0,0,0,5,1};
        Message received = messageFromBytes(bytes, PEER1);
        UnchokeMessage expected = new UnchokeMessage(PEER1);
        Assertions.assertTrue(received instanceof UnchokeMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testUnchokeMessageSerialization() {
        byte[] bytes = {0,0,0,5,1};
        String expected = new String(bytes);
        UnchokeMessage msg = new UnchokeMessage(PEER1);
        String received = msg.serialize();
        Assertions.assertEquals(expected,received);

        MessageFactory factory = new MessageFactory();
        Message expectedMsg = factory.makeMessage(received, PEER1);
        Assertions.assertEquals(msg,expectedMsg);
    }

    @Test
    void testInterestedMessageEquals() {
        InterestedMessage m1 = new InterestedMessage(PEER1);
        InterestedMessage m2 = new InterestedMessage(PEER1);
        InterestedMessage m3 = new InterestedMessage(PEER2);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
    }

    @Test
    void testMessageFactoryInterestedMessage() {
        byte[] bytes = {0,0,0,5,2};
        Message received = messageFromBytes(bytes, PEER1);
        InterestedMessage expected = new InterestedMessage(PEER1);
        Assertions.assertTrue(received instanceof InterestedMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testInterestedMessageSerialization() {
        byte[] bytes = {0,0,0,5,2};
        String expected = new String(bytes);
        InterestedMessage msg = new InterestedMessage(PEER1);
        String received = msg.serialize();
        Assertions.assertEquals(expected,received);

        MessageFactory factory = new MessageFactory();
        Message expectedMsg = factory.makeMessage(received, PEER1);
        Assertions.assertEquals(msg,expectedMsg);
    }

    @Test
    void testUninterestedMessageEquals() {
        UninterestedMessage m1 = new UninterestedMessage(PEER1);
        UninterestedMessage m2 = new UninterestedMessage(PEER1);
        UninterestedMessage m3 = new UninterestedMessage(PEER2);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
    }

    @Test
    void testMessageFactoryUninterestedMessage() {
        byte[] bytes = {0,0,0,5,3};
        Message received = messageFromBytes(bytes, PEER1);
        UninterestedMessage expected = new UninterestedMessage(PEER1);
        Assertions.assertTrue(received instanceof UninterestedMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testUninterestedMessageSerialization() {
        byte[] bytes = {0,0,0,5,3};
        String expected = new String(bytes);
        UninterestedMessage msg = new UninterestedMessage(PEER1);
        String received = msg.serialize();
        Assertions.assertEquals(expected,received);

        MessageFactory factory = new MessageFactory();
        Message expectedMsg = factory.makeMessage(received, PEER1);
        Assertions.assertEquals(msg,expectedMsg);
    }

    @Test
    void testHaveMessageEquals() {
        HaveMessage m1 = new HaveMessage(1, PEER1);
        HaveMessage m2 = new HaveMessage(1, PEER1);
        HaveMessage m3 = new HaveMessage(1, PEER2);
        HaveMessage m4 = new HaveMessage(2, PEER1);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
        Assertions.assertNotEquals(m1,m4);
        Assertions.assertNotEquals(m3,m4);
    }

    @Test
    void testMessageFactoryHaveMessage() {
        byte[] bytes = {0,0,0,9,4,0,0,0,1};
        Message received = messageFromBytes(bytes, PEER1);
        HaveMessage expected = new HaveMessage(1, PEER1);
        Assertions.assertTrue(received instanceof HaveMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testHaveMessageSerialization() {
        byte[] bytes = {0,0,0,9,4,0,0,0,1};
        String expected = new String(bytes);
        HaveMessage msg = new HaveMessage(1, PEER1);
        String received = msg.serialize();
        Assertions.assertEquals(expected,received);

        MessageFactory factory = new MessageFactory();
        Message expectedMsg = factory.makeMessage(received, PEER1);
        Assertions.assertEquals(msg,expectedMsg);
    }

    @Test
    void testRequestMessageEquals() {
        RequestMessage m1 = new RequestMessage(1, PEER1);
        RequestMessage m2 = new RequestMessage(1, PEER1);
        RequestMessage m3 = new RequestMessage(1, PEER2);
        RequestMessage m4 = new RequestMessage(2, PEER1);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
        Assertions.assertNotEquals(m1,m4);
        Assertions.assertNotEquals(m3,m4);
    }

    @Test
    void testMessageFactoryRequestMessage() {
        byte[] bytes = {0,0,0,9,6,0,0,0,1};
        Message received = messageFromBytes(bytes, PEER1);
        RequestMessage expected = new RequestMessage(1, PEER1);
        Assertions.assertTrue(received instanceof RequestMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testRequestMessageSerialization() {
        byte[] bytes = {0,0,0,9,6,0,0,0,1};
        String expected = new String(bytes);
        RequestMessage msg = new RequestMessage(1, PEER1);
        String received = msg.serialize();
        Assertions.assertEquals(expected,received);

        MessageFactory factory = new MessageFactory();
        Message expectedMsg = factory.makeMessage(received, PEER1);
        Assertions.assertEquals(msg,expectedMsg);
    }

    @Test
    void testPieceMessageEquals() {
        byte[] p1 = {0,0,0,0,0,0,0,1};
        byte[] p2 = {0,0,0,0,0,0,0,2};
        PieceMessage m1 = new PieceMessage(1, p1, PEER1);
        PieceMessage m2 = new PieceMessage(1, p1, PEER1);
        PieceMessage m3 = new PieceMessage(2, p1, PEER1);
        PieceMessage m4 = new PieceMessage(1, p2, PEER1);
        PieceMessage m5 = new PieceMessage(1, p1, PEER2);
        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
        Assertions.assertNotEquals(m1,m4);
        Assertions.assertNotEquals(m1,m5);
    }

    @Test
    void testMessageFactoryPieceMessage() {
        byte[] bytes = {0,0,0,17,7,0,0,0,1,0,0,0,0,0,0,0,1};
        byte[] piece = {0,0,0,0,0,0,0,1};
        Message received = messageFromBytes(bytes, PEER1);
        PieceMessage expected = new PieceMessage(1, piece, PEER1);
        Assertions.assertTrue(received instanceof PieceMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testPieceMessageSerialization() {
        byte[] bytes = {0,0,0,17,7,0,0,0,1,0,0,0,0,0,0,0,1};
        byte[] piece = {0,0,0,0,0,0,0,1};
        PieceMessage expectedObj = new PieceMessage(1, piece, PEER1);
        String expectedMsg = new String(bytes);
        Assertions.assertEquals(expectedObj.serialize(), expectedMsg);

        MessageFactory factory = new MessageFactory();
        Message receivedObj = factory.makeMessage(expectedObj.serialize(), PEER1);
        Assertions.assertEquals(receivedObj, expectedObj);
    }

    @Test
    void testBitfieldMessageEquals() {
        byte[] bitfield1 = {1,1};
        byte[] bitfield2 = {0,1};
        BitfieldMessage m1 = new BitfieldMessage(bitfield1, PEER1);
        BitfieldMessage m2 = new BitfieldMessage(bitfield1, PEER1);
        BitfieldMessage m3 = new BitfieldMessage(bitfield2, PEER1);
        BitfieldMessage m4 = new BitfieldMessage(bitfield1, PEER2);

        Assertions.assertEquals(m1,m2);
        Assertions.assertNotEquals(m1,m3);
        Assertions.assertNotEquals(m1,m4);
    }

    @Test
    void testBitfieldMessageBooleanEncoding() {
        // Test just positive bytes
        byte[] bitfield = {1,4}; // 0th bit and 10th bit
        boolean[] boolBitfield = new boolean[13]; // Pick an intermediate value
        boolBitfield[0] = true;
        boolBitfield[10] = true;
        BitfieldMessage m1 = new BitfieldMessage(bitfield, PEER1);
        BitfieldMessage m2 = new BitfieldMessage(boolBitfield, PEER1);
        Assertions.assertEquals(m1,m2);

        // Test positive and negative bytes
        bitfield[0] = (byte) 255; // Force the MSB of the 1st byte to 1
        boolBitfield[0] = false; // Flip this back to zero
        boolBitfield[7] = true; // Set MSB index to 1
        m1 = new BitfieldMessage(bitfield, PEER1);
        m2 = new BitfieldMessage(bitfield, PEER1);
        Assertions.assertEquals(m1,m2);
    }

    @Test
    void testBitfieldMessageHasPiece() {
        byte[] bitfield = {1,4}; // 0th bit and 10th bit
        BitfieldMessage m1 = new BitfieldMessage(bitfield, PEER1);
        Assertions.assertTrue(m1.hasPiece(0));
        Assertions.assertTrue(m1.hasPiece(10));

        bitfield[0] = (byte) 255; // Force the MSB of the 1st byte to 1
        bitfield[1] = (byte) 255; // Force the MSB of the 1st byte to 1
        m1 = new BitfieldMessage(bitfield, PEER1);
        Assertions.assertTrue(m1.hasPiece(7));
        Assertions.assertTrue(m1.hasPiece(15));
    }

    @Test
    void testMessageFactoryBitfieldMessage() {
        byte[] bytes = {0,0,0,7,5,4,(byte)255};
        byte[] bitfield = {4, (byte)255};
        Message received = messageFromBytes(bytes, PEER1);
        BitfieldMessage expected = new BitfieldMessage(bitfield, PEER1);
        Assertions.assertTrue(received instanceof BitfieldMessage);
        Assertions.assertEquals(expected, received);
    }

    @Test
    void testBitfieldMessageSerialization() {
        byte[] bytes = {0,0,0,7,5,4,(byte)255};
        byte[] bitfield = {4, (byte)255};
        Message received = new BitfieldMessage(bitfield, PEER1);
        String expectedMsg = StringEncoder.bytesToString(bytes);
        String receivedMsg = received.serialize();
        Assertions.assertEquals(receivedMsg, expectedMsg);
    }

    @Test
    void testMessageFactoryInvalidType() {
        byte[] bytes = {0,0,0,5,12};
        String raw = new String(bytes);
        MessageFactory factory = new MessageFactory();
        Assertions.assertThrows(IllegalArgumentException.class, () -> factory.makeMessage(raw, PEER1));
    }
    @Test
    void testMessageFactoryInvalidLength() {
        byte[] bytes = {0,0,0,12,0};
        String raw = new String(bytes);
        MessageFactory factory = new MessageFactory();
        Assertions.assertThrows(IllegalArgumentException.class, () -> factory.makeMessage(raw, PEER1));
    }

}
