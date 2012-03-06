library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
use IEEE.NUMERIC_STD.ALL;

entity main is
	port (
		clk_in:	in		std_logic;
		rx:		in		std_logic;
		tx:		out	std_logic;
		wing:		inout	std_logic_vector(15 downto 0));
end main;

architecture Behavioral of main is

	component clock is
   port (
		clk_in:		in    std_logic;
		clk:			out   std_logic;
		clk180:		out   std_logic);
	end component;

	component cpu is
	port (
		clk:			in  std_logic;
		read:			out std_logic;
		write:		out std_logic;
		a:				out std_logic_vector (14 downto 0);
		d_in:			out std_logic_vector (15 downto 0);
		d_out:		in  std_logic_vector (15 downto 0));
	end component;

	component ram is
	port (
		clk:			in  std_logic;
		we:			in  std_logic;
		a:				in  std_logic_vector (11 downto 0);
		d_in:			in  std_logic_vector (15 downto 0);
		d_out:		out std_logic_vector (15 downto 0));
	end component;
	
	component uart is
	port (
		clk:		in  STD_LOGIC;
		rx:		in  STD_LOGIC;
		tx:		out STD_LOGIC;
		read:		in  STD_LOGIC;
		write:	in  STD_LOGIC;
		a: 		in  STD_LOGIC;
		d_in: 	in  STD_LOGIC_VECTOR (15 downto 0);
		d_out:	out STD_LOGIC_VECTOR (15 downto 0));
	end component;

	component io is
	port (
		clk:			in		std_logic;
		io:			inout	std_logic_vector (15 downto 0);
		write:		in		std_logic;
		a:				in		std_logic;
		d_in:			in		std_logic_vector (15 downto 0);
		d_out:		out	std_logic_vector (15 downto 0));
	end component;
	
	signal clk:				std_logic;
	signal clk180:			std_logic;
	
	signal read:			std_logic;
	signal write:			std_logic;
	signal a:				std_logic_vector (14 downto 0);
	signal d_in:			std_logic_vector (15 downto 0);
	signal d_out:			std_logic_vector (15 downto 0);
	
	signal ram_write:		std_logic;
	signal ram_d_out:		std_logic_vector (15 downto 0);
	
	signal uart_read:		std_logic;
	signal uart_write:	std_logic;
	signal uart_d_out:	std_logic_vector (15 downto 0);
	
	signal io_write:		std_logic;
	signal io_d_out:		std_logic_vector (15 downto 0);

begin

	clock_inst: clock
	port map (
		clk_in	=> clk_in,
		clk		=> clk,
		clk180	=> clk180);

	cpu_inst: cpu
	port map (
		clk		=> clk180,
		read		=> read,
		write		=> write,
		a			=> a,
		d_in		=> d_in,
		d_out		=> d_out);

	ram_inst: ram
	port map (
		clk		=> clk,
		a			=> a(11 downto 0),
		we			=> ram_write,
		d_in		=> d_in,
		d_out		=> ram_d_out
	);

	io_inst: io
	port map (
		clk		=> clk,
		io			=> wing,
		a			=> a(0),
		write		=> io_write,
		d_in		=> d_in,
		d_out		=> io_d_out
	);
	
	uart_inst: uart
	port map (
		clk		=> clk,
		rx			=> rx,
		tx			=> tx,
		read		=> uart_read,
		write		=> uart_write,
		a			=> a(0),
		d_in		=> d_in,
		d_out		=> uart_d_out);

	uart_read	<= read  when a(14 downto 12)="111" else '0';
	
	ram_write	<= write when a(14 downto 12)="000" else '0';
	io_write		<= write when a(14 downto 12)="100" else '0';
	uart_write	<= write when a(14 downto 12)="111" else '0';
	
	with a(14 downto 12) select
	d_out <=	ram_d_out	when "000",
				io_d_out		when "100",
				uart_d_out	when "111",
				(others=>'0') when others;
	
end Behavioral;

