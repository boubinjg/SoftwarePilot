DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
NDRONES=$1

python3 SimOpt.py $NDRONES Models/
