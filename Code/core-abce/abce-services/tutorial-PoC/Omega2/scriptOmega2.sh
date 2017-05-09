#!/bin/sh

#Stop script if an error occurs.
set -e

Rpi3IP="192.168.33.1"
myIP="192.168.33.96"


#################

# Setup System Parameters.

read -p "Press ENTER to init the system: must start the ThirdParty actors." -n1 -s


# Store credential specification at user.
# This method is not specified in H2.2.
echo "Store credential specification at user"
curl -X PUT --header 'Content-Type: text/xml' -d @tutorial-resources/credentialSpecificationVIPSoccerTicket.xml "http://$Rpi3IP:9200/user/storeCredentialSpecification/http%3A%2F%2FMyFavoriteSoccerTeam%2Ftickets%2Fvip" > storeCredentialSpecificationAtUserResponce.xml
 
read -p "Receive => systemparameters.xml <= from the Issuer. Then press any key to continue... " -n1 -s

# Store System parameters at User.
# This method is not specified in H2.2.
echo "Store System parameters at User"
curl -X POST --header 'Content-Type: text/xml' -d @systemparameters.xml "http://$Rpi3IP:9200/user/storeSystemParameters/" > storeSystemParametersResponceAtUser.xml

read -p "Receive => revocationAuthorityParameters.xml <= from the Revocation Service. Then press any key to continue... " -n1 -s

# Store Revocation Authority Parameters at user.
# This method is not specified in H2.2.
echo "Store Revocation Authority Parameters at user"
curl -X PUT --header 'Content-Type: text/xml' -d @revocationAuthorityParameters.xml "http://$Rpi3IP:9200/user/storeRevocationAuthorityParameters/http%3A%2F%2Fticketcompany%2Frevocation"  > storeRevocationAuthorityParametersAtUserResponce.xml

read -p "Receive => inspectorPublicKey.xml <= from the Inspector. Then press any key to continue... " -n1 -s

# Store Inspector Public Key at user.
# This method is not specified in H2.2.
echo "Store Inspector Public Key at user"
curl -X PUT --header 'Content-Type: text/xml' -d @inspectorPublicKey.xml "http://$Rpi3IP:9200/user/storeInspectorPublicKey/http%3A%2F%2Fticketcompany%2Finspection"  > storeInspectorPublicKeyAtUserResponce.xml

read -p "Receive => issuerParameters.xml <= from the Issuer. Then press any key to continue... " -n1 -s

# Store Issuer Parameters at user.
# This method is not specified in H2.2.
echo "Store Issuer Parameters at user"
curl -X PUT --header 'Content-Type: text/xml' -d @issuerParameters.xml "http://$Rpi3IP:9200/user/storeIssuerParameters/http%3A%2F%2Fticketcompany%2FMyFavoriteSoccerTeam%2Fissuance%3Aidemix"  > storeIssuerParametersAtUser.xml



#############################



# Init IoT smartcard at user.

# This method is not specified in H2.2.
echo "Init IoT smartcard at user"
curl -X POST --header 'Content-Type: text/xml' "http://$Rpi3IP:9200/user/initIoTsmartcard/http%3A%2F%2Fticketcompany%2FMyFavoriteSoccerTeam%2Fissuance%3Aidemix?host=$myIP&port=8888"




### ISSUANCE ###


read -p "Press ENTER to init the issuance: must start the Issuer." -n1 -s


read -p "Receive => issuanceMessageAndBoolean.xml <= from the Issuer. Then press any key to continue... " -n1 -s


# Extract issuance message.
curl -X POST --header 'Content-Type: text/xml' -d @issuanceMessageAndBoolean.xml "http://$Rpi3IP:9200/user/extractIssuanceMessage/" > firstIssuanceMessage.xml

# First issuance protocol step (first step for the user).
echo "First issuance protocol step for the user"
curl -X POST --header 'Content-Type: text/xml' -d @firstIssuanceMessage.xml "http://$Rpi3IP:9200/user/issuanceProtocolStep/" > issuanceReturn.xml


echo "Select first usable identity"
curl -X POST --header 'Content-Type: text/xml' -d @issuanceReturn.xml "http://$Rpi3IP:9600/identity/issuance/" > uiIssuanceReturn.xml

# First issuance protocol step - UI (first step for the user).
echo "Second issuance protocol step (first step for the user)"
curl -X POST --header 'Content-Type: text/xml' -d @uiIssuanceReturn.xml "http://$Rpi3IP:9200/user/issuanceProtocolStepUi/" > secondIssuanceMessage.xml


read -p "Send => secondIssuanceMessage.xml <= back to the Issuer. Then press any key to continue... " -n1 -s

read -p "Receive => thirdIssuanceMessageAndBoolean.xml <= from the Issuer. Then press any key to continue... " -n1 -s


# Extract issuance message.
curl -X POST --header 'Content-Type: text/xml' -d @thirdIssuanceMessageAndBoolean.xml "http://$Rpi3IP:9200/user/extractIssuanceMessage/" > thirdIssuanceMessage.xml

# Third issuance protocol step (second step for the user).
echo "Third issuance protocol step (second step for the user)"
curl -X POST --header 'Content-Type: text/xml' -d @thirdIssuanceMessage.xml "http://$Rpi3IP:9200/user/issuanceProtocolStep/" > fourthIssuanceMessageAndBoolean.xml



### PROVING ###

read -p "Press ENTER to init the proving: must start the Verifier." -n1 -s


read -p "Receive => presentationPolicyAlternatives.xml <= from the Verifier. Then press any key to continue... " -n1 -s

# Create presentation UI return.
# This method is not specified in H2.2.
echo "Create presentation UI return"
curl -X POST --header 'Content-Type: text/xml' -d @presentationPolicyAlternatives.xml "http://$Rpi3IP:9200/user/createPresentationToken/" > uiPresentationArguments.xml

echo "Select first posisble identity"
curl -X POST --header 'Content-Type: text/xml' -d @uiPresentationArguments.xml "http://$Rpi3IP:9600/identity/presentation" > uiPresentationReturn.xml

# Create presentation token.
# This method is not specified in H2.2.
echo "Create presentation token"
curl -X POST --header 'Content-Type: text/xml' -d @uiPresentationReturn.xml "http://$Rpi3IP:9200/user/createPresentationTokenUi/" > presentationToken.xml


# Setup presentationPolicyAlternativesAndPresentationToken.xml.
#This part is broken. the <?xml version...> of the ppa and pt needs to be stripped
presentationPolicy=`cat presentationPolicyAlternatives.xml | sed 's/^<?xml version="1.0" encoding="UTF-8" standalone="yes"?>//'`
presentationToken=`cat presentationToken.xml | sed 's/^<?xml version="1.0" encoding="UTF-8" standalone="yes"?>//'`
# echo "${presentationPolicy}"
# echo "${presentationToken}"
echo -n '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' > presentationPolicyAlternativesAndPresentationToken.xml
echo -n '<abc:PresentationPolicyAlternativesAndPresentationToken xmlns:abc="http://abc4trust.eu/wp2/abcschemav1.0" Version="1.0">' >> presentationPolicyAlternativesAndPresentationToken.xml
echo -n "${presentationPolicy}" >> presentationPolicyAlternativesAndPresentationToken.xml
echo -n "${presentationToken}" >> presentationPolicyAlternativesAndPresentationToken.xml
echo -n '</abc:PresentationPolicyAlternativesAndPresentationToken>' >> presentationPolicyAlternativesAndPresentationToken.xml



read -p "Send => presentationPolicyAlternativesAndPresentationToken.xml <= back to the Verifier. Then press any key to continue... " -n1 -s


### INSPECTION ###


read -p "Send => presentationToken.xml <= back to the Inspector. Then press any key to continue... " -n1 -s

