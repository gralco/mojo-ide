package com.embeddedmicro.mojo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

import com.embeddedmicro.mojo.parser.Verilog2001Lexer;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Always_constructContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Case_statementContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Conditional_statementContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Continuous_assignContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Input_declarationContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Local_parameter_declarationContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Module_declarationContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Module_keywordContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Net_declarationContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.NumberContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Output_declarationContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Reg_declarationContext;
import com.embeddedmicro.mojo.parser.Verilog2001Parser.Seq_blockContext;

public class StyleTokenReader {
	private static final String KEYWORDS = "always|begin|end|assign|if|for|else|case|endcase|"
			+ "casex|posedge|negedge|generate|endgenerate";
	private static final String VARIABLES = "input|output|reg|wire|localparam|parameter|integer|genvar";
	private static final String MODULE = "module|endmodule";

	private ArrayList<StyleRange> styles = new ArrayList<StyleRange>();

	public StyleRange[] getStyles(CommonTokenStream tokens) {
		styles.clear();
		tokens.fill();

		for (Token t : tokens.getTokens()) {
			switch (t.getType()) {
			case Verilog2001Lexer.One_line_comment:
			case Verilog2001Lexer.Block_comment:
				addStyle(t.getStartIndex(), t.getStopIndex(),
						Theme.commentColor);
				break;
			case Verilog2001Lexer.Real_number:
			case Verilog2001Lexer.Hex_number:
			case Verilog2001Lexer.Binary_number:
			case Verilog2001Lexer.Octal_number:
			case Verilog2001Lexer.Decimal_number:
				addStyle(t.getStartIndex(), t.getStopIndex(), Theme.valueColor);
				break;
			case Verilog2001Lexer.String:
				addStyle(t.getStartIndex(), t.getStopIndex(), Theme.stringColor);
				break;
			default:
				if (t.getText().matches("[*!~+#\\-/:@|&{}?^=><]+")) {
					addStyle(t.getStartIndex(), t.getStopIndex(),
							Theme.operatorColor, SWT.BOLD);
				} else if (t.getText().matches(KEYWORDS)) {
					addStyle(t.getStartIndex(), t.getStopIndex(),
							Theme.keyWordColor, SWT.BOLD);
				} else if (t.getText().matches(VARIABLES)) {
					addStyle(t.getStartIndex(), t.getStopIndex(),
							Theme.varTypeColor);
				} else if (t.getText().matches(MODULE)) {
					addStyle(t.getStartIndex(), t.getStopIndex(),
							Theme.moduleColor, SWT.BOLD);
				}

			}
		}

		Collections.sort(styles, new Comparator<StyleRange>() {
			@Override
			public int compare(StyleRange o1, StyleRange o2) {
				return o1.start - o2.start;
			}
		});
		return styles.toArray(new StyleRange[styles.size()]);
	}

	private void addStyle(int start, int stop, Color foreground) {
		addStyle(start, stop, foreground, SWT.NONE);
	}

	private void addStyle(int start, int stop, Color foreground, int style) {
		int length = stop - start + 1;
		StyleRange styleRange = new StyleRange();
		styleRange.start = start;
		styleRange.length = length;
		styleRange.foreground = foreground;
		styleRange.fontStyle = style;
		styles.add(styleRange);
	}
}
