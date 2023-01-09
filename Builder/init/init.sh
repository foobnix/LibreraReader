#!/usr/bin/env bash
sudo apt-get install \
git gnome-tweaks mc vlc transmission dconf-editor cmake \
mesa-common-dev libxcursor-dev libxrandr-dev libxinerama-dev \
libglu1-mesa-dev libxi-dev pkg-config libgl-dev \
ruby-full build-essential zlib1g-dev \
imagemagick jekyll \
python3-gpg \
gimp libcanberra-gtk-module libcanberra-gtk3-module \

sudo apt autoremove

#stop add printers
sudo systemctl stop cups-browsed
sudo systemctl disable cups-browsed

sudo cp -rfv 51-android.rules /etc/udev/rules.d/
sudo chmod a+r /etc/udev/rules.d/51-android.rules
sudo service udev restart

cp -rfv profile /home/dev/.profile
cp -rfv unzip_epub /home/dev/.local/share/nautilus/scripts

mkdir -p /home/dev/Dropbox/FREE_PDF_APK/testing/

cp -rfv org.gnome.AndroidStudio.desktop /home/dev/.local/share/applications/
cp -rfv org.gnome.Terminal.desktop /home/dev/.local/share/applications/
