library IEEE;
use IEEE.STD_LOGIC_1164.all;
USE ieee.numeric_std.ALL;
library UNISIM;
use UNISIM.VComponents.all;

entity stack is
	port (
		clk:			in  std_logic;
		direction:	in  std_logic;
		push:			in  std_logic;
		pop:			in  std_logic;
		d_in:			in  std_logic_vector(15 downto 0);
		d_out:		out std_logic_vector(15 downto 0);
		full:			out std_logic);

end entity;

architecture rtl of stack is

	signal a_ctrl:	std_logic_vector(1 downto 0);
	signal a:		unsigned(9 downto 0);
	signal l_tos:	unsigned(9 downto 0) := (others=>'1');
	signal h_tos:	unsigned(9 downto 0) := (others=>'0');

begin

	full <= '1' when l_tos=h_tos else '0';
	
	a_ctrl(0) <= push;
	a_ctrl(1) <= direction;
	with a_ctrl select
	a <=	h_tos-1	when "11",
			h_tos		when "10",
			l_tos+1	when "01",
			l_tos		when others;
			

   RAMB_inst : RAMB16_S18
   port map (
      CLK	=> clk,
      EN		=> '1',
      SSR	=> '0',
      WE		=> push,
      ADDR	=> std_logic_vector(a),
      DI		=> d_in,
      DO		=> d_out,
		DIP	=> "00"
   );
	
	process (clk)
	begin
		if falling_edge(clk) then
			if push='1' then
				if direction='0' then
					l_tos <= l_tos+1;
				else
					h_tos <= h_tos-1;
				end if;

			elsif pop='1' then
				if direction='0' then
					l_tos <= l_tos-1;
				else
					h_tos <= h_tos+1;
				end if;
			end if;
		end if;
	end process;

end architecture;

