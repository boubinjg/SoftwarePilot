FILE=$1
TOP=$2
RIGHT=$3
BOTTOM=$4
LEFT=$5
PREF=$6
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 $DIR/Saturation.py $FILE $TOP $RIGHT $BOTTOM $LEFT $PREF
