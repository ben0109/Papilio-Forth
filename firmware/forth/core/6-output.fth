( output words )

: bl			0x20 ;
: __emit		begin 0xfffc @ 2 and until 0xfffe ! ;
: emit			dup  __emit 10 = if 13 __emit then ;
: cr			10 emit ;
: space			bl emit ;
: spaces		0 do space loop ;
: count			dup char+ swap c@ ;
: type			{ xt-type: } 0 do dup c@ emit char+ loop drop ;

: decimal 		10 $base ! ;
: hex	 		16 $base ! ;
: base 			$base @ ;
decimal

(
: hold			unimplemented ;
: sign			unimplemented ;
: <#			unimplemented ;
: #				unimplemented ;
: #>			unimplemented ;
: #s			unimplemented ;
: .				unimplemented ;
: u.			unimplemented ;
: ."			unimplemented ;
)

: __print_digit dup 10 < if 0x30 else 0x57 then + emit ;
: __print_word
	4 0 do dup 0xf and swap 4 rshift loop drop
	4 0 do __print_digit loop
;
: __dump 0 do dup @ __print_word 2+ loop drop cr ;
