package eu.abc4trust.smartcard.IoTsmartcardio;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import java.io.IOException;

/**
 * Created by joseluis on 7/03/17.
 */
public class IoTCardTerminal extends CardTerminal {

    IoTCard card;
    private String host;
    private int port;


    public IoTCardTerminal(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getName() {
        return "IoT Card Terminal - Host: " + host + " - Port: " + port;
    }

    @Override
    public Card connect(String s) throws CardException {
        if(card != null && card.isAlive()) {
            return card;
        } else {
            card = new IoTCard();
            try {
                card.connect(host, port);
            } catch (IOException e) {
                throw new CardException(e);
            }
            return card;
        }
    }

    @Override
    public boolean isCardPresent() throws CardException {
        return (card != null && card.isAlive());
    }

    @Override
    public boolean waitForCardPresent(long l) throws CardException {
        return false;
    }

    @Override
    public boolean waitForCardAbsent(long l) throws CardException {
        return false;
    }
}
