# Third-party notices

jBladeRF is licensed under the Apache License 2.0. The jar also contains native
libraries from the projects below, which keep their own licenses. Full license
texts are in `META-INF/licenses/` inside the jar.

The upstream revision of each jar is recorded in
`jbladerf/native/<platform>/build-info.properties`, and `native/versions.env` in
this repository records the release a jar was built from. `native/build-unix.sh`
and `native/build-windows.ps1` in the same revision of this repository build the
libraries from those sources.

## libbladeRF

Source: <https://github.com/Nuand/bladeRF>

Licensed under the GNU Lesser General Public License version 2.1. Some of its
source files have other licenses that are compatible with it. See
`META-INF/licenses/bladeRF/`, where `COPYING` explains which parts of the bladeRF
project each license applies to. jBladeRF is an independent project and is not
endorsed by Nuand or its contributors.

libbladeRF is shipped as a separate shared library so it can be replaced: point
the `jbladerf.library.path` system property at a directory containing your own
build. It uses the libusb that jlibusb supplies.

## AD9361 driver

Source: <https://github.com/analogdevicesinc/no-OS>

Copyright Analog Devices, Inc., with modifications by Nuand, and statically linked
into libbladeRF. See `META-INF/licenses/ad9361-LICENSE.txt`. Its license requires
use of the software to be on, or directly connected to, an Analog Devices
component, such as the AD9361 in a bladeRF 2.0, and it does not grant any patent
rights.

## libusb

jBladeRF does not contain libusb. It is supplied at runtime by
[jlibusb](https://github.com/DSheirer/jlibusb), which ships it under the GNU
Lesser General Public License version 2.1 or later and documents how to
replace it. jBladeRF is built against the libusb headers of the release that
`native/versions.env` records.
