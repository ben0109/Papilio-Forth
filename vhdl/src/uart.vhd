library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity uart is
	port (
		clk:		in  STD_LOGIC;
		rx:		in  STD_LOGIC;
		tx:		out STD_LOGIC;
		read:		in  STD_LOGIC;
		write:	in  STD_LOGIC;
		a: 		in  STD_LOGIC;
		d_in: 	in  STD_LOGIC_VECTOR (15 downto 0);
		d_out:	out STD_LOGIC_VECTOR (15 downto 0));
end uart;

architecture Behavioral of uart is

	component uart_rx is
	port (
		clk:			in  STD_LOGIC;
		clk_divider:in  STD_LOGIC_VECTOR (15 downto 0);
		serial_in:	in  STD_LOGIC;
		read:			in  STD_LOGIC;
		ready:		out STD_LOGIC;
		d_out:		out STD_LOGIC_VECTOR (7 downto 0));
	end component;

	component uart_tx is
	port (
		clk:			in  STD_LOGIC;
		clk_divider:in  STD_LOGIC_VECTOR (15 downto 0);
		d_in:			in  STD_LOGIC_VECTOR (7 downto 0);
		write:		in  STD_LOGIC;
		ready:		out STD_LOGIC;
		serial_out:	out STD_LOGIC);
	end component;

	signal clk_divider:	std_logic_vector(15 downto 0) := (others=>'0');
	
	signal rx_read:		std_logic;
	signal rx_ready:		std_logic;
	signal rx_d_out:		std_logic_vector(7 downto 0);
	
	signal tx_write:		std_logic;
	signal tx_ready:		std_logic;
	
begin

	uart_rx_inst: uart_rx
	port map (
		clk				=> clk,
		clk_divider		=> clk_divider,
		serial_in		=> rx,
		read				=> rx_read,
		ready				=> rx_ready,
		d_out				=> rx_d_out);

	uart_tx_inst: uart_tx
	port map (
		clk				=> clk,
		clk_divider		=> clk_divider,
		d_in				=> d_in(7 downto 0),
		write				=> tx_write,
		ready				=> tx_ready,
		serial_out		=> tx);
	
	process (clk)
	begin
		if rising_edge(clk) then
			if write='1' then
				if a='0' then
					clk_divider <= d_in;
				end if;
			end if;
		end if;
	end process;
	
	rx_read  <= read  when a='1' else '0';
	tx_write <= write when a='1' else '0';
	d_out <=	"00000000"&rx_d_out when a='1' else "00000000000000"&tx_ready&rx_ready;

end Behavioral;

