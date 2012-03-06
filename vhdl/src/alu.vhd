library IEEE;
use IEEE.STD_LOGIC_1164.all;
USE ieee.numeric_std.ALL;

entity alu is
	port (
		alu_op:		in  std_logic_vector(3 downto 0);
		alu_in1:		in  std_logic_vector(15 downto 0);
		alu_in2:		in  std_logic_vector(15 downto 0);
		alu_out:		out std_logic_vector(15 downto 0);
		flag_c:		in  std_logic;
		flag_v:		in  std_logic;
		flag_z:		in  std_logic;
		flag_s:		in  std_logic;
		new_c:		out std_logic;
		new_v:		out std_logic;
		new_z:		out std_logic;
		new_s:		out std_logic);
end alu;

architecture rtl of alu is
	signal i_alu_out: std_logic_vector(15 downto 0);
	signal i_alu_out_z: std_logic;
	signal carry : unsigned(0 downto 0);
begin

	alu_out <= i_alu_out;

	with i_alu_out select
	i_alu_out_z	<= '1' when "0000000000000000",
						'0' when others;
						
	with alu_op select
	new_z			<= flag_z when "0000",
						i_alu_out_z when others;
					
	with alu_op select
	new_s			<= flag_s when "0000",
						i_alu_out(15) when others;

	carry <= "0" when flag_c='0' else "1";

	process (alu_op, alu_in1, alu_in2, flag_c, flag_v, flag_s, flag_s, flag_z, carry)
		variable in1,in2,tmp: unsigned(16 downto 0);
	begin
		in1 := "0"&unsigned(alu_in1);
		in2 := "0"&unsigned(alu_in2);
		case alu_op is
			
		when "0010" =>
			i_alu_out <= alu_in2(15)&alu_in2(15 downto 1);
			new_c <=  alu_in2(0);
			new_v <= flag_v;
			
		when "0011" =>
			i_alu_out <= "0"&alu_in2(15 downto 1);
			new_c <=  alu_in2(0);
			new_v <= flag_v;
			
		when "0100" =>
			i_alu_out <= alu_in2(14 downto 0)&"0";
			new_c <= alu_in2(15);
			new_v <= flag_v;
			
		when "0101" =>
			i_alu_out <= flag_c&alu_in2(15 downto 1);
			new_c <=  alu_in2(0);
			new_v <= flag_v;
			
		when "0110" =>
			i_alu_out <= alu_in2(14 downto 0)&flag_c;
			new_c <= alu_in2(15);
			new_v <= flag_v;

		when "0111" =>
			for I in 0 to 15 loop
				i_alu_out(I) <= not alu_in2(I);
			end loop;
			new_c <= flag_c;
			new_v <= flag_v;
			
		when "1000" =>
			for I in 0 to 15 loop
				i_alu_out(I) <= alu_in1(I) and alu_in2(I);
			end loop;
			new_c <= flag_c;
			new_v <= flag_v;
			
		when "1001" =>
			for I in 0 to 15 loop
				i_alu_out(I) <= alu_in1(I) or alu_in2(I);
			end loop;
			new_c <= flag_c;
			new_v <= flag_v;
			
		when "1010" =>
			for I in 0 to 15 loop
				i_alu_out(I) <= alu_in1(I) xor alu_in2(I);
			end loop;
			new_c <= flag_c;
			new_v <= flag_v;
			
		when "1100" =>
			tmp := in1 + in2;
			i_alu_out <= std_logic_vector(tmp(15 downto 0));
			new_c <= std_logic(tmp(16));
			new_v <= (not (in1(15) xor in2(15))) and (in2(15) xor tmp(15));
			
		when "1101" =>
			tmp := in1 + in2 + carry;
			i_alu_out <= std_logic_vector(tmp(15 downto 0));
			new_c <= std_logic(tmp(16));
			new_v <= (not (in1(15) xor in2(15))) and (in2(15) xor tmp(15));
			
		when "1110" =>
			tmp := in1 - in2;
			i_alu_out <= std_logic_vector(tmp(15 downto 0));
			new_c <= std_logic(tmp(16));
			new_v <= (in1(15) xor in2(15)) and (in1(15) xor tmp(15));
			
		when "1111" =>
			tmp := in1 - in2 - carry;
			i_alu_out <= std_logic_vector(tmp(15 downto 0));
			new_c <= std_logic(tmp(16));
			new_v <= (in1(15) xor in2(15)) and (in1(15) xor tmp(15));
			
		when others =>					-- ld,ldf
			i_alu_out <= alu_in2;
			new_c <= flag_c;
			new_v <= flag_v;
		end case;
	end process;
end architecture;

