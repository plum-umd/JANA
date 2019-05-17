### Automated Setup

Setup is currently done by a script in a Vagrant environment. The setup script manages all dependencies and setup.

## Dependencies

All that's needed on the host machine is Vagrant. All other dependencies are pulled in and installed on the guest box.

## Steps

1. From the root of the cloned repository:

```
cd auto_setup
vagrant up
vagrant ssh
/* now in the virtual machine */
cp /vagrant/setup.sh .
chmod +x setup.sh
./setup.sh
```

The script will require your github credentials when it starts running, in order to clone the repository from online. The script will then setup all dependencies (including sbt, Apron, and ELINA), and then build the Ainterp.jar tool (note: this script takes a few minutes to run). It will test this tool on the Ainterp.jar found in JANA/scala-tools, and you can find the results of the test in JANA/scala-tools/files/xalan

Note that this *will not* build JANA on the host machine. If you wish to do so, you may attempt to follow the same steps as in the setup.sh script, but may have to change some paths (e.g., java, sbt) to reflect different versions. You may also find the Instructions_From_Scratch.txt document helpful (it is in the resources.tar.gz tarball under auto_setup).

