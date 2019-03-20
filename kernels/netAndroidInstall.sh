cp $AUAVHOME/AUAVAndroid/app/build/outputs/apk/app-debug.apk AUAVAndroid.apk
scp -i $AUAVHOME/META/dronephone_rsa -P 1234 AUAVAndroid.apk reroutlab@$1:.
