acpitool -B
counter=0
while [ $counter -le 10 ]
do
    bash runParser.sh features.properties
    ((++counter))
done
acpitool -B
