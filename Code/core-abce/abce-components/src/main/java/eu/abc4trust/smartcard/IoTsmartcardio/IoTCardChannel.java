package eu.abc4trust.smartcard.IoTsmartcardio;

import eu.abc4trust.smartcard.IoTsmartcardio.util.IoTsmartcardConnection;

import javax.smartcardio.*;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by joseluis on 7/03/17.
 */
public class IoTCardChannel extends CardChannel {

    IoTCard card;

    public void setCard(IoTCard card) {
        this.card = card;
    }

    @Override
    public IoTCard getCard() {
        return card;
    }

    @Override
    public int getChannelNumber() {
        return 0;
    }

    @Override
    public ResponseAPDU transmit(CommandAPDU commandAPDU) throws CardException {
        byte commandBytes[] = IoTsmartcardConnection.generateAPDUprotocolRcvBytes(commandAPDU.getBytes());

        IoTsmartcardConnection sc = getCard().getConnection();

        try {
            sc.sendBytes(commandBytes);
            byte responseBytes[] = sc.recvBytes();
            ResponseAPDU responseAPDU = new ResponseAPDU(responseBytes);
            return responseAPDU;
        } catch (IOException e) {
            throw new CardException(e);
        }
    }

    @Override
    public int transmit(ByteBuffer byteBuffer, ByteBuffer byteBuffer1) throws CardException {
        return 0;
    }

    @Override
    public void close() throws CardException {

    }
}
