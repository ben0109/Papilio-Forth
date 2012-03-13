library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;
--library UNISIM;
--use UNISIM.VComponents.all;

entity uart_rx is
	port (
		clk:			in  STD_LOGIC;
		clk_divider:in  STD_LOGIC_VECTOR (15 downto 0);
		serial_in:	in  STD_LOGIC;
		rd:			in  STD_LOGIC;
		ready:		out STD_LOGIC;
		dout:			out STD_LOGIC_VECTOR (7 downto 0));
end uart_rx;

architecture Behavioral of uart_rx is

	signal bit_counter:	unsigned(3 downto 0) := (others=>'0');
	signal clk_counter:	unsigned(15 downto 0) := (others=>'0');
	signal last_in:		std_logic;
	signal shift:			std_logic_vector(8 downto 0) := (others=>'0');
	signal byte_received:std_logic := '0';
	signal ready_buf:		std_logic := '0';

begin

	ready <= ready_buf;
	dout	<= shift(7 downto 0);

	process (clk)
	begin
		if rising_edge(clk) then
			if bit_counter=0 then
				if last_in='1' and serial_in='0' then
					bit_counter <= "1010";
					clk_counter <= unsigned(clk_divider);
				else	
					last_in		<= serial_in;
				end if;
				byte_received	<= '0';

			else
				if clk_counter=0 then
					clk_counter <= unsigned(clk_divider);
				else
					if std_logic_vector(clk_counter)="0"&clk_divider(15 downto 1) then
						if bit_counter=1 then
							byte_received	<= serial_in;
						end if;
						shift(8 downto 0) <= serial_in&shift(8 downto 1);
						bit_counter <= bit_counter-1;
					end if;
					clk_counter <= clk_counter-1;
				end if;
			end if;
			
			ready_buf <= (ready_buf or byte_received) and not rd;
		end if;
	end process;
end Behavioral;

