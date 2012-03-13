package j1;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;


public class Debugger {
	
	private MemoryImpl memory;
	private Disassembler disassembler;
	private Simulator machine;

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

		disassembler = new Disassembler(memory);
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
				disassembler.addLabel(m.group(1), (short)fromHex(m.group(2)));
			}
		}
		br.close();
		
		machine = new Simulator(memory);
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
		int n=0;
		do {
			short i = memory.read(machine.getPc());
			if (isAtBreakpoint()) {
				break;
			}
			if (J1.isCall(i)) { n++; }
			if (J1.isReturn(i)) { n--; }
			machine.step();
		} while (n>0);
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
	JLabel pc,d0;
	JLabel[] ds,rs;
	JTextArea assembly,console;
	
	void createWindow()
	{
		frame = new JFrame("Debugger");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		JPanel controls = new JPanel();
		controls.setLayout(new BorderLayout());
		
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
		constraints.gridx = 0; constraints.gridy = 2;
		panel.add(new JLabel("D0"),constraints);
		d0 = new JLabel("0000");
		constraints = new GridBagConstraints();
		constraints.gridx = 1; constraints.gridy = 2;
		panel.add(d0,constraints);

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
		controls.add(panel,BorderLayout.WEST);
		
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
		controls.add(buttons, BorderLayout.NORTH);
		
		assembly = new JTextArea(10, 25);
		assembly.setEditable(false);
		controls.add(assembly, BorderLayout.CENTER);
		
		frame.add(controls, BorderLayout.NORTH);
		
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
	
	void updateState(Simulator m)
	{
		pc.setText(new Formatter().format("%04x", m.pc).toString());
		d0.setText(new Formatter().format("%04x", m.d0).toString());
		showStack(m.ds_a, m.ds, ds);
		showStack(m.rs_a, m.rs, rs);
	}

	private void showStack(int d, short[] stack, JLabel[] labels)
	{
		for (int i=0; i<5; i++) {
			if (i<=d) {
				labels[i].setText(new Formatter().format("%04x", stack[d-i]).toString());
			} else {
				labels[i].setText(".");
			}
		}
		if (d>=5) {
			labels[4].setText("...");
		}
	}
	
	private void updateAssembly(short address)
	{
		disassembler.setPc(machine.getPc());
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
