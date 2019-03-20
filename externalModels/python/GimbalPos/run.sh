FILE=$1
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python3.6 $DIR/gimbalYaml.py $FILE
