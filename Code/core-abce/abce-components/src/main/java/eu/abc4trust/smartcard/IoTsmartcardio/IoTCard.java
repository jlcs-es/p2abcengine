package eu.abc4trust.smartcard.IoTsmartcardio;

import eu.abc4trust.smartcard.IoTsmartcardio.util.IoTsmartcardConnection;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import java.io.IOException;

/**
 * Created by joseluis on 7/03/17.
 */
public class IoTCard extends Card {

    private IoTsmartcardConnection connection = null;

    public void connect(String host, int port) throws IOException {
        connection = new IoTsmartcardConnection(host, port);
        connection.connect();
    }

    public boolean isAlive() {
        return !connection.isClosed();
    }

    public IoTsmartcardConnection getConnection() {
        return connection;
    }

    @Override
    public ATR getATR() {
        return null;
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public IoTCardChannel getBasicChannel() {
        IoTCardChannel ch = new IoTCardChannel();
        ch.setCard(this);
        return ch;
    }

    @Override
    public IoTCardChannel openLogicalChannel() throws CardException {
        return null;
    }

    @Override
    public void beginExclusive() throws CardException {

    }

    @Override
    public void endExclusive() throws CardException {

    }

    @Override
    public byte[] transmitControlCommand(int i, byte[] bytes) throws CardException {
        return new byte[0];
    }

    @Override
    public void disconnect(boolean b) throws CardException {
        if(connection!=null && !connection.isClosed())
            try {
                connection.close();
            } catch (IOException e) {
                throw new CardException(e);
            }
    }
}
