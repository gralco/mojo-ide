package com.embeddedmicro.mojo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class Project {
	private ArrayList<String> sourceFiles;
	private ArrayList<String> ucfFiles;
	private String topSource;
	private String projectName;
	private String projectFile;
	private boolean open;
	private Tree tree;

	public Project(Composite parent, int style) {
		tree = new Tree(parent, style);
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

		sourceFiles = new ArrayList<>();
		ucfFiles = new ArrayList<>();
		open = false;
	}

	public void addControlListener(ControlListener listener) {
		tree.addControlListener(listener);
	}

	public boolean isOpen() {
		return open;
	}

	public void addSourceFile(String file) {
		sourceFiles.add(file);
	}

	public void addUCFFile(String file) {
		ucfFiles.add(file);
	}

	public void setProjectName(String name) {
		projectName = name;
	}

	public void updateTree() {
		if (open) {
			tree.clearAll(true);
			TreeItem project = new TreeItem(tree, SWT.NONE);
			project.setText(projectName);

			TreeItem sourceBranch = new TreeItem(project, SWT.NONE);
			sourceBranch.setText("Source");

			TreeItem ucfBranch = new TreeItem(project, SWT.NONE);
			ucfBranch.setText("Constraints");

			for (String source : sourceFiles) {
				new TreeItem(sourceBranch, SWT.NONE).setText(source);
			}
			for (String ucf : ucfFiles) {
				new TreeItem(ucfBranch, SWT.NONE).setText(ucf);
			}
			tree.showItem(sourceBranch);
		}
	}

	public void openXML(String xmlPath) throws ParseException, IOException {
		open = false;
		sourceFiles.clear();
		ucfFiles.clear();
		topSource = null;
		projectName = null;
		projectFile = xmlPath;

		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(xmlPath);

		Document document;
		try {
			document = (Document) builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new ParseException(e.getMessage());
		}
		Element project = document.getRootElement();

		if (!project.getName().equals(Tags.project)) {
			throw new ParseException("Root element not project tag");
		}

		Attribute projName = project.getAttribute(Tags.Attributes.name);
		if (projName == null) {
			throw new ParseException("Project name is missing");
		}
		projectName = projName.getValue();

		final List<Element> list = project.getChildren();
		for (int i = 0; i < list.size(); i++) {
			Element node = list.get(i);

			switch (node.getName()) {
			case Tags.files:
				final List<Element> files = node.getChildren();
				for (int j = 0; j < files.size(); j++) {
					Element file = files.get(j);
					switch (file.getName()) {
					case Tags.source:
						Attribute att = file.getAttribute(Tags.Attributes.top);
						if (att != null && att.getValue().equals("true")) {
							if (topSource != null)
								throw new ParseException(
										"Multiple \"top\" source files");
							topSource = file.getText();
						}
						sourceFiles.add(file.getText());
						break;
					case Tags.ucf:
						ucfFiles.add(file.getText());
						break;
					default:
						throw new ParseException("Unknown tag "
								+ file.getName());
					}
				}
				break;
			default:
				throw new ParseException("Unknown tag " + node.getName());
			}
		}
		open = true;
	}

	public void saveXML() throws IOException {
		saveXML(projectFile);
	}

	public void saveXML(String file) throws IOException {
		Element project = new Element(Tags.project);

		project.setAttribute(new Attribute(Tags.Attributes.name, projectName));
		Document doc = new Document(project);

		Element source = new Element(Tags.files);
		for (String sourceFile : sourceFiles) {
			Element ele = new Element(Tags.source).setText(sourceFile);
			if (sourceFile == topSource)
				ele.setAttribute(new Attribute(Tags.Attributes.top, "true"));
			source.addContent(ele);
		}

		for (String ucfFile : ucfFiles) {
			source.addContent(new Element(Tags.ucf).setText(ucfFile));
		}

		project.addContent(source);

		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		xmlOutput.output(doc, new FileWriter(file));
	}
}
