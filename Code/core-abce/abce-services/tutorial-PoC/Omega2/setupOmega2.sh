#!/bin/sh

#Stop script if an error occurs
set -e

rm -rf tmp

mkdir -p tmp

cp -R tutorial-resources tmp/

cp scriptOmega2.sh tmp/
 
