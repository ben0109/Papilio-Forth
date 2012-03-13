library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity uart_tx is
	port (
		clk:			in  STD_LOGIC;
		clk_divider:in  STD_LOGIC_VECTOR (15 downto 0);
		din:			in  STD_LOGIC_VECTOR (7 downto 0);
		wr:			in  STD_LOGIC;
		ready:		out STD_LOGIC;
		serial_out:	out STD_LOGIC);
end uart_tx;

architecture Behavioral of uart_tx is

	signal bit_counter:	unsigned( 3 downto 0) := (others=>'0');
	signal clk_counter:	unsigned(15 downto 0) := (others=>'0');
	signal shift:			std_logic_vector(7 downto 0);

begin

	ready <= '1' when bit_counter=0 else '0';

	process (clk)
	begin
		if rising_edge(clk) then
			if wr='1' then
				bit_counter <= "1010";
				clk_counter <= unsigned(clk_divider);
				shift <= din;
				serial_out <= '0';
				
			elsif bit_counter>0 then
				if clk_counter=0 then
					serial_out <= shift(0);
					shift(7 downto 0) <= "1"&shift(7 downto 1);
					bit_counter <= bit_counter-1;
					clk_counter <= unsigned(clk_divider);
				else
					clk_counter <= clk_counter-1;
				end if;
				
			else
				serial_out <= '1';
			end if;
		end if;
	end process;

end Behavioral;

