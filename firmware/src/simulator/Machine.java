package simulator;
import java.util.Stack;

import compiler.Instruction;


public class Machine {
	
	public enum State { LoadI, Load1, Load2, Store };
	
	Memory memory;
	
	public State state;
	public short pc,r1,r2;
	public Stack<Short> ds,rs;
	public boolean cf,zf,vf,sf;
	
	private int instr_op,instr_cond,instr_arg1,instr_arg2;
	private short instr_imm;
	
	private int alu_in1,alu_in2;
	private short memory_address,memory_in,memory_out,stack_in,stack_out;
	private boolean memory_we,stack_push,stack_pop,stack_sel;
	
	public Machine(Memory memory)
	{
		this.memory = memory;
		reset();
	}

	public void reset()
	{
		/*
		state = State.Load2;
		instr_op = Instruction.OP_LD;
		instr_cond = Instruction.CC_AL;
		instr_arg1 = Instruction.ARG_PC;
		instr_arg2 = Instruction.ARG_NXT;
		pc = 0;
		*/

		state = State.Load2;
		pc = memory.read((short)0);
		
		r1 = r2 = 0;
		ds = new Stack<Short>();
		rs = new Stack<Short>();
		cf = zf = vf = sf = false;
		memory_we = stack_push = stack_pop = false;
	}
	
	public void clock()
	{
		boolean cond = doTest();
		int arg = computeArg();
		short alu_out = do_alu(false);
		
		boolean write	= cond && state==State.Store;
		boolean read	= cond && ((state==State.Load1) || (state==State.Load2));

		memory_in		= alu_out;
		memory_we		= (arg==Instruction.ARG_MEM) && write ;
		memory_address	= (arg==Instruction.ARG_MEM) ? r1 : pc ;

		stack_in		= alu_out;
		stack_sel		= (arg&1)!=0;
		stack_pop		= ((arg==Instruction.ARG_DS) || (arg==Instruction.ARG_RS)) && read;
		stack_push		= ((arg==Instruction.ARG_DS) || (arg==Instruction.ARG_RS)) && write;

		if (memory_we) {
			memory.write(memory_address, memory_in);
		} else {
			memory_out = memory.read(memory_address);
		}
		if (stack_push) {
			if (stack_sel)
				rs.push(stack_in);
			else
				ds.push(stack_in);
		} else if (stack_pop) {
			stack_out = (stack_sel) ? rs.pop() : ds.pop();
		}
		
		short arg_value;
		switch (arg) {
		case Instruction.ARG_DS:
		case Instruction.ARG_RS:	arg_value = stack_out; break;
		case Instruction.ARG_A:		arg_value = r1; break;
		case Instruction.ARG_B:		arg_value = r2; break;
		case Instruction.ARG_PC:	arg_value = pc; break;
		case Instruction.ARG_IMM:	arg_value = instr_imm; break;
		default:					arg_value = memory_out;  break;// (a),next
		}
		
		switch (state) {
		case LoadI:
			Instruction i = new Instruction(memory_out);
			if (i.isCall()) {
				instr_op	= Instruction.OP_LD;
				instr_cond	= Instruction.CC_AL;
				instr_arg1	= Instruction.ARG_RS;
				alu_in2		= (short)(pc+2);

				state		= State.Store;
				pc			= memory_out;
//				System.out.printf("calling %04x\n", pc);
					
			} else {
				int arg1 = i.getArg1Bits();
				switch (arg1) {
				case 6:
					instr_op	= Instruction.OP_ADD;
					instr_cond	= i.getCCBits();
					instr_arg1	= Instruction.ARG_PC;
					instr_arg2	= Instruction.ARG_IMM;
					instr_imm	= (short)(byte)(i.getOffsetBits()<<1);
					break;
						
				case 7:
					instr_op	= Instruction.OP_LD;
					instr_cond	= i.getCCBits();
					instr_arg1	= Instruction.ARG_PC;
					instr_arg2	= Instruction.ARG_NXT;
					break;
	
				default:
					instr_op	= i.getOpBits();
					instr_cond	= Instruction.CC_AL;
					instr_arg1	= arg1;
					instr_arg2	= i.getArg2Bits();
					instr_imm	= (short)i.getImmediate();
					break;
				}
				state	= (instr_op&8)!=0 ? State.Load1 : State.Load2;
				pc		= (short)(pc+2);
			}
			break;
			
		case Load1:
			alu_in1	= arg_value;
			state	= State.Load2;
			break;
			
		case Load2:
			alu_in2	= arg_value;
			if (arg==Instruction.ARG_NXT) {
				pc = (short)(pc+2);
			}
			state	= State.Store;
			break;
			
		case Store:
			if (doTest()) {
				short new_alu_out = do_alu(true);
				switch (instr_arg1) {
				case Instruction.ARG_A:		r1	= new_alu_out; break;
				case Instruction.ARG_B:		r2	= new_alu_out; break;
				case Instruction.ARG_PC:	pc	= new_alu_out; break;
				}
			}
			state = State.LoadI;
			break;
		}
	}
	
	public void step()
	{
		do {
			clock();
		} while (!state.equals(State.LoadI));
	}

	private int computeArg()
	{
		switch (state) {
		case Load1:
		case Store: return instr_arg1;
		case Load2: return instr_arg2;
		default:	return Instruction.ARG_PC;
		}
	}

	private short do_alu(boolean setFlags)
	{
		short alu_out = 0;
		int tmp;
		boolean sf=this.sf, zf=this.zf,vf=this.vf,cf=this.cf;
		alu_in1 &= 0xffff;
		alu_in2 &= 0xffff;
		switch (instr_op) {
		case Instruction.OP_LD:
			alu_out = (short)alu_in2;
			break;
		case Instruction.OP_LDF:
			alu_out = (short)alu_in2;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_ASR:
			alu_out = (short)(alu_in2>>1);
			cf = (alu_in2&1)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_LSR:
			alu_out = (short)((alu_in2>>1)&0x7fff);
			cf = (alu_in2&1)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_LSL:
			alu_out = (short)(alu_in2<<1);
			cf = (alu_in2&0x8000)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_RRC:
			alu_out = (short)((alu_in2>>1) | (cf?0x8000:0));
			cf = (alu_in2&1)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_RLC:
			alu_out = (short)((alu_in2<<1) | (cf?0x1:0));
			cf = (alu_in2&0x8000)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_NOT:
			alu_out = (short)(~alu_in2);
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_AND:
			alu_out = (short)(alu_in1 & alu_in2);
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_OR:
			alu_out = (short)(alu_in1 | alu_in2);
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_XOR:
			alu_out = (short)(alu_in1 ^ alu_in2);
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_ADD:
			tmp = alu_in1 + alu_in2;
			alu_out = (short)tmp;
			cf = (tmp&0x10000)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_ADC:
			tmp = alu_in1 + alu_in2 + (cf?1:0);
			alu_out = (short)tmp;
			cf = (tmp&0x10000)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_SUB:
			tmp = alu_in1 - alu_in2;
			alu_out = (short)tmp;
			cf = (tmp&0x10000)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		case Instruction.OP_SBC:
			tmp = alu_in1 - alu_in2 - (cf?1:0);
			alu_out = (short)tmp;
			cf = (tmp&0x10000)!=0;
			zf = alu_out==0; sf = (alu_out&0x8000)!=0;
			break;
		}
		if (setFlags) {
			this.cf = cf;
			this.vf = vf;
			this.zf = zf;
			this.sf = sf;
		}
		return alu_out;
	}
	
	boolean doTest()
	{
		switch (instr_cond) {
		case Instruction.CC_EQ: return zf;
		case Instruction.CC_NE: return !zf;
		case Instruction.CC_HS: return cf;
		case Instruction.CC_LO: return !cf;
		case Instruction.CC_MI: return sf;
		case Instruction.CC_PL: return !sf;
		case Instruction.CC_VS: return vf;
		case Instruction.CC_VC: return !vf;
		case Instruction.CC_HI: return cf && !zf;
		case Instruction.CC_LS: return !cf || zf;
		case Instruction.CC_GE: return sf==vf;
		case Instruction.CC_LT: return sf!=vf;
		case Instruction.CC_GT: return !zf && (sf==vf);
		case Instruction.CC_LE: return zf || (sf!=vf);
		case Instruction.CC_AL: return true;
		default: return false;
		}
	}
}
