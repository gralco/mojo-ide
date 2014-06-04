Copyright (C) 2013-2014 Embedded Micro

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Version 0.0.3 Pre-Alpha

This is the third release of the Mojo IDE! It is intended as a preview and will likely contain many bugs. Since this is such an early build it also lacks many of the features that will be introduced in later versions. Some planned features include, real-time error checking, search/replace, templates (auto-fill), auto-complete, warnings for common mistakes, and extra awesome.

The IDE features right now include, Verilog syntax highlighting, Verilog syntax error checking, Verilog auto-indent full code fix-indent (ctrl+shift+f), basic project creation/manipulation, integration with PlanAhead for building your project, integration with the Mojo Loader to load the generated bin file. 

Change log: 
    Added syntax error underline and tooltips.

    Revered new line tabs to just copy the number of tabs from the previous line (temporary fix for undo bug).

Major known bugs: 
    undo/redo is known to crash when undoing/redoing very quickly.

    Entering a new line does not indent properly.

    No way to disable verification/load directly to the FPGA

    No way to rearange tabs




