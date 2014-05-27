#!/bin/bash
#
# Just installs udev rules to give the Android devices permissions allowing anyone to debug.
# TODO: For security conscious, adjust the rules to use a different group...

sudo cp android.rules /etc/udev/rules.d/51-android.rules
sudo chmod 644   /etc/udev/rules.d/51-android.rules
sudo chown root. /etc/udev/rules.d/51-android.rules
sudo service udev restart
sudo killall adb
