printf "" >  $AUAVHOME/AUAVAndroid/app/src/main/assets/jarList
cd $AUAVHOME/libs/
for i in `ls *jar`; do printf "$i:" >>  $AUAVHOME/AUAVAndroid/app/src/main/assets/jarList; done
cd $AUAVHOME/apps
for i in `ls *jar`; do printf "$i:">>  $AUAVHOME/AUAVAndroid/app/src/main/assets/jarList; done


