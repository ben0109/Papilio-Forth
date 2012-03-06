package compiler;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.UnmodifiableSetException;

public class Instruction {
	
	public static final int OP_LD	= 0x0;
	public static final int OP_LDF	= 0x1;
	public static final int OP_ASR	= 0x2;
	public static final int OP_LSR	= 0x3;
	public static final int OP_LSL	= 0x4;
	public static final int OP_RRC	= 0x5;
	public static final int OP_RLC	= 0x6;
	public static final int OP_NOT	= 0x7;
	public static final int OP_AND	= 0x8;
	public static final int OP_OR	= 0x9;
	public static final int OP_XOR	= 0xa;
	public static final int OP_BRK	= 0xb;
	public static final int OP_ADD	= 0xc;
	public static final int OP_ADC	= 0xd;
	public static final int OP_SUB	= 0xe;
	public static final int OP_SBC	= 0xf;
	
	public static final int CC_EQ	= 0x0;
	public static final int CC_NE	= 0x1;
	public static final int CC_HS	= 0x2;
	public static final int CC_LO	= 0x3;
	public static final int CC_MI	= 0x4;
	public static final int CC_PL	= 0x5;
	public static final int CC_VS	= 0x6;
	public static final int CC_VC	= 0x7;
	public static final int CC_HI	= 0x8;
	public static final int CC_LS	= 0x9;
	public static final int CC_GE	= 0xa;
	public static final int CC_LT	= 0xb;
	public static final int CC_GT	= 0xc;
	public static final int CC_LE	= 0xd;
	public static final int CC_AL	= 0xf;

	public static final int ARG_DS	= 0;
	public static final int ARG_RS	= 1;
	public static final int ARG_A	= 2;
	public static final int ARG_B	= 3;
	public static final int ARG_PC	= 4;
	public static final int ARG_MEM	= 5;
	public static final int ARG_IMM	= 6;
	public static final int ARG_NXT	= 7;

	public static final Map<String,Integer> ALU;
	public static final Map<String,Integer> ARG;
	public static final Map<String,Integer> CC;
	
	static {
		ALU = new HashMap<String, Integer>();
		ALU.put("ld",  OP_LD);
		ALU.put("ldf", OP_LDF);
		ALU.put("asr", OP_ASR);
		ALU.put("lsr", OP_LSR);
		ALU.put("lsl", OP_LSL);
		ALU.put("rrc", OP_RRC);
		ALU.put("rlc", OP_RRC);
		ALU.put("not", OP_NOT);
		ALU.put("and", OP_AND);
		ALU.put("or",  OP_OR);
		ALU.put("xor", OP_XOR);
		ALU.put("add", OP_ADD);
		ALU.put("adc", OP_ADC);
		ALU.put("sub", OP_SUB);
		ALU.put("sbc", OP_SBC);

		ARG = new HashMap<String, Integer>();
		ARG.put("ds",  ARG_DS);
		ARG.put("rs",  ARG_RS);
		ARG.put("a",   ARG_A);
		ARG.put("b",   ARG_B);
		ARG.put("pc",  ARG_PC);
		ARG.put("(a)", ARG_MEM);
		ARG.put("imm", ARG_IMM);
		ARG.put("nxt", ARG_NXT);

		CC = new HashMap<String, Integer>();
		CC.put("eq", CC_EQ);
		CC.put("ne", CC_NE);
		CC.put("hi", CC_HI);
		CC.put("ls", CC_LS);
		CC.put("mi", CC_MI);
		CC.put("pl", CC_PL);
		CC.put("vs", CC_VS);
		CC.put("vc", CC_VC);
		CC.put("hs", CC_HS);
		CC.put("lo", CC_LO);
		CC.put("ge", CC_GE);
		CC.put("lt", CC_LT);
		CC.put("gt", CC_GT);
		CC.put("le", CC_LE);
		CC.put("al", CC_AL);
	}
	
	public static enum Type { Call,Alu,JumpRel,JumpAbs };
	
	public static final short RET, LD_DS_NXT;
	
	static {
		RET = getCodeForAlu(OP_LD, ARG_PC, ARG_RS);
		LD_DS_NXT = getCodeForAlu(OP_LD, ARG_DS, ARG_NXT);
	}

	private short instructionWord,extensionWord;
	private short address;

	public Instruction() {}
	
	public Instruction(short instructionWord)
	{
		this.instructionWord = instructionWord;
	}
	
	public short getInstructionWord() {
		return instructionWord;
	}

	public short getExtensionWord() {
		return extensionWord;
	}

	public void setExtensionWord(short w) {
		extensionWord = w;
	}

	int getBits(int p,int n, short s)
	{
		return (s>>p)&((1<<n)-1);
	}
	
	short setBits(int p,int n, short s, int value)
	{
		s &= ~(((1<<n)-1)<<p);
		s |= (value&((1<<n)-1))<<p;
		return s;
	}
	
	public int getArg1Bits()			{ return			getBits(13,3, instructionWord); }
	public void setArg1Bits(int v)		{ instructionWord =	setBits(13,3, instructionWord, v); }

	public int getOpBits()				{ return			getBits(9,4, instructionWord); }
	public void setOpBits(int v)		{ instructionWord =	setBits(9,4, instructionWord, v); }

	public int getCCBits()				{ return			getBits(9,4, instructionWord); }
	public void setCCBits(int i)		{ instructionWord =	setBits(9,4, instructionWord, i); }

	public int getArg2Bits()			{ return			getBits(6,3, instructionWord); }
	public void setArg2Bits(int v)		{ instructionWord =	setBits(6,3, instructionWord, v); }

	public int getImmBits()				{ return			getBits(1,5, instructionWord); }
	public void setImmBits(int i)		{ instructionWord =	setBits(1,5, instructionWord, i); }

	public int getOffsetBits()			{ return			getBits(1,8, instructionWord); }
	public void setOffsetBits(int i)	{ instructionWord =	setBits(1,8, instructionWord, i); }

	public short getImmediate()
	{
		int v = getImmBits();
		return (short)((v&0x10)!=0 ? (v|0xfff0) : v);
	}

	public Type getType()
	{
		if (isCall()) {
			return Type.Call;
		} else {
			switch (getArg1Bits()) {
			case ARG_IMM:	return Type.JumpRel;
			case ARG_NXT:	return Type.JumpAbs;
			default:		return Type.Alu;
			}
		}
	}

	public void setType(Type t)
	{
		switch (t) {
		case Call:
			instructionWord &= ~1;
			break;
		case JumpRel:
			instructionWord |= 1;
			setArg1Bits(ARG_IMM);
			break;
		case JumpAbs:
			instructionWord |= 1;
			setArg1Bits(ARG_NXT);
			break;
		default:
			instructionWord |= 1;
			setArg1Bits(ARG_A);
			break;
		}
	}
	
	public boolean isCall() { return (instructionWord&1)==0; }
	public boolean isLong()
	{
		switch (getType()) {
		case Call:		return false;
		case JumpRel:	return false;
		case JumpAbs:	return true;
		default:		return getArg2Bits()==ARG_NXT;
		}
	}
	
	public static short getCodeForAlu(int op,int arg1,int arg2)
	{
		Instruction i = new Instruction();
		i.setType(Type.Alu);
		i.setOpBits(op);
		i.setArg1Bits(arg1);
		i.setArg2Bits(arg2);
		return i.instructionWord;
	}
	
	public static short getCodeForAluImm(int op,int arg1,int imm)
	{
		Instruction i = new Instruction();
		i.setType(Type.Alu);
		i.setOpBits(op);
		i.setArg1Bits(arg1);
		i.setArg2Bits(ARG_IMM);
		i.setImmBits(imm);
		return i.instructionWord;
	}
	
	public static boolean canRelJump(short here,short to)
	{
		int offset = to-here-1;
		return (offset>=-0x80 && offset<0x80);
	}
	
	public static short getCodeForJumpRel(int cc,short here,short to)
	{
		Instruction i = new Instruction();
		i.setType(Type.JumpRel);
		i.setCCBits(cc);
		i.setOffsetBits(to-here-1&0xff);
		return i.instructionWord;
	}
	
	public static short getCodeForJumpAbs(int cc)
	{
		Instruction i = new Instruction();
		i.setType(Type.JumpAbs);
		i.setCCBits(cc);
		return i.instructionWord;
	}
	
	public static boolean isImmediateValueOK(int n)
	{
		return n>=-8 && n<8;
	}

	public int getExecOp() {
		switch (getType()) {
		case Call:		return OP_LD;
		case JumpRel:	return OP_ADD;
		case JumpAbs:	return OP_LD;
		default:		return getOpBits();
		}
	}
	
	public int getExecArg1() {
		switch (getType()) {
		case Call:		return ARG_RS;
		case JumpRel:	return ARG_PC;
		case JumpAbs:	return ARG_PC;
		default:		return getArg1Bits();
		}
	}
	
	public int getExecArg2() {
		switch (getType()) {
		case Call:		return ARG_PC;
		case JumpRel:	return ARG_IMM;
		case JumpAbs:	return ARG_NXT;
		default:		return getArg2Bits();
		}
	}
	
	public int getExecCC() {
		switch (getType()) {
		case JumpRel:	return getCCBits();
		case JumpAbs:	return getCCBits();
		default:		return CC_AL;
		}
	}
	
	public int getExecImm() {
		switch (getType()) {
		case JumpRel:	return getOffsetBits();
		default:		return getArg2Bits();
		}
	}
}
