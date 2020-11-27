#!/bin/bash
# Simple bash script which allows to generate all the java keystores required by the system.
# It generates keystores and truststores for each type of terminal (urn, post, station)

choose_psw() {
	while true;
	do
		echo "Choose a password for $1"
		read -s psw1
		echo "Confirm password"
		read -s psw2

		if [ "$psw1" == "$psw2" ]; 
		then
			if [ ${#psw1} -ge 6 ];
			then
				break
			else
				echo "Error - Passwords must be 6 characters long minimum"
			fi
		else
			echo "Error - Passwords don't match - Try Again"
		fi
	done

	password=$psw1
}

genkeystores() {
	echo "--- $1: Generating KeyStore ---"
	choose_psw "$1/KeyStore"
	echo "ks:$password" >> "Evoting/$1/src/main/resources/cfg/psws.cfg"
	keytool -genkey -alias "$2-cert" -keyalg RSA -keysize 2048 -validity 365 -keystore "Evoting/$1/ssl/keystore.jks" -storepass $password

	echo "$1: Exporting CA"
	keytool -export -alias "$2-cert" -file "/tmp/certs/$2.cer" -keystore "Evoting/$1/ssl/keystore.jks" -storepass $password
}

dbgenkeystores() {
	echo "--- $1: Generating KeyStore ---"
	choose_psw "$1/KeyStore"
	echo "ks:$password" >> "Evoting/$1/src/main/resources/cfg/psws.cfg"
	keytool -importkeystore -srckeystore "$db_path/client-keystore.p12" -destkeystore "Evoting/$1/ssl/keystore.jks" -deststorepass $password -srcstorepass tmppass
	keytool -changealias -alias db-cert -destalias "$2-cert" -keystore "Evoting/$1/ssl/keystore.jks" -storepass $password
	
	if [ $2 == "urn" ];
	then
		echo "$1: Exporting CA"
		keytool -export -alias "$2-cert" -file "/tmp/certs/$2.cer" -keystore "Evoting/$1/ssl/keystore.jks" -storepass $password
	fi
}

gentruststores() {
	echo "--- $1: Generating TrustStore ---"
	choose_psw "$1/TrustStore"
	echo "ts:$password" >> "Evoting/$1/src/main/resources/cfg/psws.cfg"

	certs="post
	stat
	aux-stat
	urn"

	for c in $certs;
	do
		if [ $2 != $c ] && [ $2 != "poll" ] && [ $2 != "proc-mgr" ] && [ $2 != "test" ];
		then
			echo "$1 TrustStore - Importing certificate for: $c"
			keytool -import -trustcacerts -alias "$c-cert" -file "/tmp/certs/$c.cer" -keystore "Evoting/$1/ssl/truststore.jks" -storepass $password			
		fi
	done

	if [ $2 == "urn" ] || [ $2 == "poll" ] || [ $2 == "proc-mgr" ] || [ $2 == "test" ];
	then
		echo "$1 TrustStore - Importing certificate for: db"
		keytool -import -trustcacerts -alias db -file "$db_path/ca.pem" -keystore "Evoting/$1/ssl/truststore.jks" -storepass $password
		
		echo "$1: This terminal interacts with the database. Do you want to setup db credentials now? (y=yes)"
		read answer
		user=""
		pass=""
		if [ $answer == "y" ];
		then
			echo "$1 - Inserting DB Credentials"
			echo "Username:"
			read user
			echo "Password:"
			read -s pass
		fi
		echo "dbu:$user" >> "Evoting/$1/src/main/resources/cfg/psws.cfg"
		echo "dbp:$pass" >> "Evoting/$1/src/main/resources/cfg/psws.cfg"
		if [ $answer == "y" ];
		then
			echo "Thanks, your data has been stored"
		fi
	fi

	chmod 400 Evoting/$1/ssl/*.jks
	chmod 400 Evoting/$1/src/main/resources/cfg/*.cfg
}

genipscfg() {
	echo "urn:$urn_ip" >> "Evoting/$1/src/main/resources/cfg/ips.cfg"
	chmod 400 "Evoting/$1/src/main/resources/cfg/ips.cfg"
}

if [ $# -ne 2 ] || ! [ -d "$1" ] || ! [ -d "$2" ];
then
	echo "Usage: $0 git_folder_path db_pem_folder"
	exit 1
fi

git_path=$1
db_path=$2
cd $git_path

echo "The following files will be permanently deleted:"
find . -name "*.cfg" -type f
find . -name "*.jks" -type f

echo "Are you sure that you want to proceed? (y=yes)"
read answ
if [ $answ != "y" ];
then
	exit 1
fi

find . -name "*.cfg" -type f -delete
find . -name "*.jks" -type f -delete

mkdir -p "/tmp/certs"

mkdir -p "Evoting/Poll/ssl"
mkdir -p "Evoting/Postazione/ssl"
mkdir -p "Evoting/ProcedureManager/ssl"
mkdir -p "Evoting/Seggio/ssl"
mkdir -p "Evoting/SeggioAusiliario/ssl"
mkdir -p "Evoting/Urna/ssl"
mkdir -p "Evoting/Test/ssl"

mkdir -p "Evoting/Poll/src/main/resources/cfg/"
mkdir -p "Evoting/Postazione/src/main/resources/cfg/"
mkdir -p "Evoting/ProcedureManager/src/main/resources/cfg/"
mkdir -p "Evoting/Seggio/src/main/resources/cfg/"
mkdir -p "Evoting/SeggioAusiliario/src/main/resources/cfg/"
mkdir -p "Evoting/Urna/src/main/resources/cfg/"
mkdir -p "Evoting/Test/src/main/resources/cfg/"


openssl pkcs12 -export -in "$db_path/client-cert.pem" -inkey "$db_path/client-key.pem" -name db-cert -out "$db_path/client-keystore.p12" -passout pass:tmppass

genkeystores "Postazione" "post"
genkeystores "Seggio" "stat"
genkeystores "SeggioAusiliario" "aux-stat"
dbgenkeystores "Urna" "urn"
dbgenkeystores "ProcedureManager" "proc-mgr"
dbgenkeystores "Poll" "poll"
dbgenkeystores "Test" "test"


echo " "
echo "--- Keystores done. Generating TrustStores now ---"
echo " "

gentruststores "Postazione" "post"
gentruststores "Seggio" "stat"
gentruststores "SeggioAusiliario" "aux-stat"
gentruststores "Urna" "urn"
gentruststores "ProcedureManager" "proc-mgr"
gentruststores "Poll" "poll"
gentruststores "Test" "test"

rm -rf "/tmp/certs"

echo " "
echo "--- TrustStores done. Finalizing config files now ---"
echo " "

echo "Please, provide the IP of the urn"
read urn_ip

genipscfg "Postazione"
genipscfg "Seggio"
genipscfg "SeggioAusiliario"

echo " "
echo "Done."