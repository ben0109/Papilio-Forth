library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity io is
	port (
		clk:		in  STD_LOGIC;
		io:		inout STD_LOGIC_VECTOR (15 downto 0);
		write:	in  STD_LOGIC;
		a:			in  STD_LOGIC;
		d_in:		in  STD_LOGIC_VECTOR (15 downto 0);
		d_out:	out STD_LOGIC_VECTOR (15 downto 0));
end io;

architecture Behavioral of io is

	signal control:STD_LOGIC_VECTOR (15 downto 0) := (others=>'0');
	signal value:	STD_LOGIC_VECTOR (15 downto 0);

begin

	process (clk)
	begin
		if rising_edge(clk) then
			if write='1' then 
				if a='0' then
					control	<= d_in;
				else
					value		<= d_in;
				end if;
			end if;
		end if;
	end process;
	
	io( 0) <= value( 0) when control( 0)='1' else 'Z';
	io( 1) <= value( 1) when control( 1)='1' else 'Z';
	io( 2) <= value( 2) when control( 2)='1' else 'Z';
	io( 3) <= value( 3) when control( 3)='1' else 'Z';
	io( 4) <= value( 4) when control( 4)='1' else 'Z';
	io( 5) <= value( 5) when control( 5)='1' else 'Z';
	io( 6) <= value( 6) when control( 6)='1' else 'Z';
	io( 7) <= value( 7) when control( 7)='1' else 'Z';
	io( 8) <= value( 8) when control( 8)='1' else 'Z';
	io( 9) <= value( 9) when control( 9)='1' else 'Z';
	io(10) <= value(10) when control(10)='1' else 'Z';
	io(11) <= value(11) when control(11)='1' else 'Z';
	io(12) <= value(12) when control(12)='1' else 'Z';
	io(13) <= value(13) when control(13)='1' else 'Z';
	io(14) <= value(14) when control(14)='1' else 'Z';
	io(15) <= value(15) when control(15)='1' else 'Z';

	d_out <= io;

end Behavioral;

