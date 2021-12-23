#https://askubuntu.com/questions/465454/problem-with-the-installation-of-virtualbox
#sudo apt install --reinstall virtualbox-dkms
sudo apt install -f
sudo dpkg-reconfigure virtualbox-dkms
sudo dpkg-reconfigure virtualbox
sudo modprobe vboxdrv

