library IEEE;
use IEEE.STD_LOGIC_1164.ALL;
library UNISIM;
use UNISIM.VComponents.all;

entity ram is
	Port (clk:		in  std_logic;
			we:		in  std_logic;
			a:			in  std_logic_vector (11 downto 0);
			d_in:		in  std_logic_vector (15 downto 0);
			d_out:	out std_logic_vector (15 downto 0));
end ram;

architecture Behavioral of ram is

begin

--	RAMB_inst0 : RAMB16_S18
--	generic map (
--		INIT_00 => X"0000000000000000000000000020FE010031A1C1FFFE41C10D04A1C1FFFC41C1",
--		INIT_01 => X"000000000000000000000000000000000000000000000000000000000020FE01"
--	)
--	port map (
--		CLK	=> clk,
--		EN		=> '1',
--		SSR	=> '0',
--		WE		=> we,
--		ADDR	=> a(9 downto 0),
--		DI		=> d_in,
--		DO		=> d_out
--		,DIP	=> "00"
--	);

	RAMB_inst0 : RAMB16_S9
	port map (
		CLK	=> clk,
		EN		=> '1',
		SSR	=> '0',
		WE		=> we,
		ADDR	=> a(10 downto 0),
		DI		=> d_in(7 downto 0),
		DO		=> d_out(7 downto 0),
		DIP	=> "0"
	);

	RAMB_inst1 : RAMB16_S9
	port map (
		CLK	=> clk,
		EN		=> '1',
		SSR	=> '0',
		WE		=> we,
		ADDR	=> a(10 downto 0),
		DI		=> d_in(15 downto 8),
		DO		=> d_out(15 downto 8),
		DIP	=> "0"
	);

--	RAMB_inst0 : RAMB16_S4
--	port map (
--		CLK	=> clk,
--		EN		=> '1',
--		SSR	=> '0',
--		WE		=> we,
--		ADDR	=> a,
--		DI		=> d_in(3 downto 0),
--		DO		=> d_out(3 downto 0)
--	);

--	RAMB_inst1 : RAMB16_S4
--	port map (
--		CLK	=> clk,
--		EN		=> '1',
--		SSR	=> '0',
--		WE		=> we,
--		ADDR	=> a,
--		DI		=> d_in(7 downto 4),
--		DO		=> d_out(7 downto 4)
--	);

--	RAMB_inst2 : RAMB16_S4
--	port map (
--		CLK	=> clk,
--		EN		=> '1',
--		SSR	=> '0',
--		WE		=> we,
--		ADDR	=> a,
--		DI		=> d_in(11 downto 8),
--		DO		=> d_out(11 downto 8)
--	);

--	RAMB_inst3 : RAMB16_S4
--	port map (
--		CLK	=> clk,
--		EN		=> '1',
--		SSR	=> '0',
--		WE		=> we,
--		ADDR	=> a,
--		DI		=> d_in(15 downto 12),
--		DO		=> d_out(15 downto 12)
--	);

end Behavioral;

