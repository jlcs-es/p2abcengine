#!/bin/sh

#Stop script if an error occurs.
set -e


read -p "Press ENTER to init the system: must start the ThirdParty actors." -n1 -s


# Setup System Parameters.
echo "Setup System Parameters"
curl -X POST --header 'Content-Type: text/xml' 'http://localhost:9100/issuer/setupSystemParameters/?keyLength=1024' > systemparameters.xml

# Store credential specification at issuer.
echo "Store credential specification at issuer"
curl -X PUT --header 'Content-Type: text/xml' -d @tutorial-resources/credentialSpecificationVIPSoccerTicket.xml 'http://localhost:9100/issuer/storeCredentialSpecification/http%3A%2F%2FMyFavoriteSoccerTeam%2Ftickets%2Fvip' > storeCredentialSpecificationAtIssuerResponce.xml

####################################################

# Store credential specification at verifier.
# This method is not specified in H2.2.
echo "Store credential specification at verifier"
curl -X PUT --header 'Content-Type: text/xml' -d @tutorial-resources/credentialSpecificationVIPSoccerTicket.xml 'http://localhost:9300/verification/storeCredentialSpecification/http%3A%2F%2FMyFavoriteSoccerTeam%2Ftickets%2Fvip' > storeCredentialSpecificationAtVerifierResponce.xml

# Store System parameters at Revocation Authority.
# This method is not specified in H2.2.
echo "Store System parameters at Revocation Authority"
curl -X POST --header 'Content-Type: text/xml' -d @systemparameters.xml 'http://localhost:9500/revocation/storeSystemParameters/' > storeSystemParametersResponceAtRevocationAutority.xml

######################################################

# Store System parameters at verifier.
# This method is not specified in H2.2.
echo "Store System parameters at Verifier"
curl -X POST --header 'Content-Type: text/xml' -d @systemparameters.xml 'http://localhost:9300/verification/storeSystemParameters/' > storeSystemParametersResponceAtVerifier.xml

# Setup Revocation Authority Parameters.
echo "Setup Revocation Authority Parameters"
curl -X POST --header 'Content-Type: text/xml' -d @tutorial-resources/revocationReferences.xml 'http://localhost:9500/revocation/setupRevocationAuthorityParameters?keyLength=1024&uid=http%3A%2F%2Fticketcompany%2Frevocation' > revocationAuthorityParameters.xml

# Store Revocation Authority Parameters at issuer.
# This method is not specified in H2.2.
echo "Store Revocation Authority Parameters at issuer"
curl -X PUT --header 'Content-Type: text/xml' -d @revocationAuthorityParameters.xml 'http://localhost:9100/issuer/storeRevocationAuthorityParameters/http%3A%2F%2Fticketcompany%2Frevocation'  > storeRevocationAuthorityParameters.xml

#####################################################



# Store Revocation Authority Parameters at verifier.
# This method is not specified in H2.2.
echo "Store Revocation Authority Parameters at verifier"
curl -X PUT --header 'Content-Type: text/xml' -d @revocationAuthorityParameters.xml 'http://localhost:9300/verification/storeRevocationAuthorityParameters/http%3A%2F%2Fticketcompany%2Frevocation'  > storeRevocationAuthorityParametersAtVerifierResponce.xml

##

# Store System parameters at Inspector.
# This method is not specified in H2.2.
echo "Store System parameters at inspector"
curl -X POST --header 'Content-Type: text/xml' -d @systemparameters.xml 'http://localhost:9400/inspector/storeSystemParameters/' > storeSystemParametersResponceAtInspector.xml

# Store credential specification at Inspector.
# This method is not specified in H2.2.
echo "Store credential specification at inspector"
curl -X PUT --header 'Content-Type: text/xml' -d @tutorial-resources/credentialSpecificationVIPSoccerTicket.xml 'http://localhost:9400/inspector/storeCredentialSpecification/http%3A%2F%2FMyFavoriteSoccerTeam%2Ftickets%2Fvip' > storeCredentialSpecificationAtInspectorResponce.xml


# Generate Inspector Public Key
# This method is not specified in H2.2.
echo "Generating Inspector Public Key"
curl -X POST --header 'Content-Type: text/xml' 'http://localhost:9400/inspector/setupInspectorPublicKey?keyLength=1024&cryptoMechanism=idemix&uid=http%3A%2F%2Fticketcompany%2Finspection' > inspectorPublicKey.xml



############################################



# Store Inspector Public Key at verifier.
# This method is not specified in H2.2.
echo "Store Inspector Public Key at verifier"
curl -X PUT --header 'Content-Type: text/xml' -d @inspectorPublicKey.xml 'http://localhost:9300/verification/storeInspectorPublicKey/http%3A%2F%2Fticketcompany%2Finspection'  > storeInspectorPublicKeyAtVerifierResponce.xml


##

# Setup issuer parameters.
echo "Setup issuer parameters"
curl -X POST --header 'Content-Type: text/xml' -d @tutorial-resources/issuerParametersInput.xml 'http://localhost:9100/issuer/setupIssuerParameters/' > issuerParameters.xml


###################################

# Store Issuer Parameters at verifier.
# This method is not specified in H2.2.
echo "Store Issuer Parameters at verifier"
curl -X PUT --header 'Content-Type: text/xml' -d @issuerParameters.xml 'http://localhost:9300/verification/storeIssuerParameters/http%3A%2F%2Fticketcompany%2FMyFavoriteSoccerTeam%2Fissuance%3Aidemix'  > storeIssuerParametersAtVerifier.xml
 
 
 
##############################################

### ISSUANCE

read -p "Press ENTER to init the issuance: must start the Issuer." -n1 -s


# Init issuance protocol (first step for the issuer).
echo "Init issuance protocol"
curl -X POST --header 'Content-Type: text/xml' -d @tutorial-resources/issuancePolicyAndAttributes.xml 'http://localhost:9100/issuer/initIssuanceProtocol/' > issuanceMessageAndBoolean.xml

read -p "Send => issuanceMessageAndBoolean.xml <= back to the IoT device. Then press any key to continue... " -n1 -s

read -p "Receive => secondIssuanceMessage.xml <= from the IoT device. Then press any key to continue... " -n1 -s

# Second issuance protocol step (second step for the issuer).
echo "Second issuance protocol step (second step for the issuer)"
curl -X POST --header 'Content-Type: text/xml' -d @secondIssuanceMessage.xml 'http://localhost:9100/issuer/issuanceProtocolStep/' > thirdIssuanceMessageAndBoolean.xml

read -p "Send => thirdIssuanceMessageAndBoolean.xml <= back to the IoT device. Then press any key to continue... " -n1 -s


### PROVING

read -p "Press ENTER to init the proving: must start the Verifier." -n1 -s


# Create presentation policy alternatives.
# This method is not specified in H2.2.
echo "Create presentation policy alternatives"
curl -X GET --header 'Content-Type: text/xml' -d @tutorial-resources/presentationPolicyAlternatives.xml 'http://localhost:9300/verification/createPresentationPolicy?applicationData=testData' > presentationPolicyAlternatives.xml

read -p "Send => presentationPolicyAlternatives.xml <= back to the IoT device. Then press any key to continue... " -n1 -s



read -p "Receive => presentationPolicyAlternativesAndPresentationToken.xml <= from the IoT device. Then press any key to continue... " -n1 -s


# Verify presentation token against presentation policy.
echo "Verify presentation token against presentation policy"
# This method is not specified in H2.2.
curl -X POST --header 'Content-Type: text/xml' -d @presentationPolicyAlternativesAndPresentationToken.xml 'http://localhost:9300/verification/verifyTokenAgainstPolicy/' > presentationTokenDescription.xml



### INSPECTION

read -p "Receive => presentationToken.xml <= from the IoT device. Then press any key to continue... " -n1 -s


# Inspect presentation token.
echo "Inspect presentation token"
curl -X POST --header 'Content-Type: text/xml' -d @presentationToken.xml 'http://localhost:9400/inspector/inspect/' > inspectResult.xml


