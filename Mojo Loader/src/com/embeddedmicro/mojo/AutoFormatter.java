package com.embeddedmicro.mojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public class AutoFormatter implements VerifyListener, ModifyListener {
	private StyledCodeEditor editor;
	private LineStyler styler;

	private ArrayList<IndentPair> indentPairs;

	private static class IndentTag {
		public final String open;
		public final String close;
		public final boolean isolate;

		public IndentTag(String open, String close, boolean isolate) {
			this.open = open;
			this.close = close;
			this.isolate = isolate;
		}
	}

	private static class IndentPair {
		public IndentTag tag;
		public int startOffset;
		public int endOffset;

		public IndentPair(IndentTag tag, int startOffset, int endOffset) {
			this.tag = tag;
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
	}

	private static class IndentMatch {
		public String string;
		public boolean open;
		public int offset;

		public IndentMatch(String match, boolean open, int offset) {
			this.string = match;
			this.open = open;
			this.offset = offset;
		}
	}

	private static final IndentTag[] indentTags = {
			new IndentTag("begin", "end", true),
			new IndentTag("case", "endcase", true),
			new IndentTag("casex", "endcase", true),
			new IndentTag("module", "endmodule", true),
			new IndentTag("generate", "endgenerate", true),
			new IndentTag("(", ")", false) };

	private static final String singleLineTags[] = { "if", "else", "always" };
	private static final IndentTag singleLineBracketTag = indentTags[0];

	public AutoFormatter(StyledCodeEditor editor, LineStyler styler) {
		this.editor = editor;
		this.styler = styler;
		indentPairs = new ArrayList<IndentPair>();
	}

	private boolean isSingleLineIndent(String line) {
		for (String regex : singleLineTags) {
			Pattern pattern = Pattern.compile("\\b" + regex + "\\b");
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				int start = matcher.start();
				String tag;
				if (singleLineBracketTag.isolate)
					tag = "\\b" + singleLineBracketTag.open + "\\b";
				else
					tag = singleLineBracketTag.open;

				pattern = Pattern.compile(tag);
				matcher = pattern.matcher(line);
				if (matcher.find()) {
					int tagStart = matcher.start();
					if (start < tagStart)
						return false;
				}
				return true;
			}
		}
		return false;
	}

	private IndentTag openStringToTag(String string) {
		for (IndentTag tag : indentTags) {
			if (tag.open.equals(string))
				return tag;
		}
		return null;
	}

	private void updateIndentList() {
		indentPairs.clear();
		String[] lines = editor.getText().split("(?:\r)?\n");
		for (int lineNum = 0; lineNum < lines.length; lineNum++) {
			String line = lines[lineNum];
			ArrayList<IndentMatch> keyPairs = getIndentMatches(line, lineNum,
					true, true);

			for (IndentMatch keyPair : keyPairs) {
				if (keyPair.open) {
					indentPairs
							.add(new IndentPair(
									openStringToTag(keyPair.string),
									keyPair.offset, -1));
				} else {
					for (int i = indentPairs.size() - 1; i >= 0; i--) {
						IndentPair pair = indentPairs.get(i);
						if (pair.tag != null
								&& pair.tag.close.equals(keyPair.string)
								&& pair.endOffset == -1) {
							pair.endOffset = keyPair.offset;
							break;
						}
					}
				}
			}

			if (isSingleLineIndent(line)) {
				int endLine = Math.min(editor.getLineCount() - 1, lineNum + 2);
				IndentPair pair = new IndentPair(null,
						editor.getOffsetAtLine(lineNum),
						editor.getOffsetAtLine(endLine));
				indentPairs.add(pair);
			}
		}
	}

	private boolean indentPairConatins(IndentPair pair, int line) {
		if (editor.getLineAtOffset(pair.startOffset) < line
				&& (pair.endOffset < 0 || editor
						.getLineAtOffset(pair.endOffset) > line))
			return true;

		return false;
	}

	private int getIndentCount(int line) {
		int indent = 0;
		for (IndentPair pair : indentPairs) {
			if (indentPairConatins(pair, line))
				indent++;
		}
		return indent;
	}

	public void fixIndent() {
		StringBuilder builder = new StringBuilder();
		updateIndentList();
		String[] lines = editor.getText().split("(?:\r)?\n");
		for (int lineNum = 0; lineNum < lines.length; lineNum++) {
			int indent = getIndentCount(lineNum);
			builder.append(appendTabs(lines[lineNum], indent) + System.lineSeparator());
		}
		int seperatorLen = System.lineSeparator().length();
		builder.delete(builder.length() - seperatorLen, builder.length());
		editor.replaceTextRange(0, editor.getCharCount(), builder.toString());
	}

	private String appendTabs(String line, int tabs) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < tabs; i++) {
			text.append('\t');
		}
		text.append(line.trim());
		return text.toString();
	}

	private boolean conatinsMatch(ArrayList<IndentMatch> tags, int offset) {
		for (IndentMatch tag : tags)
			if (tag.offset == offset)
				return true;
		return false;
	}

	private void matchString(ArrayList<IndentMatch> tags, IndentTag indent,
			String line, int lineOffset, ArrayList<StyleRange> ignoreList,
			boolean open) {
		String string;
		if (open)
			string = indent.open;
		else
			string = indent.close;
		if (indent.isolate) {
			Pattern pattern = Pattern.compile("\\b" + string + "\\b");
			Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				int start = matcher.start() + lineOffset;
				if (!styler.styleContains(ignoreList, start)) {
					if (!conatinsMatch(tags, start))
						tags.add(new IndentMatch(string, open, start));
				}
			}
		} else {
			int offset = 0;
			while ((offset = line.indexOf(string, offset)) >= 0) {
				int start = offset + lineOffset;
				if (!styler.styleContains(ignoreList, start)) {
					if (!conatinsMatch(tags, start))
						tags.add(new IndentMatch(string, open, start));
				}
				offset += string.length();
			}
		}
	}

	private ArrayList<IndentMatch> getIndentMatches(String line, int lineNum,
			boolean open, boolean close) {
		ArrayList<IndentMatch> tags = new ArrayList<IndentMatch>();

		int lineOffset = editor.getOffsetAtLine(lineNum);

		ArrayList<StyleRange> ignoreList = styler.getIgnoredStyles(line,
				lineOffset);

		for (IndentTag indent : indentTags) {
			if (open)
				matchString(tags, indent, line, lineOffset, ignoreList, true);

			if (close)
				matchString(tags, indent, line, lineOffset, ignoreList, false);

		}

		if (open && close) {
			Collections.sort(tags, new Comparator<IndentMatch>() {
				@Override
				public int compare(IndentMatch arg0, IndentMatch arg1) {
					return arg0.offset - arg1.offset;
				}
			});
		}

		return tags;
	}

	private int countIndents(String line) {
		int idx = 0;
		while (line.indexOf('\t', idx) == idx)
			idx++;
		return idx;
	}

	private boolean matchTag(String closeTag) {
		for (int i = indentPairs.size() - 1; i >= 0; i--) {
			IndentPair pair = indentPairs.get(i);
			if (pair.endOffset < 0 && pair.tag.close.equals(closeTag)) {
				return true;
			}
		}
		return false;
	}

	private ArrayList<IndentMatch> removePairs(ArrayList<IndentMatch> list) {
		for (int i = 0; i < list.size(); i++) {
			IndentMatch match = list.get(i);
			if (!match.open) {
				for (int j = i - 1; j >= 0; j--) {
					IndentMatch openMatch = list.get(j);
					if (openMatch.open) {
						IndentTag tag = openStringToTag(openMatch.string);
						if (tag.close.equals(match.string)) {
							list.remove(i);
							list.remove(j);
							i -= 2;
							break;
						}
					}
				}
			}
		}
		return list;
	}

	private int countTags(ArrayList<IndentMatch> list, boolean open) {
		int ct = 0;
		for (IndentMatch match : list)
			if (match.open == open)
				ct++;
		return ct;
	}

	private void unindent(VerifyEvent e) {
		int lineNum = editor.getLineAtOffset(e.start);
		int lineOffset = editor.getOffsetAtLine(lineNum);
		StringBuilder line = new StringBuilder(editor.getLine(lineNum));
		line.replace(e.start - lineOffset, e.end - lineOffset, e.text);
		String modLine = line.toString();
		String trimmedLine = modLine.trim();

		for (IndentTag indent : indentTags) {
			String lastChar = indent.close.substring(indent.close.length() - 1);
			if (e.text.equals(lastChar) && trimmedLine.equals(indent.close)
					&& matchTag(trimmedLine)) {

				int tabs = -getIndentMatches(line.toString(), lineNum, false,
						true).size();

				if (lineNum > 0) {
					String prevLine = editor.getLine(lineNum - 1);
					tabs += countIndents(prevLine)
							+ countTags(
									removePairs(getIndentMatches(prevLine,
											lineNum - 1, true, true)), true);
				}

				e.doit = false;
				editor.replaceTextRange(lineOffset, editor.getLine(lineNum)
						.length(), appendTabs(trimmedLine, tabs));
				break;
			}
		}
	}

	@Override
	public void verifyText(VerifyEvent e) {
		if (e.text.equals("\n") || e.text.equals("\r\n")) {
			StringBuilder text = new StringBuilder(e.text);
			int lineNum = editor.getLineAtOffset(e.start);
			String line = editor.getLine(lineNum);
			int tabs = countIndents(line)
					+ countTags(
							removePairs(getIndentMatches(line, lineNum, true,
									true)), true)
					+ (isSingleLineIndent(line) ? 1 : 0);
			if (lineNum > 0) {
				tabs -= isSingleLineIndent(editor.getLine(lineNum - 1)) ? 1 : 0;
			}
			for (int i = 0; i < tabs; i++) {
				text.append('\t');
			}
			e.text = text.toString();
		} else {
			unindent(e);
		}
	}

	@Override
	// update tabCount
	public void modifyText(ModifyEvent e) {
		updateIndentList();
	}
}