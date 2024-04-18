#!/bin/sh
set -x
set -e
#
# Docker build calls this script to harden the image during build.
#

# groupadd --gid 5000 user && \
# useradd --home-dir /home/ --create-home --uid 5000 \
# --gid 5000 --shell /bin/sh --skel /dev/null user

#By default nginx user is created in base image https://github.com/nginxinc/docker-nginx/blob/f958fbacada447737319e979db45a1da49123142/mainline/alpine/Dockerfile
#APP_USER=$APP_USER 
#APP_DIR=$APP_DIR
#adduser -D -s /bin/sh -u 1000 -h $APP_DIR $APP_USER
#sed -i -r 's/^'"$APP_USER"':!:/'"$APP_USER"':x:/' /etc/shadow   
   
## restricting new permissions
sed -i 's/umask.*022/umask 007/g' /etc/profile

# Add permission to /home/ for backward compatibility
chmod 777 /home/

# Be informative after successful login.
echo -e "\n\nHardened App container image built on $(date)." > /etc/motd

# Remove existing crontabs, if any.
rm -fr /var/spool/cron
rm -fr /etc/crontabs
rm -fr /etc/periodic

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
find / -xdev -type d -perm +0002 -exec chmod o-w {} + \
	&& find / -xdev -type f -perm +0002 -exec chmod o-w {} + \
	&& chmod 777 /tmp/ \
	&& chown $APP_USER:root /tmp/
 
# Remove unnecessary user accounts, excluding current app user, sshd and root
sed -i -r '/^('"$APP_USER"'|root|sshd)/!d' /etc/group
sed -i -r '/^('"$APP_USER"'|root|sshd)/!d' /etc/passwd

# Remove interactive login shell for everybody but user.
sed -i -r '/^'"$APP_USER"':/! s#^(.*):[^:]*$#\1:/sbin/nologin#' /etc/passwd

sysdirs="
  /bin
  /etc
  /lib
  /sbin
  /usr
"
# Remove apk configs.
# find $sysdirs -xdev -regex '.*apk.*' -exec rm -fr {} +

# Remove crufty...
#   /etc/shadow-
#   /etc/passwd-
#   /etc/group-
find $sysdirs -xdev -type f -regex '.*-$' -exec rm -f {} +

# Ensure system dirs are owned by root and not writable by anybody else.
find $sysdirs -xdev -type d \
  -exec chown root:root {} \; \
  -exec chmod 0755 {} \;

# Remove all suid files.
find $sysdirs -xdev -type f -a -perm +4000 -delete

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
# rm -f /etc/fstab

# Remove broken symlinks (because we removed the targets above).
find $sysdirs -xdev -type l -exec test ! -e {} \; -delete
