( hard-coded words of the compiler, so you can actually call them )
( note: ;; is 'quick return', put in the previous instruction )
: +         + ;;
: xor       xor ;;
: and       and ;;
: or        or ;;
: invert    invert ;;
: =         = ;;
: <         < ;;
: u<        u< ;;
: swap      swap ;;
: dup       dup ;;
: drop     	drop ;;
: over      over ;;
: nip       nip ;
: @         @ ;;
: !         ! ;;
: dsp       dsp ;;
: lshift    lshift ;;
: rshift    rshift ;;
: 1-        1- ;;
: 2-		1- 1- ;;

: >r        r> swap >r >r ;
: r>        r> r> swap >r ;
: r@        r> r@ swap >r ;
: exit      r-drop ;




( some basic words )

: rot		>r swap r> swap ;
: 2dup		over over ;
: 2drop		drop drop ;

: negate	1- invert ;
: -			negate + ;
: 2*		1 lshift ;
: 2/		1 rshift ;
: 1+		1 + ;
: 2+		2 + ;

: >		swap < ;
: u>	swap u< ;

: false		0 ;
: true		0xffff ;
: 0= 		0 = ;
: 0<		0 < ;

(
: ?dup		unimplemented ;
: depth		unimplemented ;
)










( memory words )

: char+			1+ ;
: cell+			2+ ;
: cells			2* ;
: chars			;
: +! 			dup >r @ + r> ! ;
: 1+!			dup @ 1+ swap ! ;
: 2! 			swap over ! cell+ ! ;
: 2@ 			dup cell+ @ swap @ ;

: here			$here literal @ ;
: allot			here + $here literal ! ;
: , 			here ! 2 allot ;

: aligned		1+ 0xfffe and ;
: align			here aligned $here literal ! ;





( assembler words )

: ubranch	    	      , ;
: 0branch		0x2000 or , ;
: scall			0x4000 or , ;
: alu			0x6000 or , ;

( control words )

: if
	here					( put marker on stack )
	0 0branch				( prepare jump, unknown address )
; immediate

: then
	dup @ here 2/ or swap !	( patch jump for 'if' )
; immediate

: else
	dup @ here 2/ 1+ or swap !	( patch 'if' jump )
	here						( put marker on stack )
	0 ubranch					( prepare jump, unknown address )
; immediate


: literal
	dup 0x8000 and if
		invert 0x8000 or , ' invert literal postpone scall
	else
		0x8000 or ,
	then
; immediate


: TOK_START			0 ;
: TOK_LEAVE			1 ;

: __finish_loop
	here 2/ >r						( address after loop address )
	[ here 0 0branch ]				( "begin while" test marker: 0=start token )
		dup @ r@ or swap !			( patch jump => to here )
	[								( "repeat" )
	dup 2/ ubranch					( 	goto start )
	dup @ here 2/ or swap !			( 	patch while )
	]
	r> drop
	2/ here 2- @ or here 2- !		( patch jump before call  )
;

: begin
	here TOK_START					( put begin token on stack )
; immediate

: while
	here TOK_LEAVE					( put leave token on stack )
	0 0branch						( cond jump -- to end )
; immediate

: until
	0 0branch						( cond jump -- to start )
	__finish_loop
; immediate

: repeat
	0 ubranch						( jump -- to start )
	__finish_loop
; immediate


: xt-do			r> rot rot >r >r >r ;

: xt-loop
	r>			( ret )
	r>			( limit )
	r> 1+		( i )
	2dup - if
		>r >r >r 0 exit
	then
	2drop >r 1
;

: do
	[ ' xt-do ] literal postpone scall		( call xt-do )
	here TOK_START							( put do token on stack )
; immediate

: loop
	[ ' xt-loop ] literal postpone scall	( call xt-loop )
	0 0branch								( loop -- to start )
	__finish_loop
; immediate

: unloop		r> r> r> 2drop >r ;

: leave
	postpone unloop							( call unloop )
	here TOK_LEAVE							( put leave token on stack ) 
	0 ubranch								( reserve space )
; immediate

: i r> r> r@ rot rot >r >r ;
: j r> r> i >r >r ;






( more memory words )

: c@
	dup 1 and if
		@ 8 rshift
	else
		@ 0xff and
	then
;
: c!
	>r
	r@ 1 and if
		8 lshift
		r@ @ 0x00ff
	else
		r@ @ 0xff00
	then
	and or r> !
;
: c, 	here c! 1 allot ;

: fill	swap 0 do 2dup c! swap 1+ swap loop ;
: move	0 do 2dup swap c@ swap c! 1+ swap 1+ swap loop 2drop ;




( integer words )

: *
	>r 0 swap
	begin
		r@ while
		r@ 1 and if
			swap over + swap
		then
		2* r> 2/ >r
	repeat
	r> 2drop
;
: s>d	dup 0 < ;

(
: m*			{ call xt-mstar } ;
: um*			unimplemented ;
: fm/mod		unimplemented ;
: um/mod		unimplemented ;

: sm/rem		{ call xt-s-m-div-rem } ;
: /mod			>r s>d r> sm/rem ;
: /				/mod swap drop ;
: mod			/mod drop ;
: */mod			>r m* r> sm/rem ;
: */			*/mod swap drop ;
: d-			{ call xt-d-minus } ;
)

: max			2dup < if swap then drop ;
: min			2dup > if swap then drop ;

: abs		dup 0 < if negate then ;








( output words )

: bl			0x20 ;
: __emit		begin 0x4000 @ 2 and until 0x4001 ! ;
: emit			dup  __emit 10 = if 13 __emit then ;
: cr			10 emit ;
: space			bl emit ;
: spaces		0 do space loop ;
: count			dup char+ swap c@ ;
: type			0 do dup c@ emit char+ loop drop ;

: decimal 		10 $base literal ! ;
: hex	 		16 $base literal ! ;
: base 			$base literal @ ;

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
	dup 12 rshift 15 and __print_digit
	dup  8 rshift 15 and __print_digit
	dup  4 rshift 15 and __print_digit
	              15 and __print_digit
(
	4 0 do dup 0xf and swap 4 rshift loop drop
	4 0 do __print_digit loop
)
;
