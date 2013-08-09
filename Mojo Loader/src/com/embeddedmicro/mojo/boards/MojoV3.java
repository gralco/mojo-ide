package com.embeddedmicro.mojo.boards;

public class MojoV3 extends Board {

	@Override
	public String getFPGAName() {
		return "xc6slx9-2tqg144";
	}

	@Override
	public String getName() {
		return "Mojo V3";
	}

	@Override
	public String getBaseProjectName() {
		return "mojo-v3";
	}
	
}
