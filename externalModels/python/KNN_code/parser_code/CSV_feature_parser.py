#####################################################################################################
#                                                                                                   #
# @Author Naveen T R                                                                                #
# 06/09/2018                                                                                        #
# This script parses the feature_file and creates CSV file with values from each feature.           #
# This means, the features are segregated according to the cube postion.                            #
# Uses: - The result would later help while running K-Nearest neighbors algorithm.                  #
#                                                                                                   #
# Usage : $python3 CSV_feature_parser.py drone_features.txt                                         #
# Expected ouput format: File of the format CSV_features_compiled.                                  #
# - This file contains features that belong to that particular X-Y-Z position.                      #
#                                                                                                   #
#####################################################################################################

#Import the required libraries.
import sys
import csv
import os
import re
import os.path

#Accept the feature_filename from user.
file = str(sys.argv[1])


#Function to write to a specific file.
def write_to_file(filename,name,count):
    i = 0
    # If the file doesn't exist, create the file in write mode and insert/write the contents to the file.
    #if not(os.path.isfile(filename)):
    #print("count=",count)
    i = 0
    if not(os.path.isfile(filename)):
        with open(filename, "w") as file:
            for l in name:
                file.write(l)
                if i!= count-1:
                    file.write(',')
                i+=1
            #file.write("\n")

    else:
        data_read = ""
        with open(filename, "r") as f:
            data_read = f.readlines()
            f.close() 
        with open(filename, "w") as file:
            file.writelines(data_read)
            for l in name:
                file.write(l)
                if i!= count-1:
                    file.write(',')
                i+=1
            #file.write("\n")
        #file.write("\n") #After insertion of an entry, insert a newline character.

    

#The regular expression to be used to check if X-Y-Z position exists in every feature list in compiled feature file.
pattern = re.compile(r"X=\d[0,1],?[\s][\s][\s]Y=?\d[0,1,2],?[\s][\s][\s]Z=?\d[0,1]")


# Name is a list which holds all the values of a particular image's features.
# In our case every feature value for image-1 is stored in name[], next for image-2 all values would be stored 
# and so on till we reach end of the file.
name = []

# Open the file in read-mode and parse for regular expression.
with open(file,'rt') as myfile:
    for linenum, line in enumerate(myfile):
        count = 0
        # If there was no X-Y-Z position in our feature file, notify the user about this.
        if pattern.search(line) == None:
            print("There was no X-Y-Z position! Please look at the error in line #",linenum)
        
        #Else find all the occurances with X-Y-Z pattern    
        else:
            # Strip the unwanted characters to find the appropriate result.
            #print(line)
            name = []
            cur = line.replace("[","")
            cur = cur.replace("]","")
            cur = cur.replace(" ","")

            #print(cur)
            vals = cur.split(",")

            #vals=vals.replace(" ","")
                
            # Append name with value for every feature.
            for str1 in vals:
                print("str1=",str1)
                name.append(str1.split("=")[0])
                 
            # Make a note of count of entries in name.
            print("name=",name)
            count = len(name)
            print("count=",count)
            # Call the function where contents need to be written to.
            # The feature values would be written to a specific position based on the cube position.
        write_to_file("CSV_features_names",name,count)
        break
            #print(name)

#print("Writing to file is complete")
################################################################### END OF THE PROGRAM ####################################################################################
