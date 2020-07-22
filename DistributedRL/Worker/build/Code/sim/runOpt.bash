file=$1
maxImg=$2
seed=$3
k=$4
Init=$5
Loc=$6
Serv=$7
Glob=$8
python3 SimOpt.py $file knndatasetGI knndatasetGILoc knndatasetGIGate knndatasetGIGlob $maxImg $seed $k $Init $Loc $Serv $Glob
