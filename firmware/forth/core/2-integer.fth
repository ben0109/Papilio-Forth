( integer words )

: s>d
	{ ldf	ds,ds }
	{ ld	ds,#0 }
	{ jmp	pl,s-to-d-end }
	{ not	ds,ds }
	{ s-to-d-end: }
;

: and			{ and	ds,ds } ;	
: or			{ or	ds,ds } ;	
: xor			{ xor	ds,ds } ;
: invert		{ not	ds,ds } ;
: +				{ add	ds,ds } ;
: -				{ ld	a,ds } { sub	ds,a } ;
: 2*			{ lsl	ds,ds } ;
: 2/			{ asr	ds,ds } ;
: 1+			{ add	ds,#1 } ;
: 2+			{ add	ds,#2 } ;
: 1-			{ sub	ds,#1 } ;
: 2-			{ sub	ds,#2 } ;


: *
	{ ld	a,ds }
	{ ld	b,ds }
	{ ld	ds,#0 }
	{ star-loop: }
	{ lsr	b,b }
	{ jmp	lo,star-finish-loop }	( lo=cc )
	{ add	ds,a }
	{ star-finish-loop: }
	{ lsl	a,a }
	{ ldf	b,b }
	{ jmp	ne,star-loop }
	{ star-end: }
;
(
: m*			{ call xt-mstar } ;

: sm/rem		{ call xt-s-m-div-rem } ;
: /mod			>r s>d r> sm/rem ;
: /				/mod swap drop ;
: mod			/mod drop ;
: */mod			>r m* r> sm/rem ;
: */			*/mod swap drop ;
)

(
: max			2dup < if swap then drop ;
: min			2dup > if swap then drop ;
: d-			{ call xt-d-minus } ;
)

: abs
	{ ldf	ds,ds }
	{ jmp	pl,abs-end }
	{ not	ds,ds }
	{ add	ds,#1 }
	{ abs-end: }
;
: lshift
	{ ldf	a,ds }
	{ jmp	eq,lshift-end }
	{ lshift-loop: }
	{ lsl	ds,ds }
	{ sub	a,#1 }
	{ jmp	ne,lshift-loop }
	{ lshift-end: }
;
: rshift
	{ ldf	a,ds }
	{ jmp	eq,lshift-end }
	{ lshift-loop: }
	{ lsr	ds,ds }
	{ sub	a,#1 }
	{ jmp	ne,lshift-loop }
	{ lshift-end: }
;
: negate
	{ not	ds,ds }
	{ add	ds,#1 }
;
(
: um*			unimplemented ;
: fm/mod		unimplemented ;
: um/mod		unimplemented ;
)