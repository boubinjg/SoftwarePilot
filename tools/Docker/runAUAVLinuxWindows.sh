gpu=
usage="bash runAUAVLinux.sh [-h][-g --no-gpu] -- runs a new Docker container from image

Flags: 
	-h, --help 	show this help text
	-g, --gpu	runs gpu compatible container
       	--no-gpu	runs gpu compatible container without gpu support"
while [ "$1" != "" ]; do
	case $1 in 
		-g | --gpu )	shift
				gpu=1;
				;;
		--no-gpu ) 	shift
				gpu=2;
				;;
		-h | --help ) 	echo "$usage"
				exit
				;;
		*)		echo "$usage"
				exit
				;;
	esac
	shift
done
if [ "$gpu" = "1" ]; then
    docker run -it -p 5117:5117/udp -p 12013:12013 auavlinux/nvidia
else 
    docker run -it -p 5117:5117/udp -p 12013:12013 auavlinux 
fi
