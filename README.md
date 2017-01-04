# jdpkg-deb
Basic Java Implementation of DPKG-DEB


## NAME
jdpkg-deb - Debian package archive (.deb) manipulation tool

## SYNOPSIS
jdpkg-deb [option...] command


## DESCRIPTION 
jdpkg-deb packs, ~~unpacks and provides information about~~ Debian archives.

~~Use dpkg to install and remove packages from your system.~~

~~You can also invoke dpkg-deb by calling dpkg with whatever options<br>
you want to pass to dpkg-deb. dpkg will spot that you wanted dpkg-deb<br>
and run it for you.~~ 

For most commands taking an input archive argument, the archive can<br>
be read from standard input if the archive name is given as a single<br>
minus character («-»); otherwise lack of support will be documented<br>
in their respective command description.<br>

## COMMANDS 

-b, --build binary-directory [archive|directory]


Creates a debian archive from the filesystem tree stored in<br>
binary-directory. binary-directory must have a DEBIAN<br>
subdirectory, which contains the control information files<br>
such as the control file itself. This directory will not<br>
appear in the binary package's filesystem archive, but instead<br>
the files in it will be put in the binary package's control<br>
information area.

## OPTIONS
 
-Zcompress-type
 
Specify which compression type to use when building a package.<br>
Allowed values are gzip, xz (since dpkg 1.15.6), and none<br>
(default is xz).
