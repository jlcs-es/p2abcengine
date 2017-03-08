#!/bin/bash

cd ./issuer
konsole -e 'java -jar selfcontained-issuance-service.war 9100'
cd ../user
konsole -e 'java -jar selfcontained-user-service.war 9200'
cd ../verifier
konsole -e 'java -jar selfcontained-verification-service.war 9300'
cd ../inspector
konsole -e 'java -jar selfcontained-inspection-service.war 9400'
cd ../revocation
konsole -e 'java -jar selfcontained-revocation-service.war 9500'
cd ../identity
konsole -e 'java -jar selfcontained-identity-service.war 9600'
cd ..
