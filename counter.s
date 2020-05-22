main:
	addi x2 x0 1
    slli x2 x2 7
	addi x1 x0 0
    sw x1, 0(x2)
    addi x3 x0 255
loop:
	lw x1 0(x2)
	addi x1 x1 1
    sw x1, 0(x2)
    blt x1 x3 loop
