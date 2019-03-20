Information: This is a script which formats the Raw_images taken from SD Card. Nothing much, given a directory containing raw cube images, it formats the names so that by reading image name
a user can get sufficient details about the conditions under which image was taken.

- Basically renames image taking into consideration: selfieCube position, drone position, angle and so on.

Usage: - Place change_name.py in a file that contains raw images of the name DJI_XXXX.JPG
       - Run the script by specifying first image offset( Example if first image is DJI_5266, specify 5266) and the images would be formatted appropriately.

Command: $change_name.py 5266 (assuming the first DJI image offset is 5266)
