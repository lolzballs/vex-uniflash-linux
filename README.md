Very crude bodge for uniflash to work on Linux, using usb4java

Probably violated some license by reverse engineering some stuff

Don't sue me

Also, no warranty on any of this. If it bricks your Vex cortex or your computer or your house, I'm not responsible.

Modified files:
SerialPortIO

Created files:
LinuxDriver

To use:
sudo [java command and class path stuff] org.uniflash.Main vex [your bin file] -P[vid:pid]