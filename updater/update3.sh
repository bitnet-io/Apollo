#!/bin/bash
#Database downloader script

echo Downloading shards...

DOWNLOAD_PATH=https://apl-db-upd.s3.fr-par.scw.cloud/

DB_POSTFIX=-2021-q4
if [ $4 == 'false' ]
then
    DB_POSTFIX=${DB_POSTFIX}-noshards
fi

cd $1
rm -rfv $1/tmpdir
mkdir tmpdir
cd tmpdir
rm -rfv "$5".tar.gz

wget -v ${DOWNLOAD_PATH}$5${DB_POSTFIX}.tar.gz -O $5.tar.gz || curl --retry 100 ${DOWNLOAD_PATH}$5${DB_POSTFIX}.tar.gz -o $5.tar.gz
echo Unpacking...
tar -zxvf $5.tar.gz


CONFIGDIR=conf

if [ $5 == 'a2e9b9' ]
then
    CONFIGDIR=conf-tn1
fi

if [ $5 == '2f2b61' ]
then
    CONFIGDIR=conf-tn2
fi

echo Config dir = ${CONFIGDIR}

if [ $3 == 'true' ]
then
    rm -rfv ~/.apl-blockchain/apl-blockchain-db/$5
    cp -rfv $1/tmpdir/* ~/.apl-blockchain/apl-blockchain-db/

else
    if [ -f $1/$CONFIGDIR/apl-blockchain.properties ]
    then
	if [ 1 == $(cat $1/$CONFIGDIR/apl-blockchain.properties | grep customDbDir | grep -v "#" | wc -l) ]
	then 
    	    cd $1
    	    cd $(cat $1/$CONFIGDIR/apl-blockchain.properties | grep customDbDir | cut -f2 -d'=')
    	    rm -rfv $5
    	    cp -rfv $1/tmpdir/* .
	else
	    cd $1/../apl-blockchain-db/
	    rm -rfv $5
    	    cp -rfv $1/tmpdir/* .
	fi
	
    else
	cd $1/../apl-blockchain-db/
	rm -rfv $5
    	cp -rfv $1/tmpdir/* .
    fi

    rm -rfv $1/tmpdir
fi

rm -rfv $1/tmpdir
