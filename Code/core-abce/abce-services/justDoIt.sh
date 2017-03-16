#!/bin/bash

rm -r tmp/
./setupTutorial.sh
cd tmp/
konsole --tabs-from-file ./startServices.tabs
sleep 10
./setupBeforeCreateSmartcard.sh
