package j1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;



public class Compiler
{
	private static final short MARKER_PC 		= 0x1543;
	private static final short COLON_SYS 		= 0x1234;

	private static final short RESET			= 0x0000;
	private static final short VAR_HERE			= RESET+0x10;
	private static final short VAR_BASE			= VAR_HERE+2;
	private static final short VAR_DICT_HEAD	= VAR_BASE+2;
	private static final short VAR_LAST_DEF		= VAR_DICT_HEAD+2;
	private static final short VAR_STATE		= VAR_LAST_DEF+2;
	private static final short VAR_INPUT_BUFFER	= VAR_STATE+2;
	private static final short VAR_INPUT_SIZE	= VAR_INPUT_BUFFER+80;
	private static final short VAR_TO_IN		= VAR_INPUT_SIZE+2;
	private static final short VAR_WORD_BUFFER	= VAR_TO_IN+2;
	private static final short INITIAL_HERE		= VAR_WORD_BUFFER+32;

	public interface NativeWord {
		void execute() throws Exception;
	}
		
	private BufferedReader			input;
	private Simulator				j1;
	private short[]					memory;
	private Map<String,DefinedWord>	dict;
	private Disassembler			disassembler;
	
	private boolean					compact;
	
	DefinedWord						currentWord;
	Map<String,NativeWord>			nativeWords;
	Map<String,short[]>				aliasWords;

	enum OutputFormat { Bin, Mem, Init, Hex };
	
	public static void main(String[] args) throws Exception
	{
		int				size	= 0x800;
		String			output	= "memory.bin";
		OutputFormat	format	= OutputFormat.Bin;
		String			map		= null;
		String			start	= "__main";
		boolean			compact	= false;
		
		Compiler compiler = null;
		
		for (int i=0; i<args.length; i++) {
			if ("-format".equals(args[i])) {
				String f = args[++i];
				if ("bin".equals(f)) {
					format = OutputFormat.Bin;
				} else if ("mem".equals(f)) {
					format = OutputFormat.Mem;
				} else if ("init".equals(f)) {
					format = OutputFormat.Init;
				} else if ("hex".equals(f)) {
					format = OutputFormat.Hex;
				} else {
					Log.fatal("wrong format %s",f);
					usage(1);
				}
			} else if ("-log".equals(args[i])) {
				String l = args[++i];
				if ("error".equals(l)) {
					Log.setCurrentLevel(Log.ERROR);
				} else if ("warning".equals(l)) {
					Log.setCurrentLevel(Log.WARNING);
				} else if ("info".equals(l)) {
					Log.setCurrentLevel(Log.INFO);
				} else if ("debug".equals(l)) {
					Log.setCurrentLevel(Log.DEBUG);
				} else if ("trace".equals(l)) {
					Log.setCurrentLevel(Log.TRACE);
				} else {
					Log.fatal("wrong log level %s",l);
					usage(1);
				}
			} else if ("-map".equals(args[i])) {
				map = args[++i];
			} else if ("-compact".equals(args[i])) {
				compact = true;
			} else if (("-h".equals(args[i])) || ("-help".equals(args[i])) || ("--help".equals(args[i]))) {
				usage(0);
			} else if ("-output".equals(args[i])) {
				output = args[++i];
			} else if ("-size".equals(args[i])) {
				String n = args[++i];
				try {
					if (n.startsWith("0x")) {
						size = Integer.valueOf(n.substring(2), 16);
					} else {
						size = Integer.valueOf(n, 10);
					}
				} catch (NumberFormatException e) {
					Log.fatal("wrong number %s",n);
					usage(1);
				}
			} else if ("-start".equals(args[i])) {
				start = args[++i];
				
			} else {
				if (compiler==null) {
					compiler = new Compiler(compact, size);
				}
				compiler.parse(args[i]);
			}
		}
		
		compiler.patchStart(start);

		switch (format) {
		case Bin:	compiler.writeBinFile(output); break;
		case Mem:	compiler.writeMemFile(output); break;
		case Init:	compiler.writeInitFile(output); break;
		case Hex:   compiler.writeHexFile(output); break;
		}
		if (map!=null) {
			compiler.writeMapFile(map);
		}
	}
	
	private static void usage(int code)
	{
		System.err.println("usage: java Compiler [options] file1 file2 ... filen");
		System.err.println("Options are:");
		System.err.println("    -format format  output format, 'bin' for binary, 'mem' for mem, 'init' for init values of bram, 'hex' for hexadecimal [default: bin]");	
		System.err.println("    -help           print this help and exit");
		System.err.println("    -compact        do not include word names in produced code -- the resulting system *cannot* be interactive");
		System.err.println("    -log level      set log level, in 'error','warning','info','debug','trace' [defaults to error]");
		System.err.println("    -map file       output memory map to file");
		System.err.println("    -output file    output to file [defaults to 'memory.bin']");
		System.err.println("    -size n         use a memory of n words [defaults to 0x800]");
		System.err.println("    -start word     run word at startup [defaults to __main]");
		System.exit(code);
	}

	private void patchStart(String start)
	{
		DefinedWord startWordDef = find(start);
		if (startWordDef==null) {
			Log.fatal("could not find start word %s",start);
			System.exit(1);
		}
		short main = (short)(startWordDef.xtAddress>>1);
		store(RESET, (short)(J1.UBRANCH | main));
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
				os.printf(" %04X", w&0xffff);
			}
			os.println();
		}
		os.close();
	}

	private void writeInitFile(String name)
			throws FileNotFoundException, IOException
	{
		PrintWriter os = new PrintWriter(name);
		for (int i=0; i<memory.length; i+=16) {
			os.printf("      .INIT_%02X(256'h",i>>4);
			for (int j=0; j<16; j++) {
				int w = memory[i+15-j];
				os.printf("%04X", w&0xffff);
				if (j<15) {
					os.print("_");
				}
			}
			os.println("),");
		}
		os.close();
	}

	private void writeHexFile(String name)
			throws FileNotFoundException, IOException
	{
		PrintWriter os = new PrintWriter(name);
		for (int i=0; i<memory.length; i++) {
			os.printf("%04X\n",memory[i]);
		}
		os.close();
	}


	private void writeMapFile(String name)
			throws FileNotFoundException, IOException
	{
		PrintWriter os = new PrintWriter(name);
		for (Map.Entry<String,DefinedWord> e : dict.entrySet()) {
			os.printf("%%%s:%04x\n", e.getKey(), e.getValue().xtAddress);
		}
		os.close();
	}
	
	public Compiler(boolean compact, int size) throws NoSuchMethodException
	{
		memory = new short[size];
		store(VAR_HERE,(short)INITIAL_HERE);
		store(VAR_DICT_HEAD,(short)0);
		store(VAR_LAST_DEF,(short)0);
		store(VAR_STATE,(short)0);
		
		Memory memoryInterface = new Memory() {
			public void write(short address, short value) {
				store(address,value);
			}
			public short read(short address) {
				return fetch(address);
		}};
		
		j1 = new Simulator(memoryInterface);
		j1.reset();

		dict = new HashMap<String, DefinedWord>();
		disassembler = new Disassembler(memoryInterface);

		nativeWords = new HashMap<String, NativeWord>();
		addNativeWord("\\",			"backslash");
		addNativeWord("(",			"paren");
		addNativeWord(":",			"colon");
		addNativeWord(";",			"semicolon");
		addNativeWord(";;",			"quickSemicolon");
		addNativeWord("immediate",	"immediate");
		addNativeWord("literal",	"literal");
		addNativeWord("postpone",	"postpone");
		addNativeWord("[",			"lBracket");
		addNativeWord("]",			"rBracket");
		addNativeWord("'",			"tick");

		addConstWord("$here",		VAR_HERE);
		addConstWord("$base",		VAR_BASE);
		if (!compact) {
			addConstWord("$dictHead",	VAR_DICT_HEAD);
			addConstWord("$lastDef",	VAR_LAST_DEF);
			addConstWord("$state",		VAR_STATE);
			addConstWord("$inputBuffer",VAR_INPUT_BUFFER);
			addConstWord("$inputSize",	VAR_INPUT_SIZE);
			addConstWord("$toIn",		VAR_TO_IN);
			addConstWord("$wordBuffer",	VAR_WORD_BUFFER);
		}

		aliasWords = new HashMap<String, short[]>();		
		aliasWords.put("+",		new short[] { J1.OP_PLUS });
		aliasWords.put("xor",	new short[] { J1.OP_XOR });
		aliasWords.put("and",	new short[] { J1.OP_AND });
		aliasWords.put("or",	new short[] { J1.OP_OR });
		aliasWords.put("invert",new short[] { J1.OP_INVERT });
		aliasWords.put("=",		new short[] { J1.OP_EQUAL });
		aliasWords.put("<",		new short[] { J1.OP_LT });
		aliasWords.put("u<",	new short[] { J1.OP_ULT });
		aliasWords.put("swap",	new short[] { J1.OP_SWAP });
		aliasWords.put("dup",	new short[] { J1.OP_DUP });
		aliasWords.put("drop",	new short[] { J1.OP_DROP });
		aliasWords.put("over",	new short[] { J1.OP_OVER });
		aliasWords.put("nip",	new short[] { J1.OP_NIP });
		aliasWords.put(">r",	new short[] { J1.OP_TOR });
		aliasWords.put("r>",	new short[] { J1.OP_RFROM });
		aliasWords.put("r@",	new short[] { J1.OP_RFETCH });
		aliasWords.put("@",		new short[] { J1.OP_FETCH });
		aliasWords.put("!",		new short[] { J1.OP_STORE1, J1.OP_STORE2 });
		aliasWords.put("dsp",	new short[] { J1.OP_DSP });
		aliasWords.put("lshift",new short[] { J1.OP_LSHIFT });
		aliasWords.put("rshift",new short[] { J1.OP_RSHIFT });
		aliasWords.put("1-",	new short[] { J1.OP_1MINUS });
    	aliasWords.put("r-drop",new short[] { J1.OP_RDROP });
    	aliasWords.put("exit",	new short[] { J1.OP_RETURN });
	}
	
	void addNativeWord(String word, String methodName)
		throws NoSuchMethodException
	{
		final Object _this = this;
		final Method m = this.getClass().getMethod("word_"+methodName);
		nativeWords.put(word, new NativeWord() {
			public void execute() throws Exception {
				m.invoke(_this);
			}
		});
	}
	
	void addConstWord(String word, final short v)
	{
		nativeWords.put(word, new NativeWord() {
			public void execute() throws Exception {
				pushD(v);
			}
		});
	}

	void addAliasWord(String word, String code)
	{
		final String[] words = code.split("\\s+");
		nativeWords.put(word, new NativeWord() {
			public void execute() throws Exception {
				for (String word : words) {
					NativeWord nw = nativeWords.get(word);
					if (nw==null) {
						throw new Error("unknown word "+word);
					}
					nw.execute();
				}
			}
		});
	}

	void	pushD(int w)			{ j1.pushD((short)w); }
	short	popD()					{ return j1.popD(); }
	void	pushR(int w)			{ j1.pushR((short)w); }
	short	popR()					{ return j1.popR(); }
	
	short getHere()					{ return fetch(VAR_HERE); }
	short getDictHead()				{ return fetch(VAR_DICT_HEAD); }
	short getLastDef()				{ return fetch(VAR_LAST_DEF); }
	short getState()				{ return fetch(VAR_STATE); }

	void setHere(short value)		{ store(VAR_HERE,value); }
	void setDictHead(short value)	{ store(VAR_DICT_HEAD,value); }
	void setLastDef(short value)	{ store(VAR_LAST_DEF,value); }
	void setState(short value)		{ store(VAR_STATE,value); }

	private void parse(String file) throws FileNotFoundException
	{
		
		LineNumberReader r = new LineNumberReader(new FileReader(file));
		try {
			parse(r);
		} catch (Exception e) {
			Log.error("error in file %s line %d: %s", file,r.getLineNumber(),e.getMessage());
			if (currentWord!=null) {
				Log.error("[while/after compiling %s]",currentWord.name);
			}
			Log.error("%s: %s",e.getClass().getSimpleName(),e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
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
//			System.out.print(w+" ");
			
			// if compiling lookup alias word
			short[] code = aliasWords.get(w);
			if ((code!=null) && (getState()!=0)) {
				for (short i : code) {
					comma(i);
				}
				continue;
			}
			
			// lookup target dictionary
			DefinedWord def = find(w);
			if (def!=null) {
				if ((getState()!=0) && (getFlags(def)&DefinedWord.IMMEDIATE)==0) {
					pushD(def.xt); word_scall();
				} else {
					Log.debug("executing %s",w);
					execute(def.xt);
				}
				continue;
			}
			
			// lookup native word
			NativeWord nw = nativeWords.get(w);
			if (nw!=null) {
				nw.execute();
				continue;
			}
			
			// try a number ?
			try {
				int n = (w.startsWith("0x")) ? Integer.parseInt(w.substring(2), 16) : Integer.parseInt(w);
				pushD((short)n);
				if (getState()!=0) {
					word_literal();
				}
				continue;
			} catch (NumberFormatException e) {
				// nothing
			}
			
			// fail
			throw new Exception("unknown word "+w);
		}
	}
	
	private void execute(short xt)
	{
		pushR(MARKER_PC<<1); // dummy address
		j1.setPc((short)(xt<<1));
		int depth = 0;
		while (j1.pc!=MARKER_PC) {

			if (Log.getCurrentLevel()>=Log.TRACE)
			{
				disassembler.setPc(j1.getPc());
				StringBuilder spaces = new StringBuilder();
				for (int i=0; i<depth; i++) {
					spaces.append("    ");
				}
				Log.trace("%04x %s%s",j1.getPc(),spaces.toString(),disassembler.disassemble());
				
				short insn = fetch(j1.getPc());
				if (J1.isCall(insn)) { depth++; }
				if (J1.isReturn(insn)) { depth--; }
			}
			j1.step();
		}
	}
	
	private boolean readLine() throws IOException
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
	
	private boolean atEndOfLine()
	{
		return fetch(VAR_TO_IN)==fetch(VAR_INPUT_SIZE);
	}
	
	private int nextChar()
	{
		short toIn = fetch(VAR_TO_IN);
		int r = cFetch(VAR_INPUT_BUFFER+toIn);
		store(VAR_TO_IN, (short)(toIn+1));
		return r;
	}
	
	private String word()
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

	private String parseUntil(int j) throws Exception
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
	
	private short fetch(int address)
	{
		return memory[address>>1];
	}
	
	private void store(int address, short value)
	{
		if (address>=INITIAL_HERE) {
			Log.debug("%04x<-%04x", (address&~1), value&0xffff);
		}
		memory[address>>1] = value;
	}
	
	private int cFetch(int address)
	{
		if ((address&1)!=0) {
			return (memory[address>>1]&0xff00)>>8;
		} else {
			return (memory[address>>1]&0x00ff)>>0;
		}
	}
	
	private void cStore(int address, int value)
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
	
	private void comma(int value) {
		store(getHere(), (short)value);
		setHere((short)(getHere()+2));
	}
	
	private void cComma(int value) {
		cStore(getHere(), value);
		setHere((short)(getHere()+1));
	}
	
	private void align() {
		setHere((short)((getHere()+1) & ~1));
	}
	
	private void literal(short w)
	{
		if (w>=0) {
			comma((short)(J1.IMMEDIATE | w));
		} else {
			literal((short)((~w)&0x7fff));
			comma(J1.ALU | J1.INVERT_T);
		}
	}

	private void startDefinition(String name)
	{
		currentWord = new DefinedWord();
		currentWord.name = name;

		if (!compact) {
			// start new def
			currentWord.defAddress = getHere();
			setLastDef(currentWord.defAddress);
	
			// pointer to previous word
			comma(getDictHead());
			
			// copy name (flags=0)
			currentWord.flagsAddress = getHere();
			currentWord.nameAddress = currentWord.flagsAddress;
			cComma((short)name.length());
			for (int i=0; i<name.length(); i++) {
				cComma((short)name.codePointAt(i));
			}
			align();
		}
		
		// start compiling
		currentWord.xtAddress = getHere();
		currentWord.xt = (short)(currentWord.xtAddress>>1);
		pushD(COLON_SYS);	// to check def sanity
		word_rBracket();	// compile mode

		// for the memory map
		Log.info("%04x xt of %s",currentWord.xtAddress,name);
		disassembler.addLabel(name, currentWord.xtAddress);
		
		if (name.equals("c@"))
			name = name;
	}

	private void endDefinition()
	{
		if (popD()!=COLON_SYS) {
			throw new Error("warning: missing colon-sys");
		}
		word_lBracket();
		if (!compact) {
			setDictHead(getLastDef());
		}
		dict.put(currentWord.name, currentWord);

		if (Log.getCurrentLevel()>=Log.INFO) {
			disassembler.setPc(((short)(currentWord.xt<<1)));
			while (disassembler.getPc()<getHere()) {
				String lbl = disassembler.getLabelForAddress(disassembler.getPc());
				if (lbl!=null) {
					Log.info("     %s:", lbl);
				}
				Log.info("%04x \t%s",disassembler.getPc(),disassembler.disassemble());
			}
		}
	}

	private int getFlags(DefinedWord def)
	{
		if (!compact) {
			return cFetch(def.flagsAddress)&0xe0;
		} else {
			return def.flags;
		}
	}
	
	private void setFlags(DefinedWord def, int flags)
	{
		if (!compact) {
			cStore(def.flagsAddress,(cFetch(def.flagsAddress)&0x1f)|flags);
		} else {
			def.flags = flags;
		}
	}

	private DefinedWord find(String name)
	{
		return dict.get(name);
	}
	
	

	
	public void word_backslash() throws Exception
	{
		while (!atEndOfLine()) { nextChar(); }
	}
	
	public void word_paren() throws Exception
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
	
	public void word_postpone() throws Exception
	{
		String s = word();
		DefinedWord def = find(s);
		if (def==null) {
			throw new Exception("unknown word "+s);
		}
		pushD(def.xt);word_scall();
	}
	
	public void word_literal()
	{
		literal(popD());
	}
		
	public void word_colon() throws Exception
	{
		String name = word();
		startDefinition(name);
	}
	
	public void word_semicolon() throws Exception
	{
		comma(J1.OP_RETURN);
		endDefinition();
	}
	
	public void word_quickSemicolon() throws Exception
	{
		short prevHere = (short)(getHere() - 2);
		short lastI = fetch(prevHere);
		store(prevHere, (short)(lastI|J1.RETURN|J1.R_M1));
		endDefinition();
	}
	
	public void word_immediate() throws Exception
	{
		setFlags(currentWord, getFlags(currentWord) | DefinedWord.IMMEDIATE);
	}
	
	public void word_tick() throws Exception
	{
		pushD(find(word()).xt);
	}
	
	public void word_d_sharp()	{ pushD(Integer.valueOf(word(), 10)); };
	public void word_h_sharp()	{ pushD(Integer.valueOf(word(), 16)); };

	public void word_scall()	{ comma(popD()|J1.CALL); }

	public void word_lBracket()	{ store(VAR_STATE, (short)0); }
	public void word_rBracket()	{ store(VAR_STATE, (short)1); }
}
