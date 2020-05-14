FILE=$1
PREF=$6
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 $DIR/Saturation.py $FILE $PREF
