from PIL import Image
import face_recognition
import os
import argparse

'''files = os.listdir("NoFaces/")
print(files)

for img in files:'''

#parser = argparse.ArgumentParser();
#parser.add_argument('img');
#args= parser.parse_args();
#print(args.img)

image = face_recognition.load_image_file("../tmp/pictmp.jpg");

face_locations = face_recognition.face_locations(image, number_of_times_to_upsample=0, model="cnn");
#print(img)
print(face_locations);
'''for face_location in face_locations:
    top, right, bottom, left = face_locations[0];
    face_image = image[top:bottom, left:right]
    pil_image = Image.fromarray(face_image);
    pil_image.show()'''
