#!/bin/bash
FILE=$1
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 $DIR/3dPositionYaml.py $FILE
