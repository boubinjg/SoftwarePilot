DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
FEATS=$1
cd $DIR/python/Parser
java Parser features.properties Image=$DIR/../tmp/pictmp.jpg YAML=$DIR/../tmp/pictmp.yaml
