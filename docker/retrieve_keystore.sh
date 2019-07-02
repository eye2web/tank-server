#!/bin/sh
#
# Purpose: Retrieve keystore file from S3 store
#
set -e

error=false
[ "$AWS_ACCESS_KEY_ID" ==  "" ]  && error=true && echo "Error: Access key not defined (AWS_ACCESS_KEY_ID)"
[ "$AWS_SECRET_ACCESS_KEY" ==  "" ]  && error=true && echo "Error: Secret access key not defined (AWS_SECRET_ACCESS_KEY)"
[ "$BUCKET" ==  "" ]  && error=true && echo "Error: store name not defined (BUCKET)"
[ "$BUCKET_LOCATION" ==  "" ]  && error=true && echo "Error: store location not defined (BUCKET_LOCATION)"
[ "$BUCKET_KEYSTORE_FILE" ==  "" ]      && error=true && echo "Error: keystorefile is not defined (BUCKET_KEYSTORE_FILE)"
[ $error == true ] && exit 1;

# Export credentials
export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY

# set Bucket signature
aws configure set default.s3.signature_version s3v4

echo "Connecting to $BUCKET_LOCATION"

# For debug purposes to see if keystore is available in S3 store
aws --endpoint-url $BUCKET_LOCATION s3 ls s3://$BUCKET

aws --endpoint-url $BUCKET_LOCATION s3 cp s3://$BUCKET/$BUCKET_KEYSTORE_FILE /app/KeyStore.pfx

# For debug purposes to see if keystore has been copied to the docker container
ls -l | grep *.pfx