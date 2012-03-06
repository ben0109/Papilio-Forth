( comparison words )

: =
	{ ld	a,ds }
	{ sub	ds,a }
	{ jmp	ne,equal-false }
	{ sub	ds,#1 }
	{ ld	pc,rs }
	{ equal-false: }
	{ and	ds,#0 }
;
: <
	{ ld	a,ds }
	{ sub	a,ds }
	{ jmp	le,less-than-false }
	{ ld	ds,#0xffff }
	{ ld	pc,rs }
	{ less-than-false: }
	{ ld	ds,#0 }
;
: >
	{ ld	a,ds }
	{ sub	a,ds }
	{ jmp	ge,greater-than-false }
	{ ld	ds,#0xffff }
	{ ld	pc,rs }
	{ greater-than-false: }
	{ ld	ds,#0 }
;
: u<
	{ ld	rs,ds }
	{ ld	a,ds }
	{ sub	a,rs }
	{ jmp	pl,u-less-than-false }
	{ ld	ds,#0xffff }
	{ ld	pc,rs }
	{ u-less-than-false: }
	{ ld	ds,#0 }
;
: u>
	{ ld	a,ds }
	{ sub	a,ds }
	{ jmp	pl,u-greater-than-false }
	{ ld	ds,#0xffff }
	{ ld	pc,rs }
	{ u-greater-than-false: }
	{ ld	ds,#0 }
;

: false		0 ;
: true		0xffff ;
: 0= 		0 = ;
: 0<		0 < ;
