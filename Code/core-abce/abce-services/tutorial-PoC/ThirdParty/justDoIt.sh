#!/bin/bash

rm -r tmp/
./setupThirdParty.sh
cd tmp/
konsole --tabs-from-file ./startServices.tabs
#sleep 10
#./scriptThirdParty.sh
