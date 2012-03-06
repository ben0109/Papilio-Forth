import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import compiler.Disassembler;


import simulator.Machine;
import simulator.MemoryImpl;


public class Debugger {
	
	private MemoryImpl memory;
	private Disassembler disassembler;
	private Machine machine;

	public static void main(String[] args) throws IOException
	{
		new Debugger();
	}
	
	private Debugger() throws IOException
	{
//		String binName = "/home/ben/prog/PapilioForth/firmware/memory.bin";
//		String mapName = "/home/ben/prog/PapilioForth/firmware/memory.map";
		String binName = "memory.bin";
		String mapName = "memory.map";
		
		File bin = new File(binName);
		int len = ((int)bin.length())/2;
		short[] code = new short[len];
		
		FileInputStream is = new FileInputStream(bin);
		for (int i=0; i<len; i++) {
			int l,h;
			h = is.read();
			l = is.read();
			code[i] = (short)((h<<8) | l);
		}
		is.close();
		
		memory = new MemoryImpl(code, new MemoryImpl.SerialOut() {
			public void writeChar(int code) {
				onWriteChar(code);
			}
		});
		
		Map<Short, String> labels = new HashMap<Short, String>();
		File map = new File(mapName);
		BufferedReader br = new BufferedReader(new FileReader(map));
		while (true) {
			String l = br.readLine();
			if (l==null) {
				break;
			}
			Matcher m;
			m = Pattern.compile("%([^:]+):([0-9a-f]+)").matcher(l);
			if (m.matches()) {
				labels.put((short)fromHex(m.group(2)), m.group(1));
			}
		}
		br.close();
		
		disassembler = new Disassembler(memory,labels);
		machine = new Machine(memory);
		machine.reset();
		
		createWindow();
		updateState(machine);
		updateAssembly(machine.pc);
		
		while (true) {
			executeRunCommand();
			Thread.yield();
		}
	}
	
	public boolean isAtBreakpoint()
	{
		return false;
	}
	
	public void stepInto()
	{
		machine.step();
	}
	
	public void stepOver()
	{
		short here = machine.pc;
		short i = memory.read(here);
		if (disassembler.isCall(i)) {
			short next = (short)(here + 2*disassembler.getInstrLength(i));
			int n = 1;
			while (n>0) {
				if (isAtBreakpoint()) {
					break;
				}
				machine.step();
				if (machine.pc==here) {
					n++;
				} else if (machine.pc==next) {
					n--;
				}
			}
		} else {
			machine.step();
		}
	}
	
	public void run()
	{
		while (!isAtBreakpoint()) {
			machine.step();
		}
	}

	private static int fromHex(String s)
	{
		int r = 0;
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			r <<= 4;
			if (c>='0' && c<='9') r += c-'0';
			if (c>='a' && c<='f') r += c-'a'+10;
		}
		return r;
	}
	
	JFrame frame;
	JLabel pc,r1,r2,flags;
	JLabel[] ds,rs;
	JTextArea assembly,console;
	
	void createWindow()
	{
		frame = new JFrame("Debugger");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		GridBagConstraints constraints;
		constraints = new GridBagConstraints();
		constraints.gridx = 0; constraints.gridy = 0;
		panel.add(new JLabel("PC"),constraints);
		pc = new JLabel("0000");
		constraints = new GridBagConstraints();
		constraints.gridx = 1; constraints.gridy = 0;
		panel.add(pc,constraints);
		
		constraints = new GridBagConstraints();
		constraints.gridx = 0; constraints.gridy = 1;
		panel.add(new JLabel("SZVC"),constraints);
		flags = new JLabel("0000");
		constraints = new GridBagConstraints();
		constraints.gridx = 1; constraints.gridy = 1;
		panel.add(flags,constraints);

		constraints = new GridBagConstraints();
		constraints.gridx = 0; constraints.gridy = 2;
		panel.add(new JLabel("R1"),constraints);
		r1 = new JLabel("0000");
		constraints = new GridBagConstraints();
		constraints.gridx = 1; constraints.gridy = 2;
		panel.add(r1,constraints);

		constraints = new GridBagConstraints();
		constraints.gridx = 0; constraints.gridy = 3;
		panel.add(new JLabel("R2"),constraints);
		r2 = new JLabel("0000");
		constraints = new GridBagConstraints();
		constraints.gridx = 1; constraints.gridy = 3;
		panel.add(r2,constraints);

		constraints = new GridBagConstraints();
		constraints.gridx = 0; constraints.gridy = 4;
		constraints.insets = new Insets(10,0,0,0);
		panel.add(new JLabel("DS"),constraints);
		ds = new JLabel[5];
		for (int i=0; i<5; i++) {
			ds[i] = new JLabel("0000");
			constraints = new GridBagConstraints();
			constraints.gridx = 1; constraints.gridy = 4+i;
			if (i==0)constraints.insets = new Insets(10,0,0,0);
			panel.add(ds[i],constraints);
		}

		constraints = new GridBagConstraints();
		constraints.gridx = 0; constraints.gridy = 9;
		constraints.insets = new Insets(10,0,0,0);
		panel.add(new JLabel("RS"),constraints);
		rs = new JLabel[5];
		for (int i=0; i<5; i++) {
			rs[i] = new JLabel("0000");
			constraints = new GridBagConstraints();
			constraints.gridx = 1; constraints.gridy = 9+i;
			if (i==0)constraints.insets = new Insets(10,0,0,0);
			panel.add(rs[i],constraints);
		}
		frame.add(panel,BorderLayout.WEST);
		
		JPanel buttons = new JPanel();
		JButton button;
		/*
		button = new JButton("Reset");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				reset();
			}
		});
		buttons.add(button);
		*/
		button = new JButton("Step into");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runCommand = RunCommand.StepInto;
			}
		});
		buttons.add(button);

		button = new JButton("Step over");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runCommand = RunCommand.StepOver;
			}
		});
		buttons.add(button);

		button = new JButton("Run");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runCommand = RunCommand.Run;
			}
		});
		buttons.add(button);
		frame.add(buttons, BorderLayout.NORTH);
		
		assembly = new JTextArea(10, 25);
		assembly.setEditable(false);
		frame.add(assembly, BorderLayout.CENTER);
		
		console = new JTextArea(10, 80);
		console.setEditable(false);
		frame.add(console, BorderLayout.SOUTH);
		
		frame.pack();
		frame.setVisible(true);
		
		// catch key events
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyEventDispatcher()
        {
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				switch (e.getID()) {
				case KeyEvent.KEY_PRESSED:
					onKeyPressed(e);
					break;
				case KeyEvent.KEY_TYPED:
					onKeyTyped(e);
					break;
	            }
	            return false;

			}
		});

		frame.requestFocus();
	}
	
	enum RunCommand { StepInto, StepOver, Run };
	RunCommand runCommand = null;
	
	void executeRunCommand() {
		if (runCommand!=null) {
			switch (runCommand) {
			case StepInto: stepInto(); break;
			case StepOver: stepOver(); break;
			case Run: run(); break;
			}
			runCommand = null;
			updateState(machine);
			updateAssembly(machine.pc);
		}
	}
	
	void updateState(Machine m)
	{
		pc.setText(new Formatter().format("%04x", m.pc).toString());
		r1.setText(new Formatter().format("%04x", m.r1).toString());
		r2.setText(new Formatter().format("%04x", m.r2).toString());
		flags.setText(new Formatter().format("%d%d%d%d",m.sf?1:0,m.zf?1:0,m.vf?1:0,m.cf?1:0).toString());
		showStack(m.ds, ds);
		showStack(m.rs, rs);
	}

	private void showStack(Stack<Short> stack, JLabel[] labels)
	{
		for (int i=0; i<5; i++) {
			if (i<stack.size()) {
				labels[i].setText(new Formatter().format("%04x", stack.get(stack.size()-1-i)).toString());
			} else {
				labels[i].setText(".");
			}
		}
		if (stack.size()>5) {
			labels[4].setText("...");
		}
	}
	
	private void updateAssembly(short address)
	{
		disassembler.setPc(machine.pc);
		StringBuilder l = new StringBuilder();
		for (int i=0; i<10; i++) {
			l.append(disassembler.disassemble());
			l.append("\n");
		}
		assembly.setText(null);
		assembly.setText(l.toString());
	}

	private void onKeyPressed(KeyEvent e)
	{
		switch (e.getKeyCode()) {
		case KeyEvent.VK_BACK_SPACE:
			memory.receiveChar(0x08);
			break;
		}
	}

	private void onKeyTyped(KeyEvent e)
	{
		memory.receiveChar(Character.codePointAt(new char[]{e.getKeyChar()}, 0));
	}

	private void onWriteChar(int code) {
		switch (code) {
		case 0x08:
			try {
			int lineCount = console.getLineCount();
			int len = console.getLineEndOffset(lineCount-1);
			console.replaceRange(null, len-1,len);
			} catch (Exception e) { throw new Error(e); }
			break;
		default:
			console.append(String.valueOf(Character.toChars(code)));
			break;
		}
	}
}
