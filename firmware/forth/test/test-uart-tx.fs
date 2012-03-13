: dup dup ;;
: ! ! ;;
: or or ;;
: 1- 1- ;;
: 2/ 1 rshift ;;

: here $here literal @ ;
: __main
	0x1a08 0x4000 !
	[ here 2/ ] 0x4000 @ 1 and
	0 [ 0x2000 or here 1- 1- ! ]
	0x4001 @ 0x4001 !
	0 [ here 2/ 1- here 1- 1- ! ]
;