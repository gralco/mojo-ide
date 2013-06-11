package com.embeddedmicro.mojo;

import java.util.ArrayList;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

public class MainWindow implements Callback {
	private static final String VERSION = "1.1.2";
	private static final String PORT_PREF = "PORT";
	
	protected final Display display = Display.getDefault();
	protected Shell shlMojoLoader;
	private Text text;
	private Button btnOpen;
	private Button btnStore;
	private Button btnVerify;
	private Button btnLoad;
	private Combo combo;
	private MojoLoader loader;
	private Button btnClear;
	
	private Preferences prefs = Preferences
			.userNodeForPackage(com.embeddedmicro.mojo.MainWindow.class);

	/**
	 * Launch the application.
	 * 
	 * @param args
	 * @wbp.parser.entryPoint
	 */
	public static void main(String[] args) {
		/*
		boolean term = false;
		String port = null;
		String binFile = null;
		boolean flash = false;
		boolean verify = false;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-t"))
				term = true;
			else if (args[i].equals("-p") && i < args.length - 1)
				port = args[++i];
			else if (args[i].equals("-b") && i < args.length - 1)
				binFile = args[++i];
			else if (args[i].equals("-f"))
				flash = true;
			else if (args[i].equals("-v"))
				verify = true;
		}
		if (term) {
			if (port == null) {
				System.err.println("You must specify a port using the -p flag!");
				return;
			}
			if (binFile == null){
				System.err.println("You must specify a bin file using the -b flag!");
				return;
			}
			
			MojoLoader loader = new MojoLoader(null, null, null, true);
			loader.sendBin(port, binFile, flash, verify);
		} else { */
			try {
				MainWindow window = new MainWindow();
				window.open();
			} catch (Exception e) {
				e.printStackTrace();
			}
		//}
		return;
	}

	/**
	 * Open the window.
	 */
	public void open() {
		createContents();
		shlMojoLoader.open();
		shlMojoLoader.layout();
		while (!shlMojoLoader.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	private void updatePorts(Combo box) {
		ArrayList<String> ports = MojoLoader.listPorts();
		if (ports.size() != 0) {
			Object[] array = ports.toArray();
			String[] names = new String[array.length];
			for (int i = 0; i < array.length; i++)
				names[i] = (String) array[i];
			box.setItems(names);
		} else {
			box.removeAll();
			box.add("");
		}
	}

	/**
	 * Create contents of the window.
	 * @wbp.parser.entryPoint
	 */
	protected void createContents() {
		shlMojoLoader = new Shell();

		shlMojoLoader.setImage(SWTResourceManager.getImage(MainWindow.class,
				"/resources/icon.png"));
		shlMojoLoader.setSize(450, 178);
		shlMojoLoader.setMinimumSize(450, 178);
		shlMojoLoader.setText("Mojo Loader Version " + VERSION);
		GridLayout gl_shlMojoLoader = new GridLayout(4, false);
		gl_shlMojoLoader.marginHeight = 10;
		gl_shlMojoLoader.marginWidth = 10;
		shlMojoLoader.setLayout(gl_shlMojoLoader);

		Label lblNewLabel = new Label(shlMojoLoader, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText("Serial Port:");

		combo = new Combo(shlMojoLoader, SWT.READ_ONLY);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (!combo.getText().equals(""))
					prefs.put(PORT_PREF, combo.getText());
			}
		});
		combo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				updatePorts(combo);
			}
		});
		//combo.removeAll();
		combo.add(prefs.get(PORT_PREF, ""));
		//updatePorts(combo);
		GridData gd_combo = new GridData(SWT.FILL, SWT.CENTER, true, false, 3,
				1);
		gd_combo.heightHint = 32;
		combo.setLayoutData(gd_combo);
		combo.select(0);

		text = new Text(shlMojoLoader, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		btnOpen = new Button(shlMojoLoader, SWT.NONE);
		btnOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shlMojoLoader, SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.bin", "*" });
				String result = dialog.open();
				if (result != null)
					text.setText(result);
			}
		});
		btnOpen.setText("Open Bin File");

		btnStore = new Button(shlMojoLoader, SWT.CHECK);
		btnStore.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnVerify.setEnabled(btnStore.getSelection());
			}
		});
		btnStore.setSelection(true);
		btnStore.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false,
				2, 1));
		btnStore.setText("Store to Flash");

		btnVerify = new Button(shlMojoLoader, SWT.CHECK);
		btnVerify.setSelection(true);
		btnVerify.setText("Verify Flash");

		btnClear = new Button(shlMojoLoader, SWT.NONE);
		btnClear.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		btnClear.setText("Erase");
		btnClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setUI(false);
				loader.clearFlash(combo.getText());
			}
		});

		final TextProgressBar progressBar = new TextProgressBar(shlMojoLoader,
				SWT.NONE);
		progressBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
				false, 3, 1));

		loader = new MojoLoader(display, progressBar, MainWindow.this, false);

		btnLoad = new Button(shlMojoLoader, SWT.NONE);
		btnLoad.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setUI(false);
				loader.sendBin(combo.getText(), text.getText(),
						btnStore.getSelection(), btnVerify.getSelection());
			}
		});
		btnLoad.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		btnLoad.setText("Load");

	}

	private void setUI(boolean active) {
		btnLoad.setEnabled(active);
		btnOpen.setEnabled(active);
		btnStore.setEnabled(active);
		btnVerify.setEnabled(btnStore.getSelection() && active);
		btnClear.setEnabled(active);
		combo.setEnabled(active);
		text.setEnabled(active);
	}

	@Override
	public void onSuccess() {
		display.asyncExec(new Runnable() {
			public void run() {
				setUI(true);
			}
		});
	}

	@Override
	public void onError(final String error) {
		System.out.println(error);

		display.asyncExec(new Runnable() {
			public void run() {
				// Message with ok and cancel button and info icon
				MessageBox dialog = new MessageBox(shlMojoLoader,
						SWT.ICON_ERROR | SWT.OK);
				dialog.setText("Error!");
				dialog.setMessage(error);
				dialog.open();

				setUI(true);
			}
		});
	}
}
