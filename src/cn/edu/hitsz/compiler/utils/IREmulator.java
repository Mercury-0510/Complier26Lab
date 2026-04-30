package cn.edu.hitsz.compiler.utils;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用来模拟执行 IR 的类
 */
public class IREmulator {
    public static IREmulator load(List<Instruction> instructions) {
        return new IREmulator(instructions);
    }

    public Optional<Integer> execute() {
        final var labelPositions = new HashMap<String, Integer>();
        for (int i = 0; i < instructions.size(); i++) {
            final var instruction = instructions.get(i);
            if (instruction.getKind().isLabel()) {
                labelPositions.put(instruction.getLabelName(), i);
            }
        }

        int pc = 0;
        while (pc < instructions.size()) {
            final var instruction = instructions.get(pc);
            switch (instruction.getKind()) {
                case MOV -> {
                    final var from = eval(instruction.getFrom());
                    environment.put(instruction.getResult(), from);
                    pc++;
                }

                case ADD -> {
                    final var lhs = eval(instruction.getLHS());
                    final var rhs = eval(instruction.getRHS());
                    environment.put(instruction.getResult(), lhs + rhs);
                    pc++;
                }

                case SUB -> {
                    final var lhs = eval(instruction.getLHS());
                    final var rhs = eval(instruction.getRHS());
                    environment.put(instruction.getResult(), lhs - rhs);
                    pc++;
                }

                case MUL -> {
                    final var lhs = eval(instruction.getLHS());
                    final var rhs = eval(instruction.getRHS());
                    environment.put(instruction.getResult(), lhs * rhs);
                    pc++;
                }

                case RET -> {
                    this.returnValue = eval(instruction.getReturnValue());
                    pc++;
                }

                case BZ -> {
                    if (eval(instruction.getBranchCondition()) == 0) {
                        pc = getLabelPosition(labelPositions, instruction.getBranchLabel());
                    } else {
                        pc++;
                    }
                }

                case JMP -> pc = getLabelPosition(labelPositions, instruction.getBranchLabel());

                case LABEL -> pc++;
            }
        }

        return Optional.ofNullable(this.returnValue);
    }

    public Integer eval(IRValue value) {
        if (value instanceof IRImmediate immediate) {
            return immediate.getValue();
        } else if (value instanceof IRVariable variable) {
            return environment.get(variable);
        } else {
            throw new RuntimeException("Unknown IR value type");
        }
    }

    private IREmulator(List<Instruction> instructions) {
        this.instructions = instructions;
        this.environment = new HashMap<>();
        this.returnValue = null;
    }

    private int getLabelPosition(Map<String, Integer> labelPositions, String label) {
        if (!labelPositions.containsKey(label)) {
            throw new RuntimeException("Unknown label: " + label);
        }

        return labelPositions.get(label);
    }

    private final List<Instruction> instructions;
    private final Map<IRVariable, Integer> environment;
    private Integer returnValue;
}
