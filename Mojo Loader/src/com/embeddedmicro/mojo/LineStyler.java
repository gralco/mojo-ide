package com.embeddedmicro.mojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GlyphMetrics;

public class LineStyler implements LineStyleListener, ModifyListener {

	private StyledText styledText;
	private SyntaxFormat syntaxFormat;
	private LinkedList<int[]> commentOffsets;

	private static final String COMMENT_START = "/*";
	private static final String COMMENT_END = "*/";

	public LineStyler(StyledText text) {
		styledText = text;
		

		syntaxFormat = new SyntaxFormat(text.getDisplay());

		commentOffsets = new LinkedList<int[]>();
	}

	@Override
	public void lineGetStyle(LineStyleEvent event) {
		// Set the line number
		int activeLine = styledText
				.getLineAtOffset(styledText.getCaretOffset());
		int currentLine = styledText.getLineAtOffset(event.lineOffset);
		event.bulletIndex = currentLine;

		int width = 36;
		if (styledText.getLineCount() > 999)
			width = (int) ((Math.floor(Math.log10(styledText.getLineCount())) + 1) * 12);

		// Set the style, 12 pixles wide for each digit
		StyleRange style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, width);

		if (activeLine == currentLine) {
			style.background = Theme.highlightedLineColor;
		}

		style.foreground = Theme.bulletTextColor;

		// Create and set the bullet
		event.bullet = new Bullet(ST.BULLET_NUMBER, style);

		event.styles = matchKeywords(event);
	}

	/*
	 * private boolean styleOverlaps(StyleRange sr1, StyleRange sr2) { return
	 * false; }
	 */

	public boolean styleContains(StyleRange sr, int offset) {
		if (sr.start <= offset && (sr.start + sr.length) > offset)
			return true;
		return false;
	}

	public boolean styleContains(ArrayList<StyleRange> styles, int offset) {
		for (StyleRange sr : styles) {
			if (styleContains(sr, offset))
				return true;
		}
		return false;
	}

	private StyleRange findLineComment(String lineText, int lineOffset,
			ArrayList<StyleRange> strings, ArrayList<StyleRange> multiline) {
		int start;
		int offset = 0;

		while ((start = lineText.indexOf("//", offset)) != -1) {
			// is the comment in a string?
			if (strings != null && styleContains(strings, start + lineOffset)) { 
				offset = start + 2;
				continue;
			}
			if (multiline != null
					&& styleContains(multiline, offset + lineOffset)) {
				offset = start + 4; // the shortest distance must be "//*/"
				continue;
			}
			// "*//" is not a comment
			if (start > 0 && lineText.charAt(start - 1) == '*') { 
				offset = start + 1;
				continue;
			}

			StyleRange style = new StyleRange();
			style.start = start + lineOffset;
			style.length = lineText.length() - start;
			style.foreground = syntaxFormat.commentFormat.color;
			style.fontStyle = syntaxFormat.commentFormat.fontStyle;
			return style;
		}
		return null;
	}

	private void findStyles(Matcher matcher, ArrayList<StyleRange> styles,
			int lineOffset, Style format, ArrayList<StyleRange> ignoreList) {
		while (matcher.find()) {
			if (ignoreList != null)
				if (styleContains(ignoreList, matcher.start() + lineOffset))
					continue;

			StyleRange style = new StyleRange();
			style.start = matcher.start() + lineOffset;
			style.length = matcher.end() - matcher.start();

			style.foreground = format.color;
			style.fontStyle = format.fontStyle;
			styles.add(style);
		}
	}

	private StyleRange[] matchKeywords(LineStyleEvent event) {
		ArrayList<StyleRange> multilineComments = getMultilineComments(
				event.lineText, event.lineOffset);

		ArrayList<StyleRange> styles = new ArrayList<>();
		ArrayList<StyleRange> strings = new ArrayList<>();

		Pattern stringPattern = Pattern
				.compile(syntaxFormat.stringFormat.regex[0]);
		Matcher stringMatcher = stringPattern.matcher(event.lineText);

		findStyles(stringMatcher, strings, event.lineOffset,
				syntaxFormat.stringFormat, multilineComments);

		StyleRange comment = findLineComment(event.lineText, event.lineOffset,
				strings, multilineComments);

		String line;
		if (comment != null) {
			line = event.lineText
					.substring(0, comment.start - event.lineOffset);
			strings.clear();
			stringMatcher = stringPattern.matcher(line);
			findStyles(stringMatcher, strings, event.lineOffset,
					syntaxFormat.stringFormat, multilineComments);
			styles.add(comment);
		} else {
			line = event.lineText;
		}

		ArrayList<StyleRange> excludeList = new ArrayList<StyleRange>();
		excludeList.addAll(strings);
		excludeList.addAll(multilineComments);

		for (Style format : syntaxFormat.formats) {
			for (String regex : format.regex) {
				Pattern p = Pattern.compile(regex);
				Matcher matcher = p.matcher(line);
				findStyles(matcher, styles, event.lineOffset, format,
						excludeList);
			}
		}

		styles.addAll(excludeList);
		// styles.
		Collections.sort(styles, new Comparator<StyleRange>() {
			@Override
			public int compare(StyleRange o1, StyleRange o2) {
				return o1.start - o2.start;
			}
		});
		StyleRange[] styleRange = new StyleRange[styles.size()];
		styles.toArray(styleRange);
		return styleRange;
	}

	private boolean commentsEqual(LinkedList<int[]> newComments,
			LinkedList<int[]> oldComments) {
		if (newComments.size() != oldComments.size())
			return false;
		int i;
		for (i = 0; i < newComments.size(); i++) {
			int[] nc = newComments.get(i);
			int[] oc = oldComments.get(i);
			if (nc[0] != oc[0] || nc[1] != oc[1])
				return false;
		}
		return true;
	}

	public ArrayList<StyleRange> getIgnoredStyles(String lineText,
			int lineOffset) {
		ArrayList<StyleRange> ignoreList = new ArrayList<>();

		Pattern stringPattern = Pattern
				.compile(syntaxFormat.stringFormat.regex[0]);
		Matcher stringMatcher = stringPattern.matcher(lineText);

		findStyles(stringMatcher, ignoreList, lineOffset,
				syntaxFormat.stringFormat, null);

		StyleRange comment = findLineComment(lineText, lineOffset, ignoreList,
				null);

		String line;
		if (comment != null) {
			line = lineText.substring(0, comment.start - lineOffset);
			ignoreList.clear();
			stringMatcher = stringPattern.matcher(line);
			findStyles(stringMatcher, ignoreList, lineOffset,
					syntaxFormat.stringFormat, null);
			ignoreList.add(comment);
		}
		return ignoreList;
	}
	
	private void updateMultiLineComments(String text){
		// Clear any stored offsets
				LinkedList<int[]> comments = new LinkedList<int[]>();

				// Go through all the instances of COMMENT_START
				for (int pos = text.indexOf(COMMENT_START); pos > -1; pos = text
						.indexOf(COMMENT_START, pos)) {

					int lineNum = styledText.getLineAtOffset(pos);
					int offset = styledText.getOffsetAtLine(lineNum);
					String line = styledText.getLine(lineNum);

					ArrayList<StyleRange> ignoreList = getIgnoredStyles(line, offset);

					// start marker was in a comment or string skip it
					if (styleContains(ignoreList, pos)) {
						pos += 2;
						continue;
					}

					// offsets[0] holds the COMMENT_START offset
					// and COMMENT_END holds the ending offset
					int[] offsets = new int[2];
					offsets[0] = pos;

					// Find the corresponding end comment.
					while ((pos = text.indexOf(COMMENT_END, pos)) != -1) {
						lineNum = styledText.getLineAtOffset(pos);
						offset = styledText.getOffsetAtLine(lineNum);
						line = styledText.getLine(lineNum);

						ignoreList = getIgnoredStyles(line, offset);

						// check to make sure the marker isn't in a comment or string
						if (!styleContains(ignoreList, pos)) {
							break;
						}
						pos += 2;
					}

					// If no corresponding end comment, use the end of the text
					offsets[1] = pos == -1 ? text.length() - 1 : pos
							+ COMMENT_END.length() - 1;
					pos = offsets[1];

					// Add the offsets to the collection
					comments.add(offsets);
				}

				if (!commentsEqual(comments, commentOffsets)) {
					commentOffsets = comments;
					styledText.redraw();
				}
	}

	@Override
	public void modifyText(ModifyEvent e) { // update multiline comments
		String text = styledText.getText();
		updateMultiLineComments(text);
	}

	private ArrayList<StyleRange> getMultilineComments(String lineText,
			int lineOffset) {
		// Create a collection to hold the StyleRanges
		ArrayList<StyleRange> styles = new ArrayList<StyleRange>();

		// Store the length for convenience
		int length = lineText.length();

		for (int i = 0, n = commentOffsets.size(); i < n; i++) {
			int[] offsets = (int[]) commentOffsets.get(i);

			// If starting offset is past current line--quit
			if (offsets[0] > lineOffset + length)
				break;

			// Check if we're inside a multiline comment
			if (offsets[0] <= lineOffset + length && offsets[1] >= lineOffset) {

				// Calculate starting offset for StyleRange
				int start = Math.max(offsets[0], lineOffset);

				// Calculate length for style range
				int len = Math.min(offsets[1], lineOffset + length) - start + 1;

				// Add the style range
				styles.add(new StyleRange(start, len,
						syntaxFormat.commentFormat.color, null));
			}
		}

		// Copy all the ranges into the event
		return styles;
	}

}
