package com.embeddedmicro.mojo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ArmEvent;
import org.eclipse.swt.events.ArmListener;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;

public class MainWindow implements Callback {
	private static final String VERSION = "0.0.0 Development";

	protected final Display display = Display.getDefault();
	protected Shell shlMojoLoader;
	protected SashForm sideSashForm;
	protected SashForm bottomSashForm;
	protected CTabFolder tabFolder;
	protected Tree tree;
	protected Menu treeMenu;
	protected Project project;

	private boolean opened;

	private int leftWidth, oldLeftWeight;
	private int bottomHeight, oldBottomWeight;
	private ArrayList<StyledCodeEditor> editors;
	private StyledText console;
	private ProjectBuilder projectBuilder;

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

	public MainWindow() {
		opened = false;
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
					boolean max = shlMojoLoader.getMaximized();
					Settings.settings.putBoolean(Settings.MAXIMIZED, max);
					if (!max) {
						Settings.settings.putInt(Settings.WINDOW_HEIGHT,
								r.height);
						Settings.settings
								.putInt(Settings.WINDOW_WIDTH, r.width);
					}
					int[] weights = sideSashForm.getWeights();
					Settings.settings.putInt(
							Settings.FILE_LIST_WIDTH,
							leftWidth = (int) Math.round((double) sideSashForm
									.getClientArea().width
									* (double) weights[0]
									/ (double) (weights[0] + weights[1])));
					weights = bottomSashForm.getWeights();
					Settings.settings
							.putInt(Settings.CONSOLE_HEIGHT,
									bottomHeight = (int) Math.round((double) bottomSashForm
											.getClientArea().height
											* (double) weights[1]
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
		shlMojoLoader.setMaximized(Settings.settings.getBoolean(
				Settings.MAXIMIZED, false));

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
				NewProjectDialog dialog = new NewProjectDialog(shlMojoLoader,
						SWT.DIALOG_TRIM);
				shlMojoLoader.setEnabled(false);
				Project p = dialog.open();
				if (p != null) {
					project = p;
					project.setShell(shlMojoLoader);
					project.setTree(tree);
					project.updateTree();
				}
				shlMojoLoader.setEnabled(true);
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
				if (project.isOpen()) {
					NewSourceDialog dialog = new NewSourceDialog(shlMojoLoader,
							SWT.DIALOG_TRIM);
					shlMojoLoader.setEnabled(false);
					SourceFile file = dialog.open();
					if (file != null) {
						String filePath = null;
						switch (file.type) {
						case SourceFile.SOURCE:
							if ((filePath = project
									.addSourceFile(file.fileName)) == null)
								showError("Could not create new source file!");
							break;
						case SourceFile.CONSTRAINT:
							if ((filePath = project
									.addConstraintFile(file.fileName)) == null)
								showError("Could not create constraint file!");
							break;
						}

						if (filePath != null)
							openFile(filePath);
					}
					try {
						project.saveXML();
					} catch (IOException e) {
						showError("Failed to save project file!");
					}
					shlMojoLoader.setEnabled(true);
				} else {
					showError("A project must be open to add a new file.");
				}
			}
		});

		CustomButton buildbtn = new CustomButton(composite, SWT.NONE);
		buildbtn.setIcon(Images.buildIcon);
		buildbtn.setToolTipText("Build Project");
		buildbtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (projectBuilder.isBuilding()){
					showError("Your project is already building!");
					return;
				}
				projectBuilder.buildProject(project);
			}
		});

		CustomButton loadbtn = new CustomButton(composite, SWT.NONE);
		loadbtn.setIcon(Images.loadIcon);
		loadbtn.setToolTipText("Load to Mojo");
		loadbtn.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				// TODO: Load event
			}
		});

		project = new Project(shlMojoLoader);

		bottomSashForm = new SashForm(shlMojoLoader, SWT.VERTICAL);
		bottomSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));
		bottomSashForm.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int height = bottomSashForm.getClientArea().height;
				int[] weights = bottomSashForm.getWeights();

				double perBottom = (double) bottomHeight / (double) height;

				if (perBottom < 0.8) {
					weights[1] = (int) (perBottom * 1000.0);
					weights[0] = 1000 - weights[1];
				} else {
					weights[1] = 800;
					weights[0] = 200;
				}

				// oldWeights must be set before form.setWeights
				oldBottomWeight = weights[0];
				bottomSashForm.setWeights(weights);
			}
		});
		bottomSashForm.setBackground(Theme.windowBackgroundColor);

		sideSashForm = new SashForm(bottomSashForm, SWT.NONE);
		sideSashForm.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int width = sideSashForm.getClientArea().width;
				int[] weights = sideSashForm.getWeights();

				double perLeft = (double) leftWidth / (double) width;

				if (perLeft < 0.8) {
					weights[0] = (int) (perLeft * 1000.0);
					weights[1] = 1000 - weights[0];
				} else {
					weights[0] = 800;
					weights[1] = 200;
				}

				// oldWeights must be set before form.setWeights
				oldLeftWeight = weights[0];
				sideSashForm.setWeights(weights);
			}
		});
		sideSashForm.setBackground(Theme.windowBackgroundColor);

		tree = new Tree(sideSashForm, SWT.NONE);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = tree.getItem(point);
				if (item != null) {
					if (item.getItemCount() > 0) {
						if (item.getExpanded()) {
							item.setExpanded(false);
						} else {
							item.setExpanded(true);
						}
					} else {
						if (item.getParentItem().getText().equals("Source"))
							openFile(project.getFolder() + File.separatorChar
									+ "source" + File.separatorChar
									+ item.getText());
						else if (item.getParentItem().getText()
								.equals("Constraint"))
							openFile(project.getFolder() + File.separatorChar
									+ "constraint" + File.separatorChar
									+ item.getText());
					}
				}
			}

			@Override
			public void mouseDown(MouseEvent event) {
				if (event.button == 3) { // right click
					Point point = new Point(event.x, event.y);
					final TreeItem item = tree.getItem(point);
					for (MenuItem i : treeMenu.getItems())
						i.dispose();
					if (item != null) {
						MenuItem mi = new MenuItem(treeMenu, SWT.NONE);
						mi.setText("Remove " + item.getText());
						mi.setData(item.getText());
						mi.addSelectionListener(new SelectionListener() {

							@Override
							public void widgetSelected(SelectionEvent e) {
								if (item.getParentItem().getText()
										.equals("Source")) {
									if (!project
											.removeSourceFile((String) ((MenuItem) e
													.getSource()).getData()))
										showError("Could not remove file!");
								} else if (item.getParentItem().getText()
										.equals("Constraint")) {
									if (!project
											.removeConstaintFile((String) ((MenuItem) e
													.getSource()).getData()))
										showError("Could not remove file!");
								}
							}

							@Override
							public void widgetDefaultSelected(SelectionEvent e) {
							}
						});
					}
				}
			}
		});
		tree.setBackground(Theme.editorBackgroundColor);
		tree.setForeground(Theme.editorForegroundColor);
		tree.addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				if ((event.detail & SWT.SELECTED) != 0) {
					GC gc = event.gc;

					Rectangle rect = event.getBounds();
					Color foreground = gc.getForeground();
					Color background = gc.getBackground();
					if (tree.isFocusControl())
						gc.setBackground(Theme.treeSelectedFocusedColor);
					else
						gc.setBackground(Theme.treeSelectedColor);
					gc.fillRectangle(rect);
					// restore colors for subsequent drawing
					gc.setForeground(foreground);
					gc.setBackground(background);
					event.detail &= ~SWT.SELECTED;
				}
			}
		});

		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				int[] weights = sideSashForm.getWeights();
				if (oldLeftWeight != weights[0]) {
					oldLeftWeight = weights[0];
					leftWidth = (int) Math.round((double) sideSashForm
							.getClientArea().width
							* (double) weights[0]
							/ (double) (weights[0] + weights[1]));
				}

				weights = bottomSashForm.getWeights();

				if (oldBottomWeight != weights[1]) {
					oldBottomWeight = weights[1];
					bottomHeight = (int) Math.round((double) bottomSashForm
							.getClientArea().height
							* (double) weights[1]
							/ (double) (weights[0] + weights[1]));
				}
			}
		});

		treeMenu = new Menu(shlMojoLoader, SWT.POP_UP);
		tree.setMenu(treeMenu);
		project.setTree(tree);

		tabFolder = new CTabFolder(sideSashForm, SWT.NULL);
		tabFolder.setSimple(false);
		tabFolder.setDragDetect(true);
		// DragNDropListener dndListner = new DragNDropListener(tabFolder,
		// display);
		// tabFolder.addListener(SWT.DragDetect, dndListner);
		// tabFolder.addListener(SWT.MouseUp, dndListner);
		// tabFolder.addListener(SWT.MouseMove, dndListner);
		// tabFolder.addListener(SWT.MouseExit, dndListner);
		// tabFolder.addListener(SWT.MouseEnter, dndListner);
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

		console = new StyledText(bottomSashForm, SWT.READ_ONLY | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		console.setBackground(Theme.consoleBackgroundColor);
		console.setForeground(Theme.consoleForgoundColor);
		console.setAlwaysShowScrollBars(false);
		bottomSashForm.setWeights(new int[] { 8, 2 });

		openFile(null);

		leftWidth = Settings.settings.getInt(Settings.FILE_LIST_WIDTH, 200);
		bottomHeight = Settings.settings.getInt(Settings.CONSOLE_HEIGHT, 200);
		
		projectBuilder = new ProjectBuilder(display, shlMojoLoader, console);
	}

	private void showError(String error) {
		MessageBox b = new MessageBox(shlMojoLoader, SWT.OK | SWT.ERROR);
		b.setText("Error!");
		b.setMessage(error);
		b.open();
	}

	private boolean openFile(String path) {
		for (StyledCodeEditor editor : editors) {
			if (editor.getFilePath() != null
					&& editor.getFilePath().equals(path)) {
				editor.grabFocus();
				return true;
			}
		}

		final StyledCodeEditor codeEditor = new StyledCodeEditor(tabFolder,
				SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL, tabFolder, path);

		if (codeEditor.isOpen()) {
			int size = editors.size();
			if (size == 1 && !opened && !editors.get(0).isModifed()) {
				editors.get(0).dispose();
				editors.remove(0);
			}
			if (size > 0)
				opened = true;
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
