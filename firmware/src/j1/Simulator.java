package j1;

public class Simulator
{	
	public short pc,d0,rs_a,ds_a;
	public short[] rs = new short[32];
	public short[] ds = new short[32];
	
	Memory memory;
	
	public Simulator(Memory memory)
	{
		this.memory = memory;
	}
	
	public short getPc() {
		return (short)(pc<<1);
	}

	public void setPc(short pc) {
		this.pc = (short)(pc>>1);
	}

	boolean getBit(short v, int i) { return ((v>>i)&1)!=0; }
	int getBits(short v, int i,int n) { return (v>>i)&((1<<n)-1); }
	
	public void reset()
	{
		pc = 0;
		d0 = 0;
		rs_a = -1;
		ds_a = -2;
	}
	
	public void step()
	{
		short instr = memory.read((short)(pc<<1));
		pc++;
		
		short r0 = rs[rs_a&0x1f];
		short d1 = ds[ds_a&0x1f];
		
		if ((instr&J1.IMMEDIATE)!=0) {
			ds[(++ds_a)&0x1f] = d0;
			d0	= (short)getBits(instr,0,15);
		} else {
			switch (instr&0x6000) {
			case J1.UBRANCH:
				pc			= (short)getBits(instr,0,11);
				break;
				
			case J1.ZBRANCH:
				if (d0==0) {
					pc = (short)getBits(instr,0,11);
				}
				popD();
				break;
				
			case J1.CALL:
				rs[(++rs_a)&0x1f]	= (short)(pc<<1);
				pc			= (short)getBits(instr,0,11);
				break;
				
			case J1.ALU:
				if (getBit(instr,12)) {
					pc = (short)(r0>>1);
				}
				short next_d0;
				switch (instr&0x0f00) {
				case J1.T:			next_d0 = d0; break;
				case J1.N:			next_d0 = d1; break;
				case J1.T_PLUS_N:	next_d0 = (short)(d0+d1); break;
				case J1.T_AND_N:	next_d0 = (short)(d0&d1); break;
				case J1.T_OR_N:		next_d0 = (short)(d0|d1); break;
				case J1.T_XOR_N:	next_d0 = (short)(d0^d1); break;
				case J1.INVERT_T:	next_d0 = (short)(~d0); break;
				case J1.N_EQ_T:		next_d0 = (short)((d0==d1) ? -1 : 0); break;
				case J1.N_LT_T:		next_d0 = (short)((d1<d0) ? -1 : 0); break;
				case J1.N_LTU_T:	next_d0 = (short)(((d1&0xffff)<(d0&0xffff)) ? -1 : 0); break;
				case J1.N_RSHIFT_T:	next_d0 = (short)(d1>>d0); break;
				case J1.N_LSHIFT_T:	next_d0 = (short)(d1<<d0); break;
				case J1.T_MINUS_1:	next_d0 = (short)(d0-1); break;
				case J1.R:			next_d0 = r0; break;
				case J1.MEM:		next_d0 = memory.read(d0); break;
				case J1.DEPTH:		next_d0 = ds_a; break;
				default: throw new Error();
				}
				rs_a += getSigned(getBits(instr,2,2));
				ds_a += getSigned(getBits(instr,0,2));
				if (getBit(instr,7)) {
					ds[ds_a&0x1f] = d0;
				}
				if (getBit(instr,6)) {
					rs[rs_a&0x1f] = d0;
				}
				if (getBit(instr,5)) {
					memory.write(d0, d1);
				}
				d0 = next_d0;
				break;
			default: throw new Error();
			}
		}
	}
	
	private short getSigned(int bits) {
		return bits>=2 ? (short)(bits|0xfffe) : (short)bits;
	}
	
	public void pushD(short w) { ds[(++ds_a)&0x1f] = d0; d0 = w; }
	public short popD() { short r = d0; d0 = ds[(ds_a--)&0x1f]; return r; }
	public void pushR(short w) { rs[(++rs_a)&0x1f] = w; }
	public short popR() { return rs[(rs_a--)&0x1f]; }
}
