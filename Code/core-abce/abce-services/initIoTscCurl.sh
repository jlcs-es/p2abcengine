# Init IoT smartcard at user.
# This method is not specified in H2.2.
echo "Init IoT smartcard at user"
curl -X POST --header 'Content-Type: text/xml' 'http://localhost:9200/user/initIoTsmartcard/http%3A%2F%2Fticketcompany%2FMyFavoriteSoccerTeam%2Fissuance%3Aidemix?host=localhost&port=8888'


