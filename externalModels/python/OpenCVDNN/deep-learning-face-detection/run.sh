FILE=$1
python3 detect_faces.py --image ../$FILE --prototxt deploy.prototxt.txt --model res10_300x300_ssd_iter_140000.caffemodel
