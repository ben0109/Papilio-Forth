package simulator;


public class MemoryImpl implements Memory
{
	public interface SerialOut {
		void writeChar(int code);
	}
	
	short[] ram = new short[0x8000];
	SerialOut serialOut;
	
	boolean inCharReady = false;
	int inCharCode;

	public MemoryImpl(short[] code, SerialOut serialOut)
	{
		for (int i=0; i<code.length; i++) ram[i] = code[i];
		this.serialOut = serialOut;
	}
	
	public void receiveChar(int code)
	{
		inCharCode = code;
		inCharReady = true;
	}
	
	@Override
	public short read(short a)
	{
		int asInt = a&0xffff;
		if (asInt<0xff00) {
			return ram[asInt>>1];
		} else {
			switch (asInt) {
			case 0xfffc: return (short)(inCharReady?3:2);
			case 0xfffe: if (inCharReady) { inCharReady = false; return (short)inCharCode; } else { return 0; }
			default: return 0;
			}
		}
	}
	
	@Override
	public void write(short a, short value)
	{
		int asInt = a&0xffff;
		if (asInt<0xff00) {
			System.out.printf("%04x<=%04x\n",asInt,value);
			ram[asInt>>1] = value;
		} else {
			switch (asInt) {
			case 0xfffe: if (this.serialOut!=null) { serialOut.writeChar(value&0xff); } break;
			}
		}
	}
}
