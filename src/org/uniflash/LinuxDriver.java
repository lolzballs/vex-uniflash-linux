package org.uniflash;

import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class LinuxDriver {

	public static int iface = 0;
	public static long timeout = 1000;

	public static DeviceHandle grabDevice(int pid, int vid) {
		Context context = new Context();
		int result = LibUsb.init(context);
		if (result != LibUsb.SUCCESS) {
			throw new RuntimeException("libusb init failed");
		}
		DeviceList list = new DeviceList();
		result = LibUsb.getDeviceList(context, list);
		if (result < 0) {
			throw new RuntimeException("libusb device list failed");
		}
		Device dev = null;
		for (Device device : list) {
			DeviceDescriptor descriptor = new DeviceDescriptor();
			result = LibUsb.getDeviceDescriptor(device, descriptor);
			if (result != LibUsb.SUCCESS) {
				System.out.println("libusb failed to read descriptor");
				continue;
			}
			if (descriptor.idVendor() == pid && descriptor.idProduct() == vid) {
				dev = device;
				break;
			}
		}
		LibUsb.freeDeviceList(list, true);
		if (dev == null) {
			throw new RuntimeException("libusb device not found");
		}

		DeviceHandle handle = new DeviceHandle();
		result = LibUsb.open(dev, handle);
		if (result != LibUsb.SUCCESS) {
			throw new RuntimeException("libusb cannot open device");
		}

		result = LibUsb.claimInterface(handle, iface);
		if (result != LibUsb.SUCCESS) {
			throw new RuntimeException("libusb cannot claim interface");
		}
		boolean detach = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER) && LibUsb.kernelDriverActive(handle, 0) != 0;
		if(detach) {
			result = LibUsb.detachKernelDriver(handle, iface);
			if(result != LibUsb.SUCCESS) {
				throw new RuntimeException("libusb failed to detach kernel driver");
			}
		}

		return handle;
	}

	public static void init(DeviceHandle handle) {
		controlTransfer(handle, 0xa1, 0x21, 0, 0, new byte[7]);
		controlTransfer(handle, 0x21, 0x22, 0, 0, new byte[0]);
		controlTransfer(handle, 0x21, 0x20, 0, 0, new byte[]{0x00, (byte)0xc2, 0x01, 0x00, 0x00, 0x00, 0x08});
		controlTransfer(handle, 0xa1, 0x21, 0, 0, new byte[7]);
	}

	public static void open(DeviceHandle handle) {
		interruptTransfer(handle, 0x82, new byte[0]);
		bulkTransfer(handle, 0x85, new byte[0]);
		controlTransfer(handle, 0xa1, 0x21, 0, 0, new byte[7]);
		controlTransfer(handle, 0xa1, 0x21, 0, 0, new byte[7]);
	}

	public static void close(DeviceHandle handle) {
		bulkTransfer(handle, 0x85, new byte[0]);
		interruptTransfer(handle, 0x82, new byte[0]);
		controlTransfer(handle, 0x21, 34, 2, 0, new byte[0]);
	}

	public static void setDTR(DeviceHandle handle, boolean flag) {
		controlTransfer(handle, 0x21, 34, flag ? 1 : 0, 0, new byte[0]);
	}

	public static void updateOptions(DeviceHandle handle, int a, int parity) {
		controlTransfer(handle, 0xa1, 33, 0, 0, new byte[7]);
		controlTransfer(handle, 0xa1, 33, 0, 0, new byte[7]);
		controlTransfer(handle, 0xa1, 33, 0, 0, new byte[7]);
		controlTransfer(handle, 0xa1, 33, 0, 0, new byte[7]);
		controlTransfer(handle, 0x21, 32, 0, 0, new byte[]{0x00, (byte)0xc2, 0x01, 0x00, 0x00, 0x00, 0x08});
		controlTransfer(handle, 0xa1, 33, 0, 0, new byte[7]);

		controlTransfer(handle, 0x21, 34, a, 0, new byte[0]);
		controlTransfer(handle, 0x21, 32, 0, 0, new byte[]{0x00, (byte)0xc2, 0x01, 0x00, 0x00, (byte)parity, 0x08});
		controlTransfer(handle, 0xa1, 33, 0, 0, new byte[7]);
	}

	public static void setParity(DeviceHandle handle, int parity) {
		updateOptions(handle, 0, parity);
	}

	public static void setFlowControl(DeviceHandle handle, int parity) {
		updateOptions(handle, 2, 0);
	}

	public static int write(DeviceHandle handle, ByteBuffer buffer) {
		IntBuffer transferred = IntBuffer.allocate(1);
		int ret = LibUsb.bulkTransfer(handle, (byte)0x05, buffer, transferred, timeout);
		if(ret < 0) {
			System.err.println(ret);
			Thread.dumpStack();
			throw new RuntimeException("libusb write failed: " + ret);
		}
		return transferred.get();
	}

	public static int read(DeviceHandle handle, ByteBuffer buffer) {
		IntBuffer transferred = IntBuffer.allocate(1);
		int ret = LibUsb.bulkTransfer(handle, (byte) 0x85, buffer, transferred, timeout);
		if (ret < 0) {
			System.err.println(ret);
			Thread.dumpStack();
			throw new RuntimeException("libusb read failed: " + ret);
		}
		return transferred.get();
	}

	public static void controlTransfer(DeviceHandle handle, int bmRequestType, int bRequest, int wValue, int wIndex, byte[] buffer) {
		ByteBuffer buf = ByteBuffer.allocateDirect(buffer.length);
		if((bmRequestType & 0x80) == 0) {
			buf.put(buffer);
		}
		int ret = LibUsb.controlTransfer(handle, (byte)bmRequestType, (byte)bRequest, (short)wValue, (short)wIndex, buf, timeout);
		if(ret < 0 && ret != -7) {
			Thread.dumpStack();
			throw new RuntimeException("libusb controlTransfer failed: " + ret);
		}
		if((bmRequestType & 0x80) != 0) {
			buf.get(buffer);
		}
	}

	public static int interruptTransfer(DeviceHandle handle, int endpoint, byte[] buffer) {
		IntBuffer transferred = IntBuffer.allocate(1);
		ByteBuffer buf = ByteBuffer.allocateDirect(buffer.length);
		if((endpoint & 0x80) == 0) {
			buf.put(buffer);
		}
		int ret = LibUsb.interruptTransfer(handle, (byte)endpoint, buf, transferred, timeout);
		if(ret < 0 && ret != -7) {
			Thread.dumpStack();
			throw new RuntimeException("libusb interruptTransfer failed: " + ret);
		}
		if((endpoint & 0x80) != 0) {
			buf.get(buffer);
		}
		return transferred.get();
	}

	public static int bulkTransfer(DeviceHandle handle, int endpoint, byte[] buffer) {
		IntBuffer transferred = IntBuffer.allocate(1);
		ByteBuffer buf = ByteBuffer.allocateDirect(buffer.length);
		if((endpoint & 0x80) == 0) {
			buf.put(buffer);
		}
		int ret = LibUsb.bulkTransfer(handle, (byte)endpoint, buf, transferred, timeout);
		if(ret < 0 && ret != -7) {
			Thread.dumpStack();
			System.out.println(ret);
			throw new RuntimeException("libusb bulkTransfer failed: " + ret);
		}
		if((endpoint & 0x80) != 0) {
			buf.get(buffer);
		}
		return transferred.get();
	}
}
