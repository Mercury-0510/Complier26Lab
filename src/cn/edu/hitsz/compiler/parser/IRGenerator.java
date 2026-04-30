package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {
    private final Deque<IRInfo> symbolStack = new ArrayDeque<>();
    private List<Instruction> ir = List.of();
    private int tempCount = 0;
    private int labelCount = 0;

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        symbolStack.push(new IRInfo(currentToken.getText(), null, List.of()));
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        final var body = popBody(production);
        final IRInfo result;

        switch (production.index()) {
            case 2, 4, 5, 6, 16, 18 -> {
                // P -> S_list | S_list -> S | S -> M/U | E -> A | A -> B
                result = body.get(0);
            }
            case 3 -> {
                // S_list -> S S_list
                result = new IRInfo("", null, concat(body.get(0).code(), body.get(1).code()));
            }
            case 7 -> {
                // M -> if ( E ) M else M
                result = buildIfElse(body.get(2), body.get(4), body.get(6));
            }
            case 8, 13 -> {
                // M -> D id Semicolon | D -> int
                result = IRInfo.empty();
            }
            case 9 -> {
                // M -> id = E Semicolon
                final var target = IRVariable.named(body.get(0).text());
                final var expression = body.get(2);
                final var code = new ArrayList<>(expression.code());
                code.add(Instruction.createMov(target, expression.value()));
                result = new IRInfo("", null, code);
            }
            case 10 -> {
                // M -> return E Semicolon
                final var expression = body.get(1);
                final var code = new ArrayList<>(expression.code());
                code.add(Instruction.createRet(expression.value()));
                result = new IRInfo("", null, code);
            }
            case 11 -> {
                // U -> if ( E ) S
                result = buildIf(body.get(2), body.get(4));
            }
            case 12 -> {
                // U -> if ( E ) M else U
                result = buildIfElse(body.get(2), body.get(4), body.get(6));
            }
            case 14 -> {
                // E -> E + A
                result = buildBinary(body.get(0), body.get(2), BinaryOperator.ADD);
            }
            case 15 -> {
                // E -> E - A
                result = buildBinary(body.get(0), body.get(2), BinaryOperator.SUB);
            }
            case 17 -> {
                // A -> A * B
                result = buildBinary(body.get(0), body.get(2), BinaryOperator.MUL);
            }
            case 19 -> {
                // B -> ( E )
                result = body.get(1);
            }
            case 20 -> {
                // B -> id
                result = new IRInfo("", IRVariable.named(body.get(0).text()), List.of());
            }
            case 21 -> {
                // B -> IntConst
                result = new IRInfo("", IRImmediate.of(Integer.parseInt(body.get(0).text())), List.of());
            }
            default -> result = body.size() == 1 ? body.get(0) : IRInfo.empty();
        }

        symbolStack.push(result);
    }


    @Override
    public void whenAccept(Status currentStatus) {
        ir = symbolStack.isEmpty() ? List.of() : List.copyOf(symbolStack.peek().code());
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // IR 生成只需要 Token 中携带的标识符名, 不需要额外访问符号表.
    }

    public List<Instruction> getIR() {
        return ir;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }

    private List<IRInfo> popBody(Production production) {
        final var body = new ArrayList<IRInfo>();
        for (int i = 0; i < production.body().size(); i++) {
            body.add(0, symbolStack.pop());
        }
        return body;
    }

    private IRInfo buildBinary(IRInfo lhs, IRInfo rhs, BinaryOperator operator) {
        final var result = newTemp();
        final var code = concat(lhs.code(), rhs.code());
        switch (operator) {
            case ADD -> code.add(Instruction.createAdd(result, lhs.value(), rhs.value()));
            case SUB -> code.add(Instruction.createSub(result, lhs.value(), rhs.value()));
            case MUL -> code.add(Instruction.createMul(result, lhs.value(), rhs.value()));
        }
        return new IRInfo("", result, code);
    }

    private IRInfo buildIf(IRInfo condition, IRInfo thenBranch) {
        final var endLabel = nextLabel();
        final var code = new ArrayList<Instruction>();
        code.addAll(condition.code());
        code.add(Instruction.createBZ(condition.value(), endLabel));
        code.addAll(thenBranch.code());
        code.add(Instruction.createLabel(endLabel));
        return new IRInfo("", null, code);
    }

    private IRInfo buildIfElse(IRInfo condition, IRInfo thenBranch, IRInfo elseBranch) {
        final var elseLabel = nextLabel();
        final var endLabel = nextLabel();
        final var code = new ArrayList<Instruction>();
        code.addAll(condition.code());
        code.add(Instruction.createBZ(condition.value(), elseLabel));
        code.addAll(thenBranch.code());
        code.add(Instruction.createJMP(endLabel));
        code.add(Instruction.createLabel(elseLabel));
        code.addAll(elseBranch.code());
        code.add(Instruction.createLabel(endLabel));
        return new IRInfo("", null, code);
    }

    private List<Instruction> concat(List<Instruction> first, List<Instruction> second) {
        final var code = new ArrayList<Instruction>(first);
        code.addAll(second);
        return code;
    }

    private IRVariable newTemp() {
        return IRVariable.named("$" + tempCount++);
    }

    private String nextLabel() {
        return "L_" + labelCount++;
    }

    private enum BinaryOperator {
        ADD, SUB, MUL
    }

    private record IRInfo(String text, IRValue value, List<Instruction> code) {
        static IRInfo empty() {
            return new IRInfo("", null, List.of());
        }
    }
}
