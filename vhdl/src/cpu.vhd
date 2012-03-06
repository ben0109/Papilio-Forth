library IEEE;
use IEEE.STD_LOGIC_1164.all;
USE ieee.numeric_std.ALL;

entity cpu is
	port (
		clk:		in  std_logic;
		read:		out std_logic;
		write:	out std_logic;
		a:			out std_logic_vector (14 downto 0);
		d_in:		out std_logic_vector (15 downto 0);
		d_out:	in  std_logic_vector (15 downto 0));
end entity;

architecture rtl of cpu is

	component alu is
		port (
			alu_op:		in  std_logic_vector (3 downto 0);
			alu_in1:		in  std_logic_vector (15 downto 0);
			alu_in2:		in  std_logic_vector (15 downto 0);
			alu_out:		out std_logic_vector (15 downto 0);
			flag_c:		in  std_logic;
			flag_v:		in  std_logic;
			flag_z:		in  std_logic;
			flag_s:		in  std_logic;
			new_c:		out std_logic;
			new_v:		out std_logic;
			new_z:		out std_logic;
			new_s:		out std_logic);
	end component;
	
	component stack is
		port (
			clk:			in  std_logic;
			d_in:			in  std_logic_vector (15 downto 0);
			d_out:		out std_logic_vector (15 downto 0);
			full:			out std_logic;
			direction:	in  std_logic;
			push:			in  std_logic;
			pop:			in  std_logic);
	end component;


	constant S_LOADI:	integer := 0;
	constant S_LOAD1:	integer := 1;
	constant S_LOAD2:	integer := 2;
	constant S_STORE:	integer := 3;

	-- processor state
	signal state:				integer := S_LOAD2;
	signal pc:					unsigned(14 downto 0) := (others=>'0');
	signal reg_a:				std_logic_vector(15 downto 0) := (others=>'0');
	signal reg_b:				std_logic_vector(15 downto 0) := (others=>'0');
	signal flag_c:				std_logic := '0';
	signal flag_v:				std_logic := '0';
	signal flag_z:				std_logic := '0';
	signal flag_s:				std_logic := '0';

	-- control lines
	signal instr_op:			std_logic_vector(3 downto 0) := "0000"; -- ld
	signal instr_cc:			std_logic_vector(3 downto 0) := "1111"; -- al
	signal instr_arg1:		std_logic_vector(2 downto 0) := "100"; -- pc
	signal instr_arg2:		std_logic_vector(2 downto 0) := "111"; -- nxt
	signal instr_imm:			std_logic_vector(15 downto 0);
	
	signal base_cc:			std_logic;
	signal final_cc:			std_logic;

	signal write_cycle:		std_logic;
	signal read_cycle:		std_logic;

	signal arg:					std_logic_vector(2 downto 0);
	signal arg_value:			std_logic_vector(15 downto 0);

	signal stack_d_in:		std_logic_vector(15 downto 0);
	signal stack_d_out:		std_logic_vector(15 downto 0);
	signal stack_direction:	std_logic;
	signal stack_push:		std_logic;
	signal stack_pop:			std_logic;

	signal alu_in1:			std_logic_vector(15 downto 0);
	signal alu_in2:			std_logic_vector(15 downto 0);
	signal alu_out:			std_logic_vector(15 downto 0);
	signal alu_flag_c:		std_logic;
	signal alu_flag_v:		std_logic;
	signal alu_flag_z:		std_logic;
	signal alu_flag_s:		std_logic;

begin

	stacks: stack
	port map (
		clk			=> not clk,
		d_in			=> stack_d_in,
		d_out			=> stack_d_out,
		direction	=> stack_direction,
		push			=> stack_push,
		pop			=> stack_pop);

	alu_inst: alu
	port map (
		alu_op	=> instr_op,
		alu_in1	=> alu_in1,
		alu_in2	=> alu_in2,
		alu_out	=> alu_out,
		flag_c	=> flag_c,
		flag_v	=> flag_v,
		flag_z	=> flag_z,
		flag_s	=> flag_s,
		new_c		=> alu_flag_c,
		new_v		=> alu_flag_v,
		new_z		=> alu_flag_z,
		new_s		=> alu_flag_s);

	with state select
	arg <=	instr_arg1 when S_LOAD1,
				instr_arg2 when S_LOAD2,
				instr_arg1 when S_STORE,
				"100" when others;

	with arg select
	arg_value	<= stack_d_out						when "000", -- ds
						stack_d_out						when "001", -- rs
						reg_a								when "010", -- a
						reg_b								when "011", -- b
						std_logic_vector(pc)&"0"	when "100", -- pc
						instr_imm						when "110", -- imm
						d_out								when others;-- (reg),next
						
	with instr_cc(3 downto 1) select
	base_cc <=  flag_z when "000",
					flag_c when "001",
					flag_s when "010",
					flag_v when "011",
					flag_c and not flag_z when "100",
					not (flag_s xor flag_v) when "101",
					not flag_z and not (flag_s xor flag_v) when "110",
					'0' when others;
					
	final_cc <= not base_cc when instr_cc(0)='1' else base_cc;

	write_cycle			<= final_cc when state=S_STORE else '0';
	read_cycle			<= final_cc when state=S_LOAD1 or state=S_LOAD2 else '0';

	read		<= read_cycle				when arg="101" else '0';
	write		<= write_cycle				when arg="101" else '0';
	a			<=	reg_a(15 downto 1)	when arg="101" else std_logic_vector(pc);
	d_in		<= alu_out;

	stack_d_in			<= alu_out;
	stack_direction	<= arg(0);
	stack_pop			<= read_cycle  when (arg="000" or arg="001") else '0';
	stack_push			<= write_cycle when (arg="000" or arg="001") else '0';

	process (clk)
	begin
		if rising_edge(clk) then
			case state is				
			when S_LOADI =>
				if d_out(0)='0' then
					instr_cc		<= "1111";	-- al
					instr_op		<= "0000";	-- ld
					instr_arg1	<= "001";	-- rs
					alu_in2		<= std_logic_vector(pc+1)&"0";

					state			<= S_STORE;
					pc				<= unsigned(d_out(15 downto 1));
					
				else
					case (d_out(15 downto 13)) is
					when "111" =>
						instr_cc		<= d_out(12 downto 9);
						instr_op		<= "0000";	-- ld
						instr_arg1	<= "100";	-- pc
						instr_arg2	<= "111";	-- nxt
						state <= S_LOAD2;
						
					when "110" =>
						instr_cc		<= d_out(12 downto 9);
						instr_op		<= "1100";	-- add
						instr_arg1	<= "100";	-- pc
						instr_arg2	<= "110";	-- imm
						instr_imm(0)<= '0';
						instr_imm( 7 downto 1) <= d_out(7 downto 1);
						instr_imm(15 downto 8) <= (others => d_out(8));
						state <= S_LOAD1;

					when others =>
						instr_cc		<= "1111";	-- al
						instr_op		<= d_out(12 downto 9);
						instr_arg1	<= d_out(15 downto 13);
						instr_arg2	<= d_out(8 downto 6);
						instr_imm( 3 downto 0) <= d_out(4 downto 1);
						instr_imm(15 downto 4) <= (others => d_out(5));
						if d_out(12)='1' then
							state <= S_LOAD1;
						else
							state <= S_LOAD2;
						end if;
					end case;
					pc <= pc+1;
				end if;

			when S_LOAD1 =>
				alu_in1	<= arg_value;
				state		<= S_LOAD2;

			when S_LOAD2 =>
				if arg="111" then
					pc <= pc+1;
				end if;
				alu_in2	<= arg_value;
				if final_cc='1' then
					state <= S_STORE;
				else
					state <= S_LOADI;
				end if;

			when S_STORE =>
				case instr_arg1 is
					when "010" => reg_a	<= alu_out;
					when "011" => reg_b	<= alu_out;
					when "100" => pc		<= unsigned(alu_out(15 downto 1));
					when others =>
				end case;
				flag_c	<= alu_flag_c;
				flag_v	<= alu_flag_v;
				flag_z	<= alu_flag_z;
				flag_s	<= alu_flag_s;
				state		<= S_LOADI;

			when others =>
			end case;
		end if;
	end process;

end architecture;

