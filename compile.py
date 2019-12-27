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
    os.chdir(auavhome+"/AUAVAndroid")
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
    os.system("date > "+auavhome+"/AUAVAndroid/app/src/main/assets/AUAVassets/CompileDate")
    os.chdir(auavhome+"/AUAVAndroid")
    subprocess.call(["./gradlew","assembleDebug"])
    subprocess.call(["cp",auavhome+"/AUAVAndroid/app/build/outputs/apk/debug/app-debug.apk",auavhome+"/kernels/AUAVAndroid.apk"]);

if(args.ins):
    print("\n===================Transfering APK===================")
    os.system("ssh -p 22223 -i "+auavhome+"/tools/AUAV root\@"+args.ins+" \"rm -f /sdcard/AUAVAndroid.apk\"")
    os.system("scp -P 22223 -i "+auavhome+"/tools/AUAV "+auavhome+"/kernels/AUAVAndroid.apk root\@"+args.ins+":/sdcard/AUAVAndroid.apk")
    os.system("ssh -p 22223 -i "+auavhome+"/tools/AUAV root\@"+args.ins+" \"su -c \\\"pm install -r /sdcard/AUAVAndroid.apk\\\"\"")
    os.system("ssh -p 22223 -i "+auavhome+"/tools/AUAV root\@"+args.ins+" \"su -c \\\"am start -n org.reroutlab.code.auav.kernels.auavandroid/.MainActivity\\\"\" ")


