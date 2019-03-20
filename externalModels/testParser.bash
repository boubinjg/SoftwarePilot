#!/bin/bash
counter=1
date=0
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

while [ $counter -le 10 ]
do
    time bash runParser.sh
done
