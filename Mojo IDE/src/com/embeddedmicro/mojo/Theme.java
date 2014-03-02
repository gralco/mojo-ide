package com.embeddedmicro.mojo;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class Theme {
	public static boolean set;
	public static Color moduleColor;
	public static Color keyWordColor;
	public static Color valueColor;
	public static Color varTypeColor;
	public static Color operatorColor;
	public static Color commentColor;
	public static Color stringColor;
	public static Color instantiationColor;
	public static Color editorBackgroundColor;
	public static Color editorForegroundColor;
	public static Color bulletTextColor;
	public static Color highlightedLineColor;
	public static Color tabBackgroundColor;
	public static Color tabForegroundColor;
	public static Color tabSelectedForegroundColor;
	public static Color tabSelectedBackgroundColor;
	public static Color windowBackgroundColor;
	public static Color windowForgroundColor;
	public static Color treeSelectedFocusedColor;
	public static Color treeSelectedColor;
	public static Color toolBarHoverColor;
	public static Color toolBarClickColor;
	public static Color consoleBackgroundColor;
	public static Color consoleForgoundColor;
	public static Color errorTextColor;

	public static void initColors(Display display) {
		moduleColor = new Color(display, 10, 191, 10);
		keyWordColor = new Color(display, 10, 191, 191);
		valueColor = new Color(display, 10, 191, 100);
		varTypeColor = new Color(display, 10, 141, 191);
		operatorColor = new Color(display, 191, 10, 100);
		commentColor = new Color(display, 150, 150, 150);
		stringColor = new Color(display, 191, 191, 10);
		instantiationColor = new Color(display, 234, 182, 123);
		editorBackgroundColor = new Color(display, 40, 40, 35);
		editorForegroundColor = new Color(display, 255, 255, 255);
		bulletTextColor = new Color(display, 200, 200, 200);
		highlightedLineColor = new Color(display, 75, 75, 60);
		tabSelectedForegroundColor = bulletTextColor;
		tabSelectedBackgroundColor = editorBackgroundColor;
		windowBackgroundColor = new Color(display, 80, 80, 75);
		windowForgroundColor = new Color(display, 10, 10, 8);
		tabBackgroundColor = new Color(display, 100, 100, 90);
		tabForegroundColor = windowForgroundColor;
		treeSelectedFocusedColor = new Color(display, 38, 188, 38);
		treeSelectedColor = highlightedLineColor;
		toolBarHoverColor = treeSelectedFocusedColor;
		toolBarClickColor = new Color(display, 35, 160, 35);
		consoleBackgroundColor = editorBackgroundColor;
		consoleForgoundColor = editorForegroundColor;
		errorTextColor = new Color(display, 255, 25, 25);
		set = true;
	}

	public static void dispose() {
		moduleColor.dispose();
		keyWordColor.dispose();
		valueColor.dispose();
		varTypeColor.dispose();
		operatorColor.dispose();
		commentColor.dispose();
		stringColor.dispose();
		instantiationColor.dispose();
		editorBackgroundColor.dispose();
		editorForegroundColor.dispose();
		bulletTextColor.dispose();
		highlightedLineColor.dispose();
		tabSelectedBackgroundColor.dispose();
		tabSelectedForegroundColor.dispose();
		windowBackgroundColor.dispose();
		windowForgroundColor.dispose();
		tabBackgroundColor.dispose();
		tabForegroundColor.dispose();
		treeSelectedFocusedColor.dispose();
		treeSelectedColor.dispose();
		toolBarHoverColor.dispose();
		toolBarClickColor.dispose();
		consoleBackgroundColor.dispose();
		consoleForgoundColor.dispose();
		errorTextColor.dispose();
		set = false;
	}
}
