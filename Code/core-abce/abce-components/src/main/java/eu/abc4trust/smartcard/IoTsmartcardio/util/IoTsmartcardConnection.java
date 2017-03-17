package eu.abc4trust.smartcard.IoTsmartcardio.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joseluis on 6/03/17.
 */
public class IoTsmartcardConnection {

    private String host;
    private int port;
    private Socket socket;


    public IoTsmartcardConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public void connect() throws IOException {
        socket = new Socket(host, port);
    }

    public boolean isClosed() {
        return socket.isClosed();
    }


    public static byte[] generateAPDUprotocolRcvBytes(byte[] apduBytes) {
        short len = (short)apduBytes.length;
        short offset = 3;
        byte res[] = new byte[offset+len];

        res[0] = 0x01;
        res[1] = (byte) (len >> 8);
        res[2] = (byte) len;
        System.arraycopy(apduBytes, 0, res, offset, len);

        return res;
    }


    public void sendBytes(byte[] bytes) throws IOException {
        DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
        outStream.write(bytes);
    }



    public byte[] recvBytes() throws IOException {
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        // Read length of incoming APDU response
        byte lenbytes[] = new byte[2];
        int r = inputStream.read(lenbytes, 0, 2);
        ByteBuffer bb = ByteBuffer.wrap(lenbytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        short len = bb.getShort();

        // Read APDU
        byte apdu[] = new byte[len];
        inputStream.read(apdu, 0, len);

        return apdu;
    }

    public void close() throws IOException {
        /*
        FF : command finishLoop
        */
        byte finish[] = new byte[] {(byte)0xff};
        this.sendBytes(finish);
        socket.close();
    }

}
