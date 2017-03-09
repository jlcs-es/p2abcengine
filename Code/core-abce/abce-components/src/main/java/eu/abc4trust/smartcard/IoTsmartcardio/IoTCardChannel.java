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

    public ResponseAPDU manageGetResponse(ResponseAPDU response) throws CardException {

        if( response.getSW1() == 0x61 ){
            // Send GETRESPONSE and append the data bytes.
            CommandAPDU getResponseCommand = new CommandAPDU(new byte[] { (byte)0xBC, (byte)0xC0, 0, 0, (byte) response.getSW2() });
            ResponseAPDU getResponseResponse = this.transmit(getResponseCommand);
            byte alldata[] = new byte[response.getData().length + getResponseResponse.getData().length + 2];
            System.arraycopy(response.getData(), 0, alldata, 0, response.getData().length);
            System.arraycopy(getResponseResponse.getData(), 0, alldata, response.getData().length, getResponseResponse.getData().length);
            alldata[alldata.length-2] = (byte) getResponseResponse.getSW1();
            alldata[alldata.length-1] = (byte) getResponseResponse.getSW2();
            ResponseAPDU newResponse = new ResponseAPDU(alldata);
            return newResponse;
        } else {
            return response;
        }
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
            responseAPDU = manageGetResponse(responseAPDU);
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
