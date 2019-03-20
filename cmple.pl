#!/usr/bin/perl -w

# The Master build script compiles all of
# the AUVA projects from the command line
# and also produces JAVADOC
#
# It is a wrapper to gradle
#
# Parameters
# -drv  Compiles drivers
# -rtn  Compiles routines
# -lin  Compiles LinuxKernel
# -doc  Compiles javadocs
# -mdl  Compiles scripts in externalModels
#
#
if (!(defined $ENV{"AUAVHOME"})) {
		print "Must define System variable: AUAVHOME\n";
		exit;
}
$AUAV_HME = $ENV{"AUAVHOME"};

$cmpl_all =1;
$cmpl_drv =0;
$cmpl_mdl =0;
$cmpl_rtn =0;
$cmpl_lin =0;
$cmpl_doc =0;
$cmpl_and =0;
$cmpl_cln =0;
$cmpl_ins =0;
$AIP = "";

if ($#ARGV >= 0 ) {
		$temp_itr = 0;
		while ($temp_itr <= $#ARGV) {
				if ($ARGV[$temp_itr] =~ /^-drv$/) {
						$cmpl_all =0;
						$cmpl_drv =1;
				}
                if($ARGV[$temp_itr] =~ /^-mdl$/) {
                        $cmpl_all =0;
                        $cmpl_mdl =1;
                }
				if ($ARGV[$temp_itr] =~ /^-rtn$/) {
						$cmpl_all =0;
						$cmpl_rtn =1;
				}
				if ($ARGV[$temp_itr] =~ /^-lin$/) {
						$cmpl_all =0;
						$cmpl_lin =1;
				}
				if ($ARGV[$temp_itr] =~ /^-and$/) {
						$cmpl_all =0;
						$cmpl_and =1;
				}
				if ($ARGV[$temp_itr] =~ /^-cln$/) {
						$cmpl_all =0;
						$cmpl_cln =1;
				}
				if ($ARGV[$temp_itr] =~ /^-ins$/) {
						$cmpl_all =0;
						$cmpl_ins =1;
						$temp_itr++;
						if ($temp_itr <= $#ARGV) {
								$AIP = $ARGV[$temp_itr];
						}
						if ($AIP =~ /\d+\.\d+\.\d+\.\d+/) {
						}
						else {
								print "ins requires IPV4 address\n";
								exit;
						}
				}
				if ($ARGV[$temp_itr] =~ /^-doc$/) {
						$cmpl_all =0;
						$cmpl_doc =1;
				}
				if ($ARGV[$temp_itr] =~/^-h$/) {
						my $message = <<'END_MESSAGE';
Parameters
 -drv  Compiles drivers
 -rtn  Compiles routines
 -lin  Compiles Linux Kernel
 -and  Compiles Android Kernel
 -ins  Installs and starts APK on remote device
 -cln  Cleans all packages for recompile
 -doc  Compiles javadocs
 -mdl  Compiles models in externalModels directory
 -h    This help screen
END_MESSAGE
						print $message;
						exit;
				}
				$temp_itr++;
		}
}

chdir("$AUAV_HME/interfaces");
system("gradle installJars");

if (($cmpl_drv + $cmpl_all) >= 1) {
		chdir("$AUAV_HME/drivers.src");
		system("gradle installJars");
}

if (($cmpl_rtn + $cmpl_all) >= 1) {
		chdir("$AUAV_HME/routines.src");
		system("gradle installJars");
}

if (($cmpl_mdl + $cmpl_all) >= 1) {
        chdir("$AUAV_HME/externalModels/A_Star_code/driver_code/");
        system("bash cmple.sh");
        chdir("$AUAV_HME/externalModels/KNN_code/driver_code/");
        system("bash cmple.sh");
        chdir("$AUAV_HME/externalModels/python/Parser/");
        system("javac Parser.java");
}

if (($cmpl_lin + $cmpl_all) >= 1) {
		chdir("$AUAV_HME/AUAVLinux");
		system("gradle installJars");
}

if (($cmpl_and ) >= 1) {
		`date > $AUAV_HME/AUAVAndroid/app/src/main/assets/AUAVassets/CompileDate`;
		chdir("$AUAV_HME/AUAVAndroid");
		system("./gradlew assembleDebug");
		system("cp $AUAV_HME/AUAVAndroid/app/build/outputs/apk/app-debug.apk $AUAV_HME/kernels/AUAVAndroid.apk");
		system("cp $AUAV_HME/AUAVAndroid/app/build/outputs/apk/debug/app-debug.apk $AUAV_HME/kernels/AUAVAndroid.apk");
}

if (($cmpl_ins ) >= 1) {

		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"rm -f /sdcard/AUAVAndroid.apk\"");
		system("scp -P 22223 -i $AUAV_HME/tools/AUAV $AUAV_HME/kernels/AUAVAndroid.apk root\@$AIP:/sdcard/AUAVAndroid.apk");
		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"su -c \\\"pm install -r /sdcard/AUAVAndroid.apk\\\"\"");
#  Commented code below are from failed attempts to override DJI registration
#		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"su -c \\\"rm -r /data/user/0/org.reroutlab.code.auav.kernels.auavandroid\\\"\"");
#		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"su -c \\\"rm -r /data/user/0/com.android.vending\\\"\"");
#		system("scp -P 22223 -i $AUAV_HME/tools/AUAV -r $AUAV_HME/tools/AUAVAndroid.Directory.Snapshot root\@$AIP:/sdcard/ADS");
#		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"su -c \\\"mv /sdcard/ADS /data/user/0/org.reroutlab.code.auav.kernels.auavandroid\\\"\"");
#		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"su -c \\\"mv /sdcard/CAV.AUAVReg /data/user/0/com.android.vending\\\"\"");
#  End of failed override attempts --- Next line starts the app
		system("ssh -p 22223 -i $AUAV_HME/tools/AUAV root\@$AIP \"su -c \\\"am start -n org.reroutlab.code.auav.kernels.auavandroid/.MainActivity\\\"\" ");
}

if (($cmpl_cln ) >= 1) {
		chdir("$AUAV_HME/interfaces");
		system("gradle clean");
		chdir("$AUAV_HME/drivers.src");
		system("gradle clean");
		chdir("$AUAV_HME/routines.src");
		system("gradle clean");
		chdir("$AUAV_HME/AUAVLinux");
		system("gradle clean");
		chdir("$AUAV_HME/AUAVAndroid");
		system("./gradlew clean");

}

if (($cmpl_doc + $cmpl_all) >= 1) {
		chdir("$AUAV_HME/");
		mkdir("$AUAV_HME/docs");
        system('javadoc -d docs -classpath external/californium-core-1.1.0-SNAPSHOT.jar:external/android-3.1.jar:external/* -subpackages org.rerout `find drivers.src/ routines.src/ interfaces/  -type f -name "*.java"`' );
     }
