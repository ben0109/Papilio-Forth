package compiler;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.print.DocFlavor.INPUT_STREAM;

import simulator.Machine;
import simulator.Memory;

public class Compiler {

	private static final short MARKER_PC		= (short)0xcdef;
	
	private static final short RESET			= 0x0000;
	private static final short VAR_HERE			= RESET+0x10;
	private static final short VAR_DICT_HEAD	= VAR_HERE+2;
	private static final short VAR_LAST_DEF		= VAR_DICT_HEAD+2;
	private static final short VAR_STATE		= VAR_LAST_DEF+2;
	private static final short VAR_BASE			= VAR_STATE+2;
	private static final short VAR_INPUT_BUFFER	= VAR_BASE+2;
	private static final short VAR_INPUT_SIZE	= VAR_INPUT_BUFFER+80;
	private static final short VAR_TO_IN		= VAR_INPUT_SIZE+2;
	private static final short VAR_WORD_BUFFER	= VAR_TO_IN+2;
	private static final short INITIAL_HERE		= VAR_WORD_BUFFER+32;

	public interface NativeWord {
		void execute(Compiler state) throws Exception;
	}
	
	private BufferedReader input;
	private short[] memory;
	private Stack<Short> ds,rs;
	
	Assembler assembler;
	Disassembler disassembler;
	Map<String,NativeWord> nativeWords;
	
	public static void main(String[] args) throws Exception
	{
		String[] files = new String[] {
				"1-stack",
				"2-integer",
				"3-comparison",
				"4-memory",
				"5-control",
				"6-output",
				"7-dictionary",
				"8-input",
				"9-compiler" };
//		Compiler compiler = new Compiler(0x4000);
		Compiler compiler = new Compiler(0x800);
		for (String file : files) {
			LineNumberReader r = new LineNumberReader(new FileReader("forth/core/"+file+".fth"));
			try {
				compiler.parse(r);
			} catch (Exception e) {
				System.err.println("error in file "+file+", line "+r.getLineNumber()+": "+e.getClass().getSimpleName());
				e.printStackTrace();
				break;
			}
		}
		
		short main = compiler.find("__main");
		compiler.store(RESET, main);

		compiler.writeBinFile("memory.bin");
		compiler.writeMemFile("memory.mem");
		compiler.writeMapFile("memory.map");
	}

	private void writeBinFile(String name)
			throws FileNotFoundException, IOException
	{
		OutputStream os = new FileOutputStream(name);
		for (int i=0; i<memory.length; i++) {
			os.write((memory[i]&0xff00)>>8);
			os.write((memory[i]&0x00ff)>>0);
		}
		os.close();
	}

	private void writeMemFile(String name)
			throws FileNotFoundException, IOException
	{
		PrintWriter os = new PrintWriter(name);
		for (int i=0; i<memory.length; i+=8) {
			os.printf("@%08X",i<<1);
			for (int j=0; j<8; j++) {
				int w = memory[i+j];
				os.printf(" %02X %02X", w&0xff, (w&0xff00)>>8);
			}
			os.println();
		}
		os.close();
	}

	private void writeMapFile(String name)
			throws FileNotFoundException, IOException
	{
		PrintWriter os = new PrintWriter(name);
		for (Map.Entry<Short, String> e : assembler.getLabels().entrySet()) {
			os.printf("%%%s:%04x\n", e.getValue(), e.getKey());
		}
		os.close();
	}
	
	public Compiler(int size)
	{
		memory = new short[size];
		store(VAR_HERE,(short)INITIAL_HERE);
		store(VAR_DICT_HEAD,(short)0);
		store(VAR_LAST_DEF,(short)0);
		store(VAR_STATE,(short)0);
		ds = new Stack<Short>();
		rs = new Stack<Short>();
		
		this.assembler = new Assembler(this);
		this.disassembler = new Disassembler(new Memory() {
			public void write(short address, short value) {
				store(address,value);
			}
			public short read(short address) {
				return fetch(address);
			}
		}, assembler.getLabels());

		nativeWords = new HashMap<String, NativeWord>();
		nativeWords.put("(", new NativeWord() {
			public void execute(Compiler state) throws Exception { paren(); }});
		nativeWords.put(":", new NativeWord() {
			public void execute(Compiler state) throws Exception { colon(); }});
		nativeWords.put(";", new NativeWord() {
			public void execute(Compiler state) throws Exception { semicolon(); }});
		nativeWords.put("immediate", new NativeWord() {
			public void execute(Compiler state) throws Exception { immediate(); }});
		nativeWords.put("{", new NativeWord() {
			public void execute(Compiler state) throws Exception { brace(); }});
		nativeWords.put(",", new NativeWord() {
			public void execute(Compiler state) throws Exception { short i=ds.pop(); comma(i); }});
		nativeWords.put("c,", new NativeWord() {
			public void execute(Compiler state) throws Exception { short i=ds.pop(); cComma(i); }});
		nativeWords.put("literal", new NativeWord() {
			public void execute(Compiler state) throws Exception { literal(); }});
		nativeWords.put("postpone", new NativeWord() {
			public void execute(Compiler state) throws Exception { postpone(); }});
		
		nativeWords.put("$here",		constNWord(VAR_HERE));
		nativeWords.put("$dictHead",	constNWord(VAR_DICT_HEAD));
		nativeWords.put("$lastDef",		constNWord(VAR_LAST_DEF));
		nativeWords.put("$state",		constNWord(VAR_STATE));
		nativeWords.put("$base",		constNWord(VAR_BASE));
		nativeWords.put("$inputBuffer",	constNWord(VAR_INPUT_BUFFER));
		nativeWords.put("$inputSize",	constNWord(VAR_INPUT_SIZE));
		nativeWords.put("$toIn",		constNWord(VAR_TO_IN));
		nativeWords.put("$wordBuffer",	constNWord(VAR_WORD_BUFFER));

		nativeWords.put("$ld_ds_next",	constNWord(Instruction.getCodeForAlu(Instruction.OP_LDF, Instruction.ARG_DS, Instruction.ARG_NXT)));
		nativeWords.put("$ldf_a_ds",	constNWord(Instruction.getCodeForAlu(Instruction.OP_LDF, Instruction.ARG_A, Instruction.ARG_DS)));
		nativeWords.put("$ld_pc_ds",	constNWord(Instruction.getCodeForAlu(Instruction.OP_LD, Instruction.ARG_PC, Instruction.ARG_DS)));
		nativeWords.put("$ld_pc_rs",	constNWord(Instruction.getCodeForAlu(Instruction.OP_LD, Instruction.ARG_PC, Instruction.ARG_RS)));
		nativeWords.put("$jmp_eq",		constNWord(Instruction.getCodeForJumpAbs(Instruction.CC_EQ)));
		nativeWords.put("$jmp_ne",		constNWord(Instruction.getCodeForJumpAbs(Instruction.CC_NE)));
		nativeWords.put("$jmp_al",		constNWord(Instruction.getCodeForJumpAbs(Instruction.CC_AL)));
	}
	
	NativeWord constNWord(final short v)
	{
		return new NativeWord() {
			public void execute(Compiler state) throws Exception {
				comma(Instruction.LD_DS_NXT);
				comma(v);
			}
		};
	}
	
	short getHere()					{ return fetch(VAR_HERE); }
	short getDictHead()				{ return fetch(VAR_DICT_HEAD); }
	short getLastDef()				{ return fetch(VAR_LAST_DEF); }
	short getState()				{ return fetch(VAR_STATE); }

	void setHere(short value)		{ store(VAR_HERE,value); }
	void setDictHead(short value)	{ store(VAR_DICT_HEAD,value); }
	void setLastDef(short value)	{ store(VAR_LAST_DEF,value); }
	void setState(short value)		{ store(VAR_STATE,value); }
	
	public void parse(BufferedReader r) throws Exception
	{
		input = r;
		while (true) {
			if (atEndOfLine()) {
				if (!readLine()) {
					break;
				}
			}
			
			String w = word();
			if (w.length()==0) {
				continue;
			}
			System.out.print(w+" ");
			
			short def = findDef(w);
			if (def!=0) {
				short xt = getXT(def);
				if ((getState()!=0) && (getFlags(def)&IMMEDIATE)==0) {
					comma(xt);
				} else {
					execute(xt);
				}
				continue;
			}
			
			NativeWord nw = nativeWords.get(w);
			if (nw!=null) {
				nw.execute(this);
				continue;
			}
			
			try {
				int n = (w.startsWith("0x")) ? Integer.parseInt(w.substring(2), 16) : Integer.parseInt(w);
				if (getState()!=0) {
					if (Instruction.isImmediateValueOK(n)) {
						comma(Instruction.getCodeForAluImm(Instruction.OP_LD, Instruction.ARG_DS, n));
					} else {
						comma(Instruction.LD_DS_NXT);
						comma((short)n);
					}
				} else {
					ds.push((short)n);
				}
				continue;
			} catch (NumberFormatException e) {
				// nothing
			}
			throw new Exception("unknown word "+w);
		}
		assembler.patchForwardJumps();
	}
	
	private void execute(short xt)
	{
		Machine m = new Machine(new Memory() {
			public void write(short address, short value) {
				store(address,value);
			}
			public short read(short address) {
				return fetch(address);
			}
		});
		m.rs = new Stack<Short>();
		m.rs.push(MARKER_PC); // dummy address
		m.ds = ds;
		m.pc = xt;
		while (m.pc!=MARKER_PC) {
//			disassembler.setPc(m.pc);
//			System.out.printf("%04x: ",m.pc);
//			for (int i=0; i<m.rs.size()-1; i++) {
//				System.out.print("  ");
//			}
//			System.out.println(disassembler.disassemble());
			m.step();
		}		
	}

	String parseUntil(int j) throws Exception
	{
		StringBuilder s = new StringBuilder();
		while (!atEndOfLine()) {
			int i = nextChar();
			if (i==j) {
				return s.toString();
			}
			s.append(Character.toChars(i));
		}
		throw new Error("end of line reached while looking for '"+String.valueOf(Character.toChars(j))+"'");
	}
	
	void paren() throws Exception
	{
		while (true) {
			while (!atEndOfLine()) {
				int i = nextChar();
				if (i==')') {
					return;
				}
			}
			if (!readLine()) {
				throw new Error("'(' was not closed");
			}
		}
	}
	
	void brace() throws Exception
	{
		String s = parseUntil('}');
		assembler.translate(s);
	}
	
	void postpone() throws Exception
	{
		String s = word();
		short xt = find(s);
		if (xt==0) {
			throw new Exception("unknown word "+s);
		}
		comma(xt);
	}
	
	short fetch(int address)
	{
		return memory[address>>1];
	}
	
	void store(int address, short value)
	{
//		if (address>0x10)
//			System.out.printf("%04x<-%04x\n", address, value&0xffff);
		memory[address>>1] = value;
	}
	
	int cFetch(int address)
	{
		if ((address&1)!=0) {
			return (memory[address>>1]&0xff00)>>8;
		} else {
			return (memory[address>>1]&0x00ff)>>0;
		}
	}
	
	void cStore(int address, int value)
	{
//		System.out.printf("%04x<-%02x\n", address, value&0xff);
		if ((address&1)!=0) {
			memory[address>>1] &= 0xff;
			memory[address>>1] |= (value&0xff)<<8;
		} else {
			memory[address>>1] &= 0xff00;
			memory[address>>1] |= value&0xff;
		}
	}
	
	void align() {
		setHere((short)((getHere()+1) & ~1));
	}
	
	void comma(short value) {
		store(getHere(), value);
		setHere((short)(getHere()+2));
	}
	
	void cComma(short value) {
		cStore(getHere(), value);
		setHere((short)(getHere()+1));
	}
	
	void literal() {
		comma(Instruction.LD_DS_NXT);
		comma(ds.pop());
	}
	
	boolean readLine() throws IOException
	{
		String l = input.readLine();
		if (l==null) {
			return false;
		}
		store(VAR_INPUT_SIZE, (short)l.length());
		store(VAR_TO_IN, (short)0);
		for (int i=0; i<l.length(); i++) {
			cStore(VAR_INPUT_BUFFER+i, l.codePointAt(i));
		}
		return true;
	}
	
	boolean atEndOfLine()
	{
		return fetch(VAR_TO_IN)==fetch(VAR_INPUT_SIZE);
	}
	
	int nextChar()
	{
		short toIn = fetch(VAR_TO_IN);
		int r = cFetch(VAR_INPUT_BUFFER+toIn);
		store(VAR_TO_IN, (short)(toIn+1));
		return r;
	}
	
	String word() throws Exception
	{
		int i;
		do {
			if (atEndOfLine()) {
				return "";
			}
			i = nextChar();
		} while (Character.isWhitespace(i));
		
		StringBuilder sb = new StringBuilder();
		do {
			sb.append(Character.toChars(i));
			if (atEndOfLine()) {
				break;
			}
			i = nextChar();
		} while (!Character.isWhitespace(i));

		return sb.toString();
	}
	
	public static final int IMMEDIATE = 0x80;
	
	void colon() throws Exception
	{
		String name = word();
		doColon(name);
	}

	private void doColon(String name)
	{
		setState((short)1);
		setLastDef(getHere());
		comma(getDictHead());
		cComma((short)name.length());
		for (int i=0; i<name.length(); i++) {
			cComma((short)name.codePointAt(i));
		}
		align();
		if (name.equals("__main"))
			name = name;
		System.out.printf("%04x xt of %s\n",getHere(),name);
		assembler.getLabels().put(getHere(),name);
	}
	
	void semicolon() throws Exception
	{
		if (!rs.isEmpty() || !ds.isEmpty()) {
			System.err.println("warning: stacks not empty at end of definition");
		}
		comma(Instruction.RET);
		setState((short)0);
		setDictHead(getLastDef());
		
		disassembler.setPc(getXT(getLastDef()));
		while (disassembler.getPc()<getHere()) {
			if (assembler.getLabels().containsKey(disassembler.getPc())) {
				System.out.printf("     %s:\n", assembler.getLabels().get(disassembler.getPc()));
			}
			System.out.printf("%04x \t%s\n",disassembler.getPc(),disassembler.disassemble());
		}
	}
	
	short getPreviousDef(short def)
	{
		return fetch(def);
	}
	
	int getFlags(short def)
	{
		return cFetch(def+2)&0xe0;
	}
	
	void setFlags(short def, int flags)
	{
		cStore(def+2,(cFetch(def+2)&0x1f)|flags);
	}
	
	int getNameLength(short def)
	{
		return cFetch(def+2)&0x1f;
	}
	
	int getNamePtr(short def)
	{
		return def+3;
	}
	
	short getXT(short def)
	{
		int raw = getNamePtr(def)+getNameLength(def);
		int aligned = (raw+1) & ~1;
		return (short)aligned;
	}
	
	void immediate() throws Exception
	{
		setFlags(getLastDef(), IMMEDIATE);
	}
	
	short findDef(String name)
	{
		short ptr = getDictHead();
		while (ptr!=0) {
			if (getNameLength(ptr)==name.length()) {
				boolean same = true;
				for (int i=0; i<name.length(); i++) {
					same &= (cFetch(getNamePtr(ptr)+i)==name.codePointAt(i));
				}
				if (same) {
					return ptr;
				}
			}
			ptr = getPreviousDef(ptr);
		}
		return 0;
	}
	
	short find(String name)
	{
		short def = findDef(name);
		if (def==0) {
			return 0;
		} else {
			return getXT(def);
		}
	}
}
