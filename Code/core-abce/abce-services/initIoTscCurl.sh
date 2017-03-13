# Init IoT smartcard at user.
# This method is not specified in H2.2.
echo "Init IoT smartcard at user"
curl -X POST --header 'Content-Type: text/xml' 'http://localhost:9200/user/initIoTsmartcard/http%3A%2F%2Fticketcompany%2FMyFavoriteSoccerTeam%2Fissuance%3Aidemix?host=localhost&port=8888'


#####


# Init issuance protocol (first step for the issuer).
echo "Init issuance protocol"
curl -X POST --header 'Content-Type: text/xml' -d @tutorial-resources/issuancePolicyAndAttributes.xml 'http://localhost:9100/issuer/initIssuanceProtocol/' > issuanceMessageAndBoolean.xml

# Extract issuance message.
curl -X POST --header 'Content-Type: text/xml' -d @issuanceMessageAndBoolean.xml 'http://localhost:9200/user/extractIssuanceMessage/' > firstIssuanceMessage.xml

# First issuance protocol step (first step for the user).
echo "First issuance protocol step for the user"
curl -X POST --header 'Content-Type: text/xml' -d @firstIssuanceMessage.xml 'http://localhost:9200/user/issuanceProtocolStep/' > issuanceReturn.xml

echo "Select first usable identity"
curl -X POST --header 'Content-Type: text/xml' -d @issuanceReturn.xml 'http://localhost:9600/identity/issuance/' > uiIssuanceReturn.xml
