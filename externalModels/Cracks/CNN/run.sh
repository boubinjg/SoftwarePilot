file=$1
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3 $DIR/test.py -m $DIR/corn.model -l $DIR/lb.pickle -i $file
