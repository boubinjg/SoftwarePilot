#!/usr/bin/perl -w

if (!(defined $ENV{"AUAVHOME"})) {
		print "Must define System variable: AUAVHOME\n";
		exit;
}
$AUAV_HME = $ENV{"AUAVHOME"};


if (($#ARGV + 1) < 3) {
		print "Min 3 parameters: IPV4 dn=XXX-dc=YYY\n";
		die;
}

$cmd = sprintf("dn=$ARGV[1]-dc=$ARGV[2]");
$iter = 3;
while ($iter < ($#ARGV+1)) {
		$cmd = sprintf("%s-dp=$ARGV[$iter]",$cmd);
		$iter++;
}

system("$AUAV_HME/tools/coap-tool/coap-client coap://$ARGV[0]:5117/cr -m put -e $cmd");
