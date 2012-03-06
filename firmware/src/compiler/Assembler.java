package compiler;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assembler {
	
	private Compiler state;
	private Map<String,Short> definedLabels;
	private Map<Short,String> labels;
	private Collection<Map.Entry<Short,String>> neededLabels;
	
	public Assembler(Compiler state)
	{
		this.state = state;		
		this.labels = new HashMap<Short,String>();
		this.definedLabels = new HashMap<String, Short>();
		this.neededLabels = new ArrayList<Map.Entry<Short,String>>();
	}
	
	public Map<Short,String> getLabels()
	{
		return labels;
	}

	public void defineLabel(String label)
	{
		definedLabels.put(label,state.getHere());
		labels.put(state.getHere(),label);
	}
	
	public short useLabelAddress(String label)
	{
		Short a = definedLabels.get(label);
		if (a==null) {
			neededLabels.add(new AbstractMap.SimpleEntry<Short,String>(state.getHere(),label));
			return 0;
		} else{
			return a;
		}
	}
	
	public void patchForwardJumps() throws Exception
	{
		for (Map.Entry<Short,String> e : neededLabels) {
			String label = e.getValue();
			Short from = e.getKey();
			Short to = definedLabels.get(label);
			if (to==null) {
				throw new Exception("missing label: "+label);
			}
			state.store(from, to);
		}
	}

	private static final Pattern PATTERN_LABEL	= Pattern.compile("\\s*([^\\s]+)\\s*:\\s*");
	private static final Pattern PATTERN_CALL	= Pattern.compile("\\s*call\\s+([^\\s]+)\\s*");
	private static final Pattern PATTERN_JMP	= Pattern.compile("\\s*jmp\\s+([^\\s]+)\\s*,\\s*([^\\s]+)\\s*");
	private static final Pattern PATTERN_ALU	= Pattern.compile("\\s*([^\\s]+)\\s+([^\\s]+)\\s*,\\s*([^\\s]+)\\s*");
	
	private void translateCall(String target)
		throws Exception
	{
		state.comma(useLabelAddress(target));
	}
	
	private void translateJump(String cc,String label)
		throws Exception
	{
		Integer code  = Instruction.CC.get(cc);
		if (code==null) throw new Exception("unknown cc "+cc);
		
		state.comma(Instruction.getCodeForJumpAbs(code));
		state.comma(useLabelAddress(label));
	}

	private void translateAlu(String alu, String arg1, String arg2)
		throws Exception
	{
		Instruction r = new Instruction();
		r.setType(Instruction.Type.Alu);
		Integer code;
		
		code  = Instruction.ALU.get(alu);
		if (code==null) throw new Exception("unknown instr "+alu);
		r.setOpBits(code);

		code = Instruction.ARG.get(arg1);
		if (code==null) throw new Exception("illegal arg "+arg1);
		r.setArg1Bits(code);
		
		if (arg2.startsWith("#")) {
			int i = arg2.startsWith("#0x") ? Integer.parseInt(arg2.substring(3),16) : Integer.valueOf(arg2.substring(1));
			if (i>=-8 && i<8) {
				r.setArg2Bits(Instruction.ARG_IMM);
				r.setImmBits(i&0xf);
				state.comma(r.getInstructionWord());
			} else if (i>=-0x8000 && i<0x10000) {
				r.setArg2Bits(Instruction.ARG_NXT);
				state.comma(r.getInstructionWord());
				state.comma((short)(i&0xffff));
			} else {
				throw new Exception("const too big "+arg2);
			}
			
		} else {
			code = Instruction.ARG.get(arg2);
			if (code!=null) {
				r.setArg2Bits(code);
				state.comma(r.getInstructionWord());
			} else {
				r.setArg2Bits(Instruction.ARG_NXT);
				state.comma(r.getInstructionWord());
				state.comma(useLabelAddress(arg2));
			}
		}
	}
	
	public void translate(String s) throws Exception
	{
		Matcher m;
		
		m = PATTERN_LABEL.matcher(s);
		if (m.matches()) {
			defineLabel(m.group(1));
			return;
		}
		
		m = PATTERN_CALL.matcher(s);
		if (m.matches()) {
			translateCall(m.group(1));
			return;
		}
		
		m = PATTERN_JMP.matcher(s);
		if (m.matches()) {
			translateJump(m.group(1), m.group(2));
			return;
		}
		
		m = PATTERN_ALU.matcher(s);
		if (m.matches()) {
			String alu = m.group(1);
			String arg1 = m.group(2);
			String arg2 = m.group(3);
			translateAlu(alu, arg1, arg2);
			return;
		}
		
		throw new Error("syntax error: "+s);
	}

}
