package j1;

public class J1 {
	public static final short IMMEDIATE	= (short)0x8000;
	
	public static final short UBRANCH	= (short)0x0000;
	public static final short ZBRANCH	= (short)0x2000;
	public static final short CALL		= (short)0x4000;
	public static final short ALU		= (short)0x6000;
	
	public static final short RETURN	= (short)0x1000;
	
	public static final short T			= (short)0x0000;
	public static final short N			= (short)0x0100;
	public static final short T_PLUS_N	= (short)0x0200;
	public static final short T_AND_N	= (short)0x0300;
	public static final short T_OR_N	= (short)0x0400;
	public static final short T_XOR_N	= (short)0x0500;
	public static final short INVERT_T	= (short)0x0600;
	public static final short N_EQ_T	= (short)0x0700;
	public static final short N_LT_T	= (short)0x0800;
	public static final short N_RSHIFT_T= (short)0x0900;
	public static final short T_MINUS_1	= (short)0x0a00;
	public static final short R			= (short)0x0b00;
	public static final short MEM		= (short)0x0c00;
	public static final short N_LSHIFT_T= (short)0x0d00;
	public static final short DEPTH		= (short)0x0e00;
	public static final short N_LTU_T	= (short)0x0f00;

	public static final short T_TO_N	= (short)0x0080;
	public static final short T_TO_R	= (short)0x0040;
	public static final short N_TO_M	= (short)0x0020;
	public static final short D_P1		= (short)0x0001;
	public static final short D_M1		= (short)0x0003;
	public static final short R_P1		= (short)0x0004;
	public static final short R_M2		= (short)0x0008;
	public static final short R_M1		= (short)0x000c;

	public static boolean isImmediate(short i)
	{
		return (i&J1.IMMEDIATE)!=0;
	}

	public static boolean isAlu(short i)
	{
		return ((i&0xe000) == J1.ALU);
	}

	public static boolean isCall(short i)
	{
		return ((i&0xe000) == J1.CALL);
	}

	public static boolean isReturn(short i)
	{
		return (i&J1.RETURN)!=0;
	}
	
	public static int getRSPOffset(short i)
	{
		switch (i&0xc) {
		case 0x4:	return 1;
		case 0x8:	return -2;
		case 0xc:	return -1;
		default:	return 0;
		}
	}
	
	public static int getDSPOffset(short i)
	{
		switch (i&3) {
		case 1:		return 1;
		case 2:		return -2;
		case 3:		return -1;
		default:	return 0;
		}
	}
	
	public static final short OP_PLUS	= T_PLUS_N					| D_M1	| ALU;
	public static final short OP_AND	= T_AND_N					| D_M1	| ALU;
	public static final short OP_OR		= T_OR_N					| D_M1	| ALU;
	public static final short OP_XOR	= T_XOR_N					| D_M1	| ALU;
	public static final short OP_INVERT	= INVERT_T							| ALU;
	public static final short OP_EQUAL	= N_EQ_T					| D_M1	| ALU;
	public static final short OP_LT		= N_LT_T					| D_M1	| ALU;
	public static final short OP_ULT	= N_LTU_T					| D_M1	| ALU;

	public static final short OP_SWAP	= N			| T_TO_N				| ALU;
	public static final short OP_DUP	= T			| T_TO_N		| D_P1	| ALU;
	public static final short OP_DROP	= N							| D_M1	| ALU;
	public static final short OP_OVER	= N			| T_TO_N		| D_P1	| ALU;
	public static final short OP_NIP	= T							| D_M1	| ALU;
	public static final short OP_TOR	= N			| T_TO_R| R_P1	| D_M1	| ALU;
	public static final short OP_RFROM	= R			| T_TO_N| R_M1	| D_P1	| ALU;
	public static final short OP_RFETCH	= R			| T_TO_N		| D_P1	| ALU;
	public static final short OP_FETCH	= MEM								| ALU;
	public static final short OP_STORE1 = T			| N_TO_M		| D_M1	| ALU;
	public static final short OP_STORE2 = N							| D_M1	| ALU;
	public static final short OP_DSP	= DEPTH		| T_TO_N		| D_P1	| ALU;
	public static final short OP_LSHIFT	= N_LSHIFT_T					| D_M1	| ALU;
	public static final short OP_RSHIFT	= N_RSHIFT_T					| D_M1	| ALU;
	public static final short OP_1MINUS	= T_MINUS_1							| ALU;

	public static final short OP_RDROP	= T					| R_M1			| ALU;
	public static final short OP_RETURN	= RETURN | T		| R_M1			| ALU;
}
