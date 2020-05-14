Information: This function calculates the latitude and longitude of the drone (image captured position) using the existing image.
We use pillow library from python to achieve this.

Working: - First we provide the image from command line.
         - The function calculates the exif_data from teh image.
         - appropriate calculation of lat/long happens and we return that to calling function.

Execution Command: python3 output.py Image_name.JPG
                  Example: python3 output.py DJI_1957.JPG

Ouput: (Latitude, Longitude values) would be returned
