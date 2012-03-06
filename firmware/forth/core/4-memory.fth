( memory words )

: @
	{ ld	a,ds }
	{ ld	ds,(a) }
;
: !
	{ ld	a,ds }
	{ ld	(a),ds }
;

: c@
	{ ld	a,ds }
	{ ld	b,a }
	{ and	b,#1 }
	{ jmp	ne,c-fetch-odd }
	{ ld	ds,(a) }
	{ and	ds,#0xff }
	{ ld	pc,rs }
	{ c-fetch-odd: }
	{ and	a,#0xfffe }
	{ ld	ds,(a) }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
	{ lsr	ds,ds }
;
: c!
	{ ld	a,ds }
	{ ld	b,a }
	{ and	a,#0xfffe }
	{ ld	ds,(a) }
	{ and	b,#1 }
	{ ld	b,ds }
	{ jmp	ne,c-store-odd }
	{ and	b,#0xff00 }
	{ jmp	al,c-store-end }
	{ c-store-odd: }
	{ and	b,#0x00ff }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ lsl	ds,ds }
	{ c-store-end: }
	{ or	b,ds }
	{ ld	(a),b }
;

: char+			1+ ;
: cell+			2+ ;
: cells			2* ;
: chars			;
: +! 			dup >r @ + r> ! ;
: 1+!			dup @ 1+ swap ! ;
: 2! 			swap over ! cell+ ! ;
: 2@ 			dup cell+ @ swap @ ;

: here			$here @ ;
: allot
	here + 
	$here !
;
: , 			here ! 2 allot ;
: c, 			here c! 1 allot ;

: aligned		1+ 0xfffe and ;
: align			here aligned $here ! ;







(
: fill			swap 0 do 2dup c! swap 1+ swap loop ;
: move			0 do 2dup swap c@ swap c! 1+ swap 1+ swap loop 2drop ;
)
