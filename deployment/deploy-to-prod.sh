#!/bin/bash

echo "The script shall be executed from the project root."
echo "Before starting the deployment:"
echo "- Make sure the private key is stored under ~/.ssh/blog-key-pair.pem on local. It should have only read permissions. If not then run 'chmod 400 blog-key-pair.pem'."
echo "If this is the first time connecting to the EC2 instance you should see a message saying that the authenticity of the host cannot be established and that the key is not known. It will ask to continue the process giving specific instructions."
printf "\n"
echo "Enter the remote host: "
read -r REMOTE_HOST

echo "Configuration ready. Press any key to start the deployment process."
while [ true ] ; do
  read -t 3 -n 1
  if [ $? = 0 ] ; then
    break ;
  fi
done

./mvnw clean package

REMOTE_USER=ubuntu
SSH_REMOTE="$REMOTE_USER@$REMOTE_HOST"
# path to local SSH private key
SSH_CERT_PATH=~/.ssh/blog-key-pair.pem

# copy files to remote
echo "Copy files to remote"
scp -i $SSH_CERT_PATH "target/blog-0.0.1-SNAPSHOT.jar" "$SSH_REMOTE:~/blog.jar"

echo "Executing scripts on remote"
ssh -i $SSH_CERT_PATH ubuntu@ec2-35-156-18-247.eu-central-1.compute.amazonaws.com "bash -s" < deployment/remote-script.sh