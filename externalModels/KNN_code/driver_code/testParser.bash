#!/bin/bash
counter=1
date=0
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

date=$(date +%s%3N)
echo $date
acpitool -B
while [ $counter -le 100 ]
do
    #java Parser testFeatures Image=$DIR/../../../../Code/testImgs/Img$counter.jpg YAML=../../../tmp/pictmp.yaml
    bash run.sh

    ((counter++))
    ndate=$(date +%s%3N)
    secs=$(expr $ndate - $date)
    echo $secs
    date=$ndate
done
acpitool -B
date=date-$(date +%s%3N)
