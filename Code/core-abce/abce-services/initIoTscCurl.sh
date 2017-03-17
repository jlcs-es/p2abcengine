# Init IoT smartcard at user.
# This method is not specified in H2.2.
echo "Init IoT smartcard at user"
curl -X POST --header 'Content-Type: text/xml' 'http://localhost:9200/user/initIoTsmartcard/http%3A%2F%2Fticketcompany%2FMyFavoriteSoccerTeam%2Fissuance%3Aidemix?host=192.168.3.1&port=8888'


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

# First issuance protocol step - UI (first step for the user).
echo "Second issuance protocol step (first step for the user)"
curl -X POST --header 'Content-Type: text/xml' -d @uiIssuanceReturn.xml 'http://localhost:9200/user/issuanceProtocolStepUi/' > secondIssuanceMessage.xml

# Second issuance protocol step (second step for the issuer).
echo "Second issuance protocol step (second step for the issuer)"
curl -X POST --header 'Content-Type: text/xml' -d @secondIssuanceMessage.xml 'http://localhost:9100/issuer/issuanceProtocolStep/' > thirdIssuanceMessageAndBoolean.xml

# Extract issuance message.
curl -X POST --header 'Content-Type: text/xml' -d @thirdIssuanceMessageAndBoolean.xml 'http://localhost:9200/user/extractIssuanceMessage/' > thirdIssuanceMessage.xml

# Third issuance protocol step (second step for the user).
echo "Third issuance protocol step (second step for the user)"
curl -X POST --header 'Content-Type: text/xml' -d @thirdIssuanceMessage.xml 'http://localhost:9200/user/issuanceProtocolStep/' > fourthIssuanceMessageAndBoolean.xml

# Create presentation policy alternatives.
# This method is not specified in H2.2.
echo "Create presentation policy alternatives"
curl -X GET --header 'Content-Type: text/xml' -d @tutorial-resources/presentationPolicyAlternatives.xml 'http://localhost:9300/verification/createPresentationPolicy?applicationData=testData' > presentationPolicyAlternatives.xml

# Create presentation UI return.
# This method is not specified in H2.2.
echo "Create presentation UI return"
curl -X POST --header 'Content-Type: text/xml' -d @presentationPolicyAlternatives.xml 'http://localhost:9200/user/createPresentationToken/' > uiPresentationArguments.xml

echo "Select first posisble identity"
curl -X POST --header 'Content-Type: text/xml' -d @uiPresentationArguments.xml 'http://localhost:9600/identity/presentation' > uiPresentationReturn.xml

# Create presentation token.
# This method is not specified in H2.2.
echo "Create presentation token"
curl -X POST --header 'Content-Type: text/xml' -d @uiPresentationReturn.xml 'http://localhost:9200/user/createPresentationTokenUi/' > presentationToken.xml


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



# Verify presentation token against presentation policy.
echo "Verify presentation token against presentation policy"
# This method is not specified in H2.2.
curl -X POST --header 'Content-Type: text/xml' -d @presentationPolicyAlternativesAndPresentationToken.xml 'http://localhost:9300/verification/verifyTokenAgainstPolicy/' > presentationTokenDescription.xml

#
# Inspect presentation token.
echo "Inspect presentation token"
curl -X POST --header 'Content-Type: text/xml' -d @presentationToken.xml 'http://localhost:9400/inspector/inspect/' > inspectResult.xml

exit 0

#
# Find revocation handle and revoke the credential.
echo "Constructing revocation request"
revocationhandle=`cat fourthIssuanceMessageAndBoolean.xml | sed 's/^.*revocationhandle" DataType="xs:integer" Encoding="urn:abc4trust:1.0:encoding:integer:unsigned"\/><abc:AttributeValue xsi:type="xs:integer">//' | sed 's/<.*//'`
echo "${revocationhandle}"

echo '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<abc:AttributeList xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:abc="http://abc4trust.eu/wp2/abcschemav1.0"
    xmlns:idmx="http://zurich.ibm.com"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <abc:Attributes>
      <abc:AttributeUID>urn:abc4trust:1.0:attribute/NOT_USED</abc:AttributeUID>
      <abc:AttributeDescription Type="http://abc4trust.eu/wp2/abcschemav1.0/revocationhandle"
           DataType="xs:integer"
           Encoding="urn:abc4trust:1.0:encoding:integer:unsigned"/>
      <abc:AttributeValue xsi:type="xs:integer">' >> revocationAttributeList.xml
echo "${revocationhandle}" >> revocationAttributeList.xml
echo '</abc:AttributeValue></abc:Attributes></abc:AttributeList>' >> revocationAttributeList.xml

echo "Calling revocation authority"
curl -X POST --header 'Content-Type: text/xml' -d @revocationAttributeList.xml 'http://localhost:9500/revocation/revoke/http%3A%2F%2Fticketcompany%2Frevocation' > revokeReply.xml


#
# Check if the previous policy can be satisfied after revocation. Should return false.

echo "Checking if policy can be satisfied."
curl -X POST --header 'Content-Type: text/xml' -d @presentationPolicyAlternatives.xml 'http://localhost:9200/user/canBeSatisfied/' > canBeSatisfied.xml


#############

exit 0
