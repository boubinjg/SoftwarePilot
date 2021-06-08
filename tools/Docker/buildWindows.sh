gpu=
usage="bash build.sh [-h][-g n] -- builds the Docker image with source code and all needed libraries

Flags:
	-h, --help	show this help text
	-g, --gpu	compiles Nvidia GPU compatible version"
	
while [ "$1" != "" ]; do
	case $1 in 
		-g | --gpu )	shift
				gpu=1
				;;
		-h | --help )	echo "$usage"
				exit
				;;
		*)		echo "$usage"
		       		exit
				;;	
	esac
	shift
done
if [ "$gpu" = "1" ]; then
	docker build -t auavlinux/nvidia -f build/Dockerfile.nvidia build/. --rm
else
	docker build -t auavlinux -f build/Dockerfile build/. --rm
fi
