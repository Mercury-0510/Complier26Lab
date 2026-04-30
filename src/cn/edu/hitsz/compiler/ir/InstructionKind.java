package cn.edu.hitsz.compiler.ir;

/**
 * IR 的种类
 */
public enum InstructionKind {
    ADD, SUB, MUL, MOV, RET, BZ, JMP, LABEL;

    /**
     * @return IR 是否是二元的 (有返回值, 有两个参数)
     */
    public boolean isBinary() {
        return this == ADD || this == SUB || this == MUL;
    }

    /**
     * @return IR 是否是一元的 (有返回值, 有一个参数)
     */
    public boolean isUnary() {
        return this == MOV;
    }

    /**
     * @return IR 是否为 RET 指令
     */
    public boolean isReturn() {
        return this == RET;
    }

    /**
     * @return IR 是否为条件跳转指令
     */
    public boolean isBranch() {
        return this == BZ;
    }

    /**
     * @return IR 是否为无条件跳转指令
     */
    public boolean isJump() {
        return this == JMP;
    }

    /**
     * @return IR 是否为标号指令
     */
    public boolean isLabel() {
        return this == LABEL;
    }
}
