package com.embeddedmicro.mojo;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class Theme {
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

	public static void initColors(Display display) {
		keyWordColor = new Color(display, 134, 138, 245);
		valueColor = new Color(display, 151, 245, 134);
		varTypeColor = new Color(display, 102, 226, 226);
		operatorColor = new Color(display, 222, 80, 119);
		commentColor = new Color(display, 150, 150, 150);
		stringColor = new Color(display, 180, 123, 234);
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
	}
	
	public static void dispose(){
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
	}
}
