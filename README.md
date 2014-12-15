# Papilio Forth

Papilio Forth is a rudimentary interactive Forth to run on [Papilio One][pap1] and [Papilio Pro][pappro] FPGA boards.

Papilio Forth is a feasibility study that shows priniples but does not implement a complete
Forth development system.

Papilio Forth executes Forth program on a variation of [James Bowman's J1 FPGA soft core][j1] (see also [J1 on Github][J1github]) 
running at 66 MHz. It communicates with a host system using a serial line at a default speed of 9600 Bits/s.


## Prerequisites

   - [GNU make][gmake] for job control
   - [Java JDK7][jdk7] or [JDK8][jdk8] for compiling the cross compiler and other tools
   - [Xilinx ISE][xilinxise] to generate the FPGA bit stream (Papilio One: ISE 13.4, Papilio Pro: ISE 14.7)
   - [Papilio-Loader][paploader] to download the bitstream to the FPGA

## Directry Structure

    Papilio-Forth
    ├── firmware
    │   ├── bin		cross compiler executables (generated)
    │   ├── forth		rudimentry Forth kernel
    │   └── src		Forth cross compiler written in Java, assembler, debugger, simulator
    └── vhdl  
        ├── src		Verilog projects for J1 and UART for Papilio One and Papilio Pro 
        └── test		testbenches


## How to build Papilio Forth

   1. Build the cross compiler and appendant tools:
   
        make -C firmware tools

   2. Build the Forth image for the J1 processor:
   
        make -C firmware images
 	 
   3. Create the J1 bit stream:
   
    - **Papilio One**

      Start Xilinx ise on project `vhdl/papiolo-one-forth.xise`
      choose `Generate Programming File` on the main component. This generates `main.bit` w/o memory initialization.
	  
        make -C vhdl final.bit
	
	  generates `final.bit` from `main.bit` including the Forth image (`memory.mem`) built in step 2 as initial memory.
	 
    - **Papilio Pro**

      Start Xilinx ise on project `vhdl/papiolo-one-forth.xise`
      choose `Generate Programming File` on the papilio-pro-forth component. This generates papilio_pro_forth.bit
	  including the Forth image (`memory.hex`) built in step 2 as initial memory.
	  
   4. Load the complete bit stream (J1 and memory) into the FPGA:
   
         sudo papilio-prog -v -f final.bit

      or 
	    
         sudo papilio-prog -v -f papilio_pro_forth.bit
		 
      depending on which image you want to load. You might want to use the pre-built images for a quick start.
   
   5. Connect to Papilio Forth:
   
        screen /dev/tty.usbserial 9600

      or similar. Papilio Forth should show the prompt
	  
		Welcome to Papilio Forth

		?>
	    
      If you only see the **`?>`** prompt issue a **`0 >r`** and press the enter key to reboot the system.


##  May the Forth be with you.
 
 [pap1]: http://papilio.cc/
 [pappro]: http://papilio.cc/index.php?n=Papilio.PapilioPro
 [j1]: http://www.excamera.com/sphinx/fpga-j1.html
 [j1github]: https://github.com/jamesbowman/j1

 [paploader]: http://papilio.cc/index.php?n=Papilio.PapilioLoaderV2
 [gmake]: https://www.gnu.org/software/make/
 [jdk7]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
 [jdk8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
 [xilinxise]: http://www.xilinx.com/products/design-tools/ise-design-suite/ise-webpack.html
