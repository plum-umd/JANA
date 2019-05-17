Environment spec from last darpa doc:

  - Intel Next Unit of Computing: NUC5i5RYH with SSD Samsung 850 EVO M.2
  Sata 6Gb/s (Model#mz-n5e250bw) and Memory Crucial 16GB Kit 2-8GB
  PC3-12800 DDR3 KIT (Model #ct2kit102464BF160B).

  - Centos 7 with java-1.7.0-openjdk-1.7.0.85-2.6.1.2.el7_1.x86_64.rpm
  Java run in interpreted-only mode (no just-in-time compilation)
  using -Xint .

The java runtime rt.jar can be found in the Centos package

  - java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.rpm

The listed package only includes additions beyond the headless package
for audio/video support. I include both the rpm's here. I got them
from:

  - http://rpmfind.net/linux/RPM/centos/updates/7.1.1503/x86_64/Packages/java-1.7.0-openjdk-headless-1.7.0.85-2.6.1.2.el7_1.x86_64.html
