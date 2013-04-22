package com.embeddedmicro.mojo;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import org.eclipse.swt.widgets.Display;

public class MojoLoader {
	private Display display;
	private TextProgressBar bar;
	private InputStream in;
	private OutputStream out;
	private SerialPort serialPort;
	private Callback callback;
	private boolean terminal;

	public MojoLoader(Display display, TextProgressBar bar, Callback callback,
			boolean terminal) {
		this.display = display;
		this.bar = bar;
		this.callback = callback;
		this.terminal = terminal;
	}

	public static ArrayList<String> listPorts() {
		ArrayList<String> ports = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		java.util.Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier
				.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier portIdentifier = portEnum.nextElement();
			if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				ports.add(portIdentifier.getName());
			}
		}
		return ports;
	}

	private void updateProgress(final float value) {
		if (!terminal) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (bar.isDisposed())
						return;
					bar.setSelection((int) (value * 100.0f));
				}
			});
		} else {
			System.out.print("\r\33[20C" + String.format("%-4s", (int) (value * 100.0f) + "%"));
		}
	}

	private void updateText(final String text) {
		if (!terminal) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (bar.isDisposed())
						return;
					bar.setText(text);
				}
			});
		} else {
			System.out.print("\n" + String.format("%-20s", text));
		}
	}

	private int read(int timeout) throws IOException, TimeoutException {
		long initTime = System.currentTimeMillis();
		while (true)
			if (in.available() > 0)
				return in.read();
			else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {

				}
				if (System.currentTimeMillis() - initTime >= timeout) {
					return -1;
					// throw new TimeoutException(
					// "Timeout while reading from serial port!");
				}
			}
	}

	private void restartMojo() throws InterruptedException {
		serialPort.setDTR(true);
		Thread.sleep(5);
		serialPort.setDTR(false);
		Thread.sleep(5);
		serialPort.setDTR(true);
		Thread.sleep(5);
	}

	public void clearFlash(final String port) {
		new Thread() {
			public void run() {
				updateText("Connecting...");
				updateProgress(0.0f);
				try {
					connect(port);
				} catch (Exception e) {
					onError(e.getMessage());
					return;
				}

				try {
					restartMojo();
				} catch (InterruptedException e) {
					onError(e.getMessage());
					return;
				}

				try {
					updateText("Erasing...");

					while (in.available() > 0)
						in.skip(in.available()); // Flush the buffer

					out.write('E'); // Erase flash

					if (read(1000) != 'D') {
						onError("Mojo did not acknowledge flash erase!");
						return;
					}

					updateText("Done");
					updateProgress(1.0f);

				} catch (IOException | TimeoutException e) {
					onError(e.getMessage());
					return;
				}

				try {
					in.close();
					out.close();
				} catch (IOException e) {
					onError(e.getMessage());
					return;
				}

				serialPort.close();
				if (callback != null)
					callback.onSuccess();
			}
		}.start();
	}

	public void sendBin(final String port, final String binFile,
			final boolean flash, final boolean verify) {
		new Thread() {
			public void run() {
				updateText("Connecting...");
				if (!terminal)
					updateProgress(0.0f);
				try {
					connect(port);
				} catch (Exception e) {
					onError(e.getMessage());
					return;
				}

				File file = new File(binFile);
				InputStream bin = null;
				try {
					bin = new BufferedInputStream(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					onError("The bin file could not be opened!");
					return;
				}

				try {
					restartMojo();
				} catch (InterruptedException e) {
					onError(e.getMessage());
					try {
						bin.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					return;
				}

				try {
					while (in.available() > 0)
						in.skip(in.available()); // Flush the buffer

					updateText("Loading...");

					if (flash) {
						if (verify)
							out.write('V'); // Write to flash
						else
							out.write('F');
					} else {
						out.write('R'); // Write to FPGA
					}

					if (read(1000) != 'R') {
						onError("Mojo did not respond! Make sure the port is correct.");
						bin.close();
						return;
					}

					int length = (int) file.length();

					byte[] buff = new byte[4];

					for (int i = 0; i < 4; i++) {
						buff[i] = (byte) (length >> (i * 8) & 0xff);
					}

					out.write(buff);

					if (read(1000) != 'O') {
						onError("Mojo did not acknowledge transfer size!");
						bin.close();
						return;
					}

					updateProgress(1.0f);

					int num;
					int count = 0;
					int oldCount = 0;
					int percent = length / 100;
					byte[] data = new byte[percent];
					while (true) {
						int avail = bin.available();
						avail = avail > percent ? percent : avail;
						if (avail == 0)
							break;
						int read = bin.read(data, 0, avail);
						out.write(data, 0, read);
						count += read;

						if (count - oldCount > percent) {
							oldCount = count;
							float prog = (float) count / length;
							updateProgress(prog);
						}
					}

					if (read(1000) != 'D') {
						onError("Mojo did not acknowledge the transfer!");
						bin.close();
						return;
					}

					bin.close();

					if (flash && verify) {
						updateText("Verifying...");
						bin = new BufferedInputStream(new FileInputStream(file));
						out.write('S');

						int size = (int) (file.length() + 5);

						int tmp;
						if ((tmp = read(1000)) != 0xAA) {
							onError("Flash does not contain valid start byte! Got: "
									+ tmp);
							bin.close();
							return;
						}

						int flashSize = 0;
						for (int i = 0; i < 4; i++) {
							flashSize |= read(1000) << (i * 8);
						}

						if (flashSize != size) {
							onError("File size mismatch!\nExpected " + size
									+ " and got " + flashSize);
							bin.close();
							return;
						}

						count = 0;
						oldCount = 0;
						while ((num = bin.read()) != -1) {
							int d = read(1000);
							if (d != num) {
								onError("Verification failed at byte " + count
										+ " out of " + length + "\nExpected "
										+ num + " got " + d);
								bin.close();
								return;
							}
							count++;
							if (count - oldCount > percent) {
								oldCount = count;
								float prog = (float) count / length;
								updateProgress(prog);
							}
						}
					}

					if (flash) {
						out.write('L');
						if (read(3000) != 'D') {
							onError("Could not load from flash!");
							bin.close();
							return;
						}
					}

					bin.close();
				} catch (IOException | TimeoutException e) {
					onError(e.getMessage());
					return;
				}

				updateProgress(1.0f);
				updateText("Done");

				try {
					in.close();
					out.close();
				} catch (IOException e) {
					onError(e.getMessage());
					return;
				}

				serialPort.close();
				if (callback != null)
					callback.onSuccess();
				if (terminal)
					System.out.print("\n");
			}
		}.start();
	}

	private void onError(String e) {
		if (callback != null)
			callback.onError(e);
		updateProgress(0.0f);
		updateText("");
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		} catch (IOException err) {
			System.err.print(err);
		}
		if (serialPort != null)
			serialPort.close();
	}

	private void connect(String portName) throws Exception {
		if (portName.equals(""))
			throw new Exception("A serial port must be selected!");
		CommPortIdentifier portIdentifier = CommPortIdentifier
				.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(),
					2000);

			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				in = serialPort.getInputStream();
				out = serialPort.getOutputStream();

			} else {
				System.out.println("Error: Only serial ports can be used!");
			}
		}
	}

}
