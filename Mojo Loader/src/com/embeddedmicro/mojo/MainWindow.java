package com.embeddedmicro.mojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

public class MainWindow implements Callback {
	private static final String VERSION = "0.0.0 Development";

	protected final Display display = Display.getDefault();
	protected Shell shlMojoLoader;
	protected SashForm sashForm;
	protected CTabFolder tabFolder;
	protected Project project;

	private int leftWidth, oldWeight;
	private ArrayList<StyledCodeEditor> editors;

	// private MojoLoader loader;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 * @wbp.parser.entryPoint
	 */
	public static void main(String[] args) {
		if (parseCommand(args))
			return;

		try {
			MainWindow window = new MainWindow();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	private static boolean parseCommand(String[] args) {
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
				System.err
						.println("You must specify a port using the -p flag!");
				return true;
			}
			if (binFile == null) {
				System.err
						.println("You must specify a bin file using the -b flag!");
				return true;
			}

			MojoLoader loader = new MojoLoader(null, null, null, true);
			loader.sendBin(port, binFile, flash, verify);
			return true;
		}
		return false;
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

	private void updatePorts(MenuItem portMenu) {
		ArrayList<String> ports = MojoLoader.listPorts();
		Menu menu = portMenu.getMenu();
		MenuItem[] items = menu.getItems();
		for (MenuItem i : items)
			i.dispose();
		if (ports.size() != 0) {
			Object[] array = ports.toArray();

			for (int i = 0; i < array.length; i++) {
				MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
				menuItem.setText((String) array[i]);
				menuItem.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetDefaultSelected(SelectionEvent event) {
						widgetSelected(event);
					}

					@Override
					public void widgetSelected(SelectionEvent event) {
						Settings.settings.put(Settings.MOJO_PORT,
								((MenuItem) event.widget).getText());
					}
				});
			}
		} else {
			MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
			menuItem.setText("No Serial Ports!");
		}
	}

	private boolean saveAll() {
		for (StyledCodeEditor editor : editors) {
			switch (saveEditor(editor)) {
			case SWT.YES:
			case SWT.NO:
				continue;
			case SWT.CANCEL:
			case SWT.ERROR:
				return false;
			}
		}
		return true;
	}

	private int saveEditor(StyledCodeEditor editor) {
		if (editor.isModifed()) {
			MessageBox dialog = new MessageBox(shlMojoLoader, SWT.ICON_QUESTION
					| SWT.YES | SWT.NO | SWT.CANCEL);
			dialog.setText(editor.getFileName() + " has been modified");
			dialog.setMessage("Do you want to save the changes to "
					+ editor.getFileName() + "?");

			int returnCode = dialog.open();

			switch (returnCode) {
			case SWT.YES:
				if (!editor.save()) {
					System.err.println("Could not save file!");
					return SWT.ERROR;
				}
				return SWT.YES;
			case SWT.NO:
				return SWT.NO;
			case SWT.CANCEL:
			default:
				return SWT.CANCEL;
			}
		}
		return SWT.YES;
	}

	private boolean closeEditor(StyledCodeEditor editor) {
		switch (saveEditor(editor)) {
		case SWT.YES:
		case SWT.NO:
			editors.remove(editor);
			return true;
		case SWT.CANCEL:
		case SWT.ERROR:
			return false;
		}
		return false;
	}

	/**
	 * Create contents of the window.
	 * 
	 * @wbp.parser.entryPoint
	 */
	protected void createContents() {
		Theme.initColors(display);
		Images.loadImages(display);
		editors = new ArrayList<StyledCodeEditor>();
		shlMojoLoader = new Shell();
		shlMojoLoader.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				if (!saveAll()) {
					e.doit = false;
					return;
				}

				shlMojoLoader.getImage().dispose();
				try {
					Rectangle r = shlMojoLoader.getBounds();
					Settings.settings.putInt(Settings.WINDOW_HEIGHT, r.height);
					Settings.settings.putInt(Settings.WINDOW_WIDTH, r.width);
					int[] weights = sashForm.getWeights();
					Settings.settings.putInt(
							Settings.FILE_LIST_WIDTH,
							leftWidth = (int) Math.round((double) sashForm
									.getClientArea().width
									* (double) weights[0]
									/ (double) (weights[0] + weights[1])));
					Settings.settings.flush();
				} catch (BackingStoreException e1) {
					System.err.println("Failed to save settings! "
							+ e1.getMessage());
				}
				Theme.dispose();
			}
		});

		shlMojoLoader.setImage(SWTResourceManager.getImage(MainWindow.class,
				"/resources/icon.png"));
		int height = Settings.settings.getInt(Settings.WINDOW_HEIGHT, 700);
		int width = Settings.settings.getInt(Settings.WINDOW_WIDTH, 1000);
		shlMojoLoader.setSize(width, height);
		shlMojoLoader.setMinimumSize(450, 178);
		shlMojoLoader.setText("Mojo Loader Version " + VERSION);
		shlMojoLoader.setLayout(new GridLayout(1, false));

		shlMojoLoader.setBackground(Theme.windowBackgroundColor);
		shlMojoLoader.setForeground(Theme.windowForgroundColor);

		Menu menu = new Menu(shlMojoLoader, SWT.BAR);
		shlMojoLoader.setMenuBar(menu);

		MenuItem mntmFile = new MenuItem(menu, SWT.CASCADE);
		mntmFile.setText("File");

		Menu menu_1 = new Menu(mntmFile);
		mntmFile.setMenu(menu_1);
		
		MenuItem mntmNewProject = new MenuItem(menu_1, SWT.NONE);
		mntmNewProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				NewProjectDialog dialog = new NewProjectDialog(shlMojoLoader, SWT.DIALOG_TRIM);
				dialog.open();
				System.out.println("Closed");
			}
		});
		mntmNewProject.setText("New Project...");

		MenuItem mntmOpenProject = new MenuItem(menu_1, SWT.NONE);
		mntmOpenProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shlMojoLoader, SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.mojo", "*" });
				String path = dialog.open();
				if (path != null) {
					try {
						project.openXML(path);
					} catch (ParseException e1) {
						MessageBox box = new MessageBox(shlMojoLoader,
								SWT.ICON_ERROR | SWT.OK);
						box.setText("Error opening file!");
						box.setMessage("Encountered an error while parsing "
								+ path + " the error was: " + e1.getMessage());
						box.open();
					} catch (IOException e1) {
						MessageBox box = new MessageBox(shlMojoLoader,
								SWT.ICON_ERROR | SWT.OK);
						box.setText("Error opening file!");
						box.setMessage("Encountered an error while opening "
								+ path + " the error was: " + e1.getMessage());
						box.open();
					}

					project.updateTree();
				}
			}
		});
		mntmOpenProject.setText("Open Project...");

		MenuItem mntmOpenFile = new MenuItem(menu_1, SWT.NONE);
		mntmOpenFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(shlMojoLoader, SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.v", "*" });
				String path = dialog.open();
				if (path != null) {
					openFile(path);
				}
			}
		});
		mntmOpenFile.setText("Open File...");

		MenuItem mntmSaveFile = new MenuItem(menu_1, SWT.NONE);
		mntmSaveFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				((StyledCodeEditor) (tabFolder.getSelection().getControl()))
						.save();
			}
		});
		mntmSaveFile.setText("Save");

		MenuItem mntmSettings = new MenuItem(menu, SWT.CASCADE);
		mntmSettings.setText("Settings");

		Menu menu_2 = new Menu(mntmSettings);
		mntmSettings.setMenu(menu_2);

		final MenuItem mntmSerialPort_1 = new MenuItem(menu_2, SWT.CASCADE);
		mntmSerialPort_1.addArmListener(new ArmListener() {
			public void widgetArmed(ArmEvent arg0) {
				updatePorts(mntmSerialPort_1);
			}
		});
		mntmSerialPort_1.setText("Serial Port");

		Menu menu_3 = new Menu(mntmSerialPort_1);
		mntmSerialPort_1.setMenu(menu_3);

		MenuItem mntmNoSerialPorts = new MenuItem(menu_3, SWT.RADIO);
		mntmNoSerialPorts.setText("No Serial Ports!");

		Composite composite = new Composite(shlMojoLoader, SWT.NONE);
		composite.setBackground(Theme.windowBackgroundColor);
		composite.setForeground(Theme.windowForgroundColor);
		RowLayout rl_composite = new RowLayout(SWT.HORIZONTAL);
		composite.setLayout(rl_composite);
		
		CustomButton newbtn = new CustomButton(composite, SWT.NONE);
		newbtn.setIcon(Images.fileIcon);
		newbtn.setToolTipText("New File");
		newbtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				//TODO: New event
			}
		});
		
		CustomButton buildbtn = new CustomButton(composite, SWT.NONE);
		buildbtn.setIcon(Images.buildIcon);
		buildbtn.setToolTipText("Build Project");
		buildbtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				//TODO: Build event
			}
		});

		CustomButton loadbtn = new CustomButton(composite, SWT.NONE);
		loadbtn.setIcon(Images.loadIcon);
		loadbtn.setToolTipText("Load to Mojo");
		loadbtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				//TODO: Load event
			}
		});
		
		sashForm = new SashForm(shlMojoLoader, SWT.NONE);
		sashForm.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int width = sashForm.getClientArea().width;
				int[] weights = sashForm.getWeights();

				double perLeft = (double) leftWidth / (double) width;

				if (perLeft < 0.8) {
					weights[0] = (int) (perLeft * 1000.0);
					weights[1] = 1000 - weights[0];
				} else {
					weights[0] = 800;
					weights[1] = 200;
				}

				// oldWeights must be set before form.setWeights
				oldWeight = weights[0];
				sashForm.setWeights(weights);
			}
		});
		GridData gd_sashForm = new GridData(SWT.FILL, SWT.FILL, true, true, 1,
				1);
		gd_sashForm.widthHint = 867;
		sashForm.setLayoutData(gd_sashForm);
		sashForm.setBackground(Theme.windowBackgroundColor);

		project = new Project(sashForm, SWT.NONE);
		project.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int[] weights = sashForm.getWeights();
				if (oldWeight != weights[0]) {
					oldWeight = weights[0];
					leftWidth = (int) Math.round((double) sashForm
							.getClientArea().width
							* (double) weights[0]
							/ (double) (weights[0] + weights[1]));
				}
			}
		});

		tabFolder = new CTabFolder(sashForm, SWT.BORDER);
		tabFolder.setSimple(false);
		tabFolder.setDragDetect(true);
		DragNDropListener dndListner = new DragNDropListener(tabFolder, display);
		tabFolder.addListener(SWT.DragDetect, dndListner);
		tabFolder.addListener(SWT.MouseUp, dndListner);
		tabFolder.addListener(SWT.MouseMove, dndListner);
		tabFolder.addListener(SWT.MouseExit, dndListner);
		tabFolder.addListener(SWT.MouseEnter, dndListner);
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			@Override
			public void close(CTabFolderEvent event) {
				int tab = editors.indexOf(((CTabItem) event.item).getControl());
				StyledCodeEditor editor = editors.get(tab);
				if (!closeEditor(editor)) {
					event.doit = false;
				}
			}
		});
		tabFolder.setBackground(Theme.tabBackgroundColor);
		tabFolder.setForeground(Theme.tabForegroundColor);
		tabFolder.setSelectionBackground(Theme.tabSelectedBackgroundColor);
		tabFolder.setSelectionForeground(Theme.tabSelectedForegroundColor);
		openFile(null);

		leftWidth = Settings.settings.getInt(Settings.FILE_LIST_WIDTH, 200);
	}

	private boolean openFile(String path) {
		final StyledCodeEditor codeEditor = new StyledCodeEditor(tabFolder,
				SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL, tabFolder, path);

		if (codeEditor.isOpen()) {
			if (editors.size() == 1
					&& editors.get(0).getFileName().equals("Untitled")
					&& !editors.get(0).isModifed()) {
				editors.get(0).dispose();
				editors.remove(0);
			}
			editors.add(codeEditor);
			return true;
		}
		return false;
	}

	private void setUI(boolean active) {
		/*
		 * btnLoad.setEnabled(active); btnOpen.setEnabled(active);
		 * btnStore.setEnabled(active);
		 * btnVerify.setEnabled(btnStore.getSelection() && active);
		 * btnClear.setEnabled(active); combo.setEnabled(active);
		 * text.setEnabled(active);
		 */
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
