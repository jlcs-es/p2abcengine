#!/bin/sh

#Stop script if an error occurs
set -e

rm -rf target/user_*
rm -rf tmp/user/user_storage
rm -rf tmp/user/abce-services
rm -rf tmp/*.xml

mkdir -p tmp
mkdir -p tmp/user
mkdir -p tmp/identity

cp target/selfcontained-user-service.war tmp/user
cp target/selfcontained-identity-service.war tmp/identity


echo "cd tmp/user/ && java -jar selfcontained-user-service.war 9200"
echo "cd tmp/identity && -jar selfcontained-identity-service.war 9200"
