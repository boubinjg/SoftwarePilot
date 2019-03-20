DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
FEATS=$1
cd $DIR/Parser
java Parser features.properties Image=../test.JPG
