acpitool -B
counter=1
while [ $counter -le 10 ]
do
    time bash run.sh
    ((++counter))
done
acpitool -B
