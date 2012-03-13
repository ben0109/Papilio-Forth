package j1;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

public class Disassembler {
	
	private Map<Short, String> labels;
	private Memory memory;
	private short pc;
	
	public Disassembler(Memory memory)
	{
		this.memory = memory;
		this.labels = new HashMap<Short, String>();
	}
	/*
	public Disassembler(Memory memory,Map<Short, String> labels)
	{
		this.memory = memory;
		this.labels = labels;
	}
	*/
	public void addLabel(String name, short address)
	{
		labels.put(address,name);
	}

	public short getPc()
	{
		return (short)(pc<<1);
	}

	public void setPc(short pc)
	{
		this.pc = (short)(pc>>1);
	}

	public String getLabelForAddress(short address)
	{
		return labels.get(address);
	}

	private short next()
	{
		return memory.read((short)((pc++)<<1));
	}
	
	private String sprintf(String format,int value)
	{
		return new Formatter().format(format, value).toString();
	}
	
	private String getName(int address)
	{
		address <<= 1;
		String name = getLabelForAddress((short)address);
		if (name != null) {
			return name;
		} else {
			return sprintf("$%04x", address);
		}
	}
	
	public String disassemble()
	{
		int i = next();
		switch (i&0xe000) {
		case J1.UBRANCH:
			return "jmp "+getName(i&0x1fff);
			
		case J1.ZBRANCH:
			return "jmp0 "+getName(i&0x1fff);

		case J1.CALL:
			return "call "+getName(i&0x1fff);

		case J1.ALU:
			StringBuilder sb = new StringBuilder();
			sb.append("% ");
			if ((i&J1.RETURN)!=0) {
				sb.append("ret ");
			} else {
				sb.append("    ");
			}
			switch (i&0xf00) {
			case J1.T:			sb.append("T    "); break;
			case J1.N:			sb.append("N    "); break;
			case J1.T_PLUS_N:	sb.append("T+N  "); break;
			case J1.T_AND_N:		sb.append("T&N  "); break;
			case J1.T_OR_N:		sb.append("T|N  "); break;
			case J1.T_XOR_N:		sb.append("T^N  "); break;
			case J1.INVERT_T:	sb.append("~T   "); break;
			case J1.N_EQ_T:		sb.append("N==T "); break;
			case J1.N_LT_T:		sb.append("N<T  "); break;
			case J1.N_LSHIFT_T:		sb.append("N<<T "); break;
			case J1.T_MINUS_1:	sb.append("T-1  "); break;
			case J1.R:			sb.append("R    "); break;
			case J1.MEM:			sb.append("[T]  "); break;
			case J1.N_RSHIFT_T:		sb.append("N>>T "); break;
			case J1.DEPTH:		sb.append("dpth "); break;
			case J1.N_LTU_T:		sb.append("Nu<T "); break;
			}
			if ((i&J1.T_TO_N)!=0) {
				sb.append("T->N ");
			} else {
				sb.append("     ");
			}
			if ((i&J1.T_TO_R)!=0) {
				sb.append("T->R ");
			} else {
				sb.append("     ");
			}
			if ((i&J1.N_TO_M)!=0) {
				sb.append("N->[T] ");
			} else {
				sb.append("       ");
			}
			switch (i&0xc) {
			case 0x0: sb.append("    "); break;
			case 0x4: sb.append("r+1 "); break;
			case 0x8: sb.append("r-2 "); break;
			case 0xc: sb.append("r-1 "); break;
			}
			switch (i&3) {
			case 0: sb.append("    "); break;
			case 1: sb.append("d+1 "); break;
			case 2: sb.append("d-2 "); break;
			case 3: sb.append("d-1 "); break;
			}
			sb.append("%");
			return sb.toString();

		default:
			return sprintf("$%04x", i&0x7fff);
		}
	}
}
