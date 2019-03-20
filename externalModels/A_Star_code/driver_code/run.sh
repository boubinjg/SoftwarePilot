DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
java -cp .:../../../externalModels/weka.jar implement_astar_search
