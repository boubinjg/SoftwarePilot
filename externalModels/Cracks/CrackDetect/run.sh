FILE=$1
PREFIX=$6
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3.6 $DIR/Cracks.py --fname $FILE --meta_file $DIR/model.meta --CP_dir $DIR/.
