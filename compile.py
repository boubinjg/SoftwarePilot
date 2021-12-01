import argparse
import os
import subprocess
import sys

parser = argparse.ArgumentParser()
parser.add_argument('-drv', action='store_true',help='Compile drivers')
parser.add_argument('-rtn', action='store_true',help='Compile routines')
parser.add_argument('-lin', action='store_true',help='Compile SoftwarePilot Linux kernel and interfaces')
parser.add_argument('-mdl', action="store_true",help='Compile certain external models')
parser.add_argument('-doc', action='store_true',help='Compile doc')
parser.add_argument('-cln', action='store_true',help='Clean Gradle')
parser.add_argument('-andr', action='store_true',help='Compile Android APK')
parser.add_argument('-code', action='store_true',help='Compile drivers, routines, kernel, and doc')
parser.add_argument('-all', action='store_true',help='Compile drivers, routines, kernel, doc, and APK')
parser.add_argument('-ins', type=str,help='Install APK on VM (Requires IP address of VM)')
args = parser.parse_args()

#cwd = os.getcwd()
#os.environ["AUAVHOME"] = cwd
auavhome = os.environ['AUAVHOME']

if(len(sys.argv) == 1):
    args.code = True

if(args.cln):
    print("\n===================Cleaning===================")
    os.chdir(auavhome+"/interfaces")
    subprocess.call(["gradle","clean"])
    os.chdir(auavhome+"/drivers.src")
    subprocess.call(["gradle","clean"])
    os.chdir(auavhome+"/routines.src")
    subprocess.call(["gradle","clean"])
    os.chdir(auavhome+"/AUAVLinux")
    subprocess.call(["gradle","clean"])
    os.chdir(auavhome+"/SPAndroid")
    subprocess.call(["./gradlew","clean"])

if(args.drv or args.all or args.code):
    print("\n===================Compiling Drivers===================")
    os.chdir(auavhome+"/drivers.src")
    subprocess.call(["gradle","installJars"])

if(args.rtn or args.all or args.code):
    print("\n===================Compiling Routines===================")
    os.chdir(auavhome+"/routines.src")
    subprocess.call(["gradle","installJars"])

if(args.mdl or args.all or args.code):
    print("\n===================Compiling External Models===================")
    os.chdir(auavhome+"/externalModels/A_Star_code/driver_code/")
    subprocess.call(["bash","cmple.sh"])
    os.chdir(auavhome+"/externalModels/KNN_code/driver_code/")
    subprocess.call(["bash","cmple.sh"])
    os.chdir(auavhome+"/externalModels/python/Parser")
    subprocess.call(["javac","Parser.java"])

if(args.lin or args.all or args.code):
    print("\n===================Compiling SoftwarePilot Linux Kernel===================")
    os.chdir(auavhome+"/AUAVLinux")
    subprocess.call(["gradle","installJars"])

if(args.doc or args.all or args.code):
    print("\n===================Compiling Documentation===================")
    os.chdir(auavhome+"/")
    try:
        os.mkdir("huh")
    except:
        pass

    os.system("javadoc -d docs -classpath external/californium-core-1.1.0-SNAPSHOT.jar:external/android-3.1.jar:external/* subpackages org.rerout `find drivers.src/ routines.src/ interfaces/ -type f -name *.java`")

if(args.andr or args.all):
    print("\n===================Compiling Android APK===================")
    os.system("date > "+auavhome+"/SPAndroid/app/src/main/assets/AUAVassets/CompileDate")
    os.chdir(auavhome+"/SPAndroid")
    subprocess.call(["./gradlew","assembleDebug"])
    subprocess.call(["cp",auavhome+"/SPAndroid/app/build/outputs/apk/debug/app-debug.apk",auavhome+"/kernels/AUAVAndroid.apk"]);

if(args.ins):
    print("\n===================Transfering APK===================")
    os.system("chmod 400 /home/SoftwarePilot/tools/AUAV")
    os.system("ssh -oHostKeyAlgorithms=+ssh-dss -p 22222 -i "+auavhome+"/tools/AUAV ssh\@"+args.ins+" \"rm -f /sdcard/AUAVAndroid.apk\"")
    os.system("scp -oHostKeyAlgorithms=+ssh-dss -P 22222 -i "+auavhome+"/tools/AUAV "+auavhome+"/kernels/AUAVAndroid.apk ssh\@"+args.ins+":/sdcard/AUAVAndroid.apk")
    os.system("ssh -oHostKeyAlgorithms=+ssh-dss -p 22222 -i "+auavhome+"/tools/AUAV ssh\@"+args.ins+" \"su -c \\\"pm install -r /sdcard/AUAVAndroid.apk\\\"\"")
    os.system("ssh -oHostKeyAlgorithms=+ssh-dss -p 22222 -i "+auavhome+"/tools/AUAV ssh\@"+args.ins+" \"su -c \\\"am start -n com.dji.sdk.sample.internal.controller/.MainActivity\\\"\" ")


