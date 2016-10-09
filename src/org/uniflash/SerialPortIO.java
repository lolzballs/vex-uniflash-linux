package org.uniflash;

import org.usb4java.DeviceHandle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

public class SerialPortIO {

	public DeviceHandle handle;

	private final ReentrantLock rxLock = new ReentrantLock(true);
	private final ReentrantLock txLock = new ReentrantLock(true);

	public static final int BUFFER_SIZE = 4096;

	private ByteBuffer rxBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

	private byte[] irxBuffer = new byte[BUFFER_SIZE];
	private int irxLength;

	public SerialPortIO(String port) throws IOException {
		String[] p = port.split(":");
		int vid = Integer.parseInt(p[0], 16);
		int pid = Integer.parseInt(p[1], 16);
		try {
			handle = LinuxDriver.grabDevice(vid, pid);
			LinuxDriver.init(handle);
			LinuxDriver.open(handle);
			LinuxDriver.setDTR(handle, false);
			LinuxDriver.setFlowControl(handle, 0);
		} catch(RuntimeException e) {
			throw new IOException(e);
		}
	}

	public synchronized void close() {
		try {
			LinuxDriver.close(handle);
		} catch(RuntimeException e) {
		}
	}

	public void flush() {
	}

	public String getName() {
		return "Linux libusb";
	}

	public long getTimeout() {
		return LinuxDriver.timeout;
	}

	public void purge() {
	}

	public byte read() throws IOException {
		return this.read(1)[0];
	}

	public synchronized void setDTR(boolean enabled) throws IOException {
		try {
			LinuxDriver.setDTR(handle, enabled);
		} catch (RuntimeException var3) {
			throw new IOException("Failed to set DTR = " + enabled);
		}
	}

	public void setRTS(boolean enabled) throws IOException {
	}

	public synchronized void setParams(int baud, int parity) throws IOException {
		try {
			LinuxDriver.setParity(handle, parity);
		} catch (RuntimeException var4) {
			throw new IOException("Failed to set port parameters");
		}
	}

	public void setTimeout(long timeout) {
		LinuxDriver.timeout = timeout;
	}

	public void write(int data) throws IOException {
		write(new byte[]{(byte)data});
	}

	public synchronized byte[] read(int length) throws IOException {
		rxLock.lock();
		byte[] read = new byte[length];
		int r;
		try {
			int totalRead = 0;
			while(totalRead < length) {
				if(irxLength > 0) {
					int toRead = Math.min(irxLength, length - totalRead);
					System.arraycopy(irxBuffer, 0, read, totalRead, toRead);
					System.arraycopy(irxBuffer, toRead, irxBuffer, 0, irxLength - toRead);
					totalRead += toRead;
					irxLength -= toRead;
				} else {
					rxBuffer.clear();
					r = LinuxDriver.read(handle, rxBuffer);
					rxBuffer.get(irxBuffer, 0, r);
					irxLength = r;
				}
			}
			return read;
		} catch (RuntimeException var12) {
			var12.printStackTrace();
			throw new IOException("Error when reading " + length + " bytes");
		} finally {
			rxLock.unlock();
		}
	}

	public synchronized void write(byte[] data) throws IOException {
		txLock.lock();
		try {
			int totalWrite = 0;
			while(totalWrite < data.length) {
				int toWrite = Math.min(data.length - totalWrite, BUFFER_SIZE);
				ByteBuffer tmpBuffer = ByteBuffer.allocateDirect(toWrite);
				tmpBuffer.put(data, totalWrite, toWrite);
				totalWrite += LinuxDriver.write(handle, tmpBuffer);
			}
		} catch (RuntimeException var3) {
			var3.printStackTrace();
			throw new IOException("Error when writing " + data.length + " bytes");
		} finally {
			txLock.unlock();
		}
	}

	public void write(String str) throws IOException {
		write(str.getBytes());
	}
}
