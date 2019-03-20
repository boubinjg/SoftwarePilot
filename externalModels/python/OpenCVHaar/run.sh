FILE=$1
PREF=$2
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 $DIR/OpenCVCascade.py $FILE $PREF
