IP=$1
CMD=$2
./coap-client coap://$IP:5117/cr -m put -e $CMD
