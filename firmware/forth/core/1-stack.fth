( stack words )

: dup
	{ ld	a,ds }
	{ ld	ds,a }
	{ ld	ds,a }
;
: drop
	{ ld	a,ds }
;
: swap
	{ ld	b,ds }
	{ ld	a,ds }
	{ ld	ds,b }
	{ ld	ds,a }
;
: over
	{ ld	b,ds }
	{ ld	a,ds }
	{ ld	ds,a }
	{ ld	ds,b }
	{ ld	ds,a }
;
: rot
	{ ld	rs,ds }
	{ ld	rs,ds }
	{ ld	a,ds }
	{ ld	ds,rs }
	{ ld	ds,rs }
	{ ld	ds,a }
;
: >r
	{ ld	a,rs }
	{ ld	rs,ds }
	{ ld	pc,a }
;
: r>
	{ ld	a,rs }
	{ ld	ds,rs }
	{ ld	pc,a }
;
: r@
	{ ld	b,rs }
	{ ld	a,rs }
	{ ld	rs,a }
	{ ld	ds,a }
	{ ld	pc,b }
;
: 2dup		over over ;
: 2drop		drop drop ;
: ?dup
	{ ldf	a,ds }
	{ jmp	eq,question-dup-false }
	{ ld	ds,a }
	{ question-dup-false: }
	{ ld	ds,a }
;


(
: depth		unimplemented ;
)