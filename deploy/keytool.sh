#!/bin/bash

# Set the variables for the keytool command
export ALIAS=server
export KEYALG=RSA
export KEYSIZE=4096
export STORETYPE=JKS
export KEYSTORE=springboot.jks
export VALIDITY=365
export S3_BUCKET=nextwork-staging-keystore-jks


# Get the keystore password from the GitLab secret variable
export STOREPASS=$KEYSTORE_PASSWORD

# Generate a new key pair and store it directly in the keystore
keytool -genkey -alias $ALIAS -keyalg $KEYALG -keysize $KEYSIZE -keystore $KEYSTORE -storepass $STOREPASS -validity $VALIDITY -dname "CN=[Advanta], OU=[siemens-advanta], O=[siemens], L=[Bengaluru], ST=[Karnataka], C=[India]"

