acpitool -B
counter=1
while [ $counter -le 100 ]
do
    time bash run.sh
    ((++counter))
done
acpitool -B
