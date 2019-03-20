#Jayson Boubin
#6/4/18

Properties Format:
    Properties files list individual features for the praser to extract. Feature names must be accompanied by
    a script directory, and that scripts requisite parameters in the following format:
    feature=scriptDirectory,Param1,Param2

    Features:
        Features are individual outputs from a given script. For instance, the DLIBFaceDetection script
        outputs features like width, centerx, etc of its detected face box. To include any one of those features
        in your featureset, you must specify the specific feature's name.
    
    Script Directories:
        A script directory is just that, a directory where a feature extraction application exists.
        Feature extraction scripts can be written in any language, and only require two things:
        1) The script must be runnable from a run.sh bash script located at the top layer of the script directory
            (e.g, DLIBFaceDetection/run.sh is a script that runs the DLIB face detection in some language)
        2) The script must output it's features and nothing else to stdout. 

    Parameters:
        Parameters may be one of the following:
            Image: specifies that the image provided to the parser will be passed to the script
            JSON: Specifies that a JSON file provided to the parse will be passed to the script
            Face: Specifies that a Face found by a prior command will be passed to the script
    
    Example Properties File Line:
        width=DLIBFaceDetection,Image
        (extracts width from the DLIB Face detection script, passes that script an image)

Parser format:
    The parser is a java file that reads a properties file, executes its specified scripts, and returns the
    requested features. This application has a number of required and optional parameters.

    Parameters:
        Required:
        1) Properties File: Any file of the above format
        2) Output File: An extant file to which the parsed featureset will be appended.
        Optional: (can be provided in any order)
        1) JSON File
        2) Image file with any reasonable extension.
            These files parameters should be passed wit the JSON= or Image= prefix for the parser to identify them

    Example call to the parser:
        java Parser features.properties outputs.txt JSON=test.json Image=test.jpeg

***************This should be updated in the next few days to include directory feature extraction
