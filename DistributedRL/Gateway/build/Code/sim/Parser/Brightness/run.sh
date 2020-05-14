FILE=$1
PREFIX=$6
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 $DIR/Brightness.py $FILE $PREFIX
