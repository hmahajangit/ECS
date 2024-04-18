#!/bin/sh
set -x
set -e
#
# Docker build calls this script to harden the image during build.
#
APP_USER=$APP_USER 
APP_DIR=$APP_DIR

#info "Applying updates... and Install required system packages..."
#apt-get update && apt-get -y install --no-install-recommends imagemagick software-properties-common gnupg

useradd -m -d $APP_DIR -r -u 1001 -g root --shell /bin/sh $APP_USER
sed -i -r 's/^'"$APP_USER"':!:/'"$APP_USER"':x:/' /etc/shadow   
   
# Add permission to /home/ for backward compatibility
chmod 777 $APP_DIR

# Be informative after successful login.
echo -e "\n\nHardened App container image built on $(date)." > /etc/motd

#Remove unnecessary softwares to Minimize Vulnerability
#apt-get --purge remove -y perl bzip2 curl git git-man mercurial mercurial-common subversion xz-utils wget unzip
apt-get --purge remove -y perl bzip2 xz-utils
apt autoremove -y


# Remove all but a handful of admin commands.
find /sbin /usr/sbin ! -type d \
  -a ! -name login_duo \
  -a ! -name nologin \
  -a ! -name setup-proxy \
  -a ! -name sshd \
  -a ! -name start.sh \
  -a ! -name nginx \
  -delete

# Remove world-writeable permissions except for /tmp/
find / -xdev -type d \( -perm -0002 -a ! -perm -1000 \) -exec chmod o-w {} + \
	&& find / -xdev -type f \( -perm -0002 -a ! -perm -1000 \) -exec chmod o-w {} + \
	&& chmod 777 /tmp/ \
	&& chown $APP_USER:root /tmp/

# Remove unnecessary user accounts, excluding current app user, nobody and root
sed -i -r '/^('"$APP_USER"'|root|nobody)/!d' /etc/group
sed -i -r '/^('"$APP_USER"'|root|nobody)/!d' /etc/passwd

# Remove interactive login shell for everybody but APP_USER.
sed -i -r '/^'"$APP_USER"':/! s#^(.*):[^:]*$#\1:/sbin/nologin#' /etc/passwd

sysdirs="
  /bin
  /etc
  /lib
  /usr
"

#Disable Unwanted SUID and SGID Binaries
find $sysdirs -xdev -type f \( -perm -4000 -o -perm -2000 \) -delete

# Remove crufty...
#   /etc/shadow-
#   /etc/passwd-
#   /etc/group-
find $sysdirs -xdev -type f -regex '.*-$' -exec rm -f {} +

# Ensure system dirs are owned by root and not writable by anybody else.
find $sysdirs -xdev -type d \
  -exec chown root:root {} \; \
  -exec chmod 0755 {} \;

# Remove other programs that could be dangerous.
find $sysdirs -xdev \( \
  -name hexdump -o \
  -name chgrp -o \
  -name chmod -o \
  -name chown -o \
  -name od -o \
  -name strings -o \
  -name su \
  \) -delete

# Remove init scripts since we do not use them.
rm -fr /etc/init.d
rm -fr /lib/rcrm -fr /root
rm -fr /etc/conf.d
rm -fr /etc/inittab
rm -fr /etc/runlevels
rm -fr /etc/rc.conf

# Remove kernel tunables since we do not need them.
rm -fr /etc/sysctl*
rm -fr /etc/modprobe.d
rm -fr /etc/modules
rm -fr /etc/mdev.conf
rm -fr /etc/acpi

# Remove root homedir since we do not need it.
rm -fr /root

# Remove fstab since we do not need it.
rm -f /etc/fstab

# Remove broken symlinks (because we removed the targets above).
find $sysdirs -xdev -type l -exec test ! -e {} \; -delete

#Removing Noowner Files
find $sysdirs -xdev \( -nouser -o -nogroup \) -delete

#Ensure no shosts cfg on system
find / -name ".shosts" -exec rm {} \;
find / -name "shosts.equiv" -exec rm {} \;

