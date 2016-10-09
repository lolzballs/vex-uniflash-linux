# Vex Uniflash on Linux

Very crude bodge for uniflash **Vex Cortex direct tether** to work on Linux, only Linux, using usb4java

Probably violated some license by reverse engineering some stuff

Don't sue me

Also, no warranty on any of this. If it bricks your Vex cortex or your computer or your house, I'm not responsible.

## Modified files:

- `org.uniflash.SerialPortIO`

## Created files:

- `org.uniflash.LinuxDriver`

## Usage:

`sudo java -jar uniflash.jar vex [your bin file] -P[vid:pid]`
