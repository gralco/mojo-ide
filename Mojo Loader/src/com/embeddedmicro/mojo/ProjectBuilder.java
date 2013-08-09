package com.embeddedmicro.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.embeddedmicro.mojo.boards.Boards;

public class ProjectBuilder {

	private static final String planAhead = "/opt/Xilinx/14.5/ISE_DS/PlanAhead/bin/planAhead";
	private static final String projectFile = "project.tcl";
	private static final String projectDir = "planAhead";

	private Shell shell;
	private Project project;
	private StyledText console;
	private Display display;
	private String workFolder;
	private Thread thread;

	public ProjectBuilder(Display display, Shell shell, StyledText console) {
		this.display = display;
		this.shell = shell;
		this.console = console;
	}

	public void buildProject(Project project) {
		this.project = project;
		thread = new Thread() {
			public void run() {
				try {
					clearText();
					if (!ProjectBuilder.this.project.isOpen()) {
						showError("A project must be opened before you can build it!");
						return;
					}
					workFolder = ProjectBuilder.this.project.getFolder()
							+ File.separatorChar + "work";
					File destDir = new File(workFolder);
					if (!destDir.exists() || !destDir.isDirectory()) {
						boolean success = destDir.mkdir();
						if (!success) {
							showError("Could not create project folder!");
							return;
						}
					}

					String tclScript = workFolder + File.separatorChar
							+ projectFile;
					FileWriter fstream = new FileWriter(tclScript);
					BufferedWriter out = new BufferedWriter(fstream);

					generateProjectFile(out);

					ProcessBuilder pb = new ProcessBuilder(planAhead,
							"-nojournal", "-nolog", "-mode", "batch",
							"-source", tclScript);

					final Process p = pb.start();

					new Thread() {
						public void run() {
							BufferedReader br = new BufferedReader(
									new InputStreamReader(p.getInputStream()));
							String line;
							try {
								while ((line = br.readLine()) != null) {
									addText(line + System.lineSeparator(),
											false);
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}.start();

					new Thread() {
						public void run() {
							BufferedReader br = new BufferedReader(
									new InputStreamReader(p.getErrorStream()));
							String line;
							try {
								while ((line = br.readLine()) != null) {
									addText(line + System.lineSeparator(), true);
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}.start();
					p.waitFor();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		thread.start();
	}

	public boolean isBuilding() {
		return thread != null && thread.isAlive();
	}

	private void clearText() {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				console.setText("");
			}
		});
	}

	private void addText(final String text, final boolean red) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				console.append(text);
				if (red) {
					int end = console.getCharCount();
					StyleRange styleRange = new StyleRange();
					styleRange.start = end - text.length();
					styleRange.length = text.length();
					styleRange.foreground = Theme.errorTextColor;
					console.setStyleRange(styleRange);
				}
				console.setTopIndex(console.getLineCount() - 1);
			}
		});
	}

	private void showError(final String error) {
		display.asyncExec(new Runnable() {

			@Override
			public void run() {
				MessageBox b = new MessageBox(shell, SWT.OK | SWT.ERROR);
				b.setText("Error building the project");
				b.setMessage(error);
				b.open();
			}

		});

	}

	private String getSpacedList(ArrayList<String> list, String prefix) {
		StringBuilder builder = new StringBuilder();
		for (String s : list) {
			builder.append(prefix).append(s).append(" ");
		}
		builder.deleteCharAt(builder.length() - 1);
		return builder.toString();
	}

	private void generateProjectFile(BufferedWriter file) throws IOException {
		final String nl = System.lineSeparator();
		file.write("set projDir " + workFolder + File.separatorChar
				+ projectDir + nl);
		file.write("set projName " + project.getProjectName() + nl);
		file.write("set topName top" + nl);
		file.write("set device "
				+ Boards.getByName(project.getBoardType()).getFPGAName() + nl);
		file.write("if {[file exists $projDir/$projName]} { file delete -force $projDir/$projName }"
				+ nl);
		file.write("create_project $projName $projDir/$projName -part $device"
				+ nl);
		file.write("set_property design_mode RTL [get_filesets sources_1]" + nl);
		file.write("set verilogSources \""
				+ getSpacedList(project.getSourceFiles(),
						project.getSourceFolder() + File.separatorChar) + "\""
				+ nl);
		file.write("import_files -fileset [get_filesets sources_1] -force -norecurse $verilogSources"
				+ nl);
		file.write("set ucfSources \""
				+ getSpacedList(project.getConstraintFiles(),
						project.getConstraintFolder() + File.separatorChar)
				+ "\"" + nl);
		file.write("import_files -fileset [get_filesets constrs_1] -force -norecurse $ucfSources"
				+ nl);
		file.write("set_property top " + project.getTop().split("\\.")[0]
				+ " [get_property srcset [current_run]]" + nl);
		file.write("set_property -name {steps.bitgen.args.More Options} -value {-g Binary:Yes} -objects [get_runs impl_1]"
				+ nl);
		file.write("launch_runs -runs synth_1" + nl);
		file.write("wait_on_run synth_1" + nl);
		file.write("launch_runs -runs impl_1" + nl);
		file.write("wait_on_run impl_1" + nl);
		file.write("launch_runs impl_1 -to_step Bitgen" + nl);
		file.write("wait_on_run impl_1" + nl);

		file.close();
	}

}
