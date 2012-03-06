package compiler;

import java.util.Formatter;
import java.util.Map;


import simulator.Memory;



public class Disassembler {
	
	private Map<Short, String> labels;
	private Memory memory;
	private short pc;
	
	public Disassembler(Memory memory,Map<Short, String> labels)
	{
		this.memory = memory;
		this.labels = labels;
	}

	public short getPc()
	{
		return pc;
	}

	public void setPc(short pc)
	{
		this.pc = pc;
	}

	private short next()
	{
		short w = memory.read(pc);
		pc += 2;
		return w;
	}
	
	private String sprintf(String format,int value)
	{
		return new Formatter().format(format, value).toString();
	}
	
	private String getName(int address)
	{
		String name = labels.get((short)address);
		if (name != null) {
			return name;
		} else {
			return sprintf("$%04x", address);
		}
	}

	public String disassemble()
	{
		Instruction i = new Instruction(next());
		switch (i.getType()) {
		case Call:
			return "call "+getName(i.getInstructionWord());

		case JumpRel:
			return "jr "+reverseLookup(Instruction.CC,i.getCCBits())+","+getName(pc+2+(byte)(i.getOffsetBits()<<1));

		case JumpAbs:
			return "jmp "+reverseLookup(Instruction.CC,i.getCCBits())+","+getName(next());

		default:
			String op = reverseLookup(Instruction.ALU,i.getOpBits());
			
			int tmp_arg2 = i.getArg2Bits();
			String arg2 = 
				(tmp_arg2==Instruction.ARG_IMM) ? sprintf("$%x", i.getImmBits()) : 
				(tmp_arg2==Instruction.ARG_NXT) ? sprintf("$%04x", next()&0xffff) :
				reverseLookup(Instruction.ARG,tmp_arg2);
				
			String arg1 = reverseLookup(Instruction.ARG,i.getArg1Bits());
			return op+" "+arg1+","+arg2;
		}
	}

	public boolean isCall(short i)
	{
		return ((i&0x8000) == 0);
	}

	public int getInstrLength(short code)
	{
		Instruction i = new Instruction(code);
		switch (i.getType()) {
		case Alu: return (i.getArg2Bits()==Instruction.ARG_NXT) ? 2 : 1;
		case JumpAbs: return 2;
		default: return 1;
		}
	}
	
	private String reverseLookup(Map<String,Integer> map,int value)
	{
		for (Map.Entry<String,Integer> e : map.entrySet()) {
			if (e.getValue()==value) {
				return e.getKey();
			}
		}
		return null;
	}
}
