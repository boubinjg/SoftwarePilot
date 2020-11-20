DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
IMG=$1
cd $DIR/Parser
java Parser features.properties Image=../$IMG
