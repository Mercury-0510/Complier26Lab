package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {
    private SymbolTable symbolTable;
    private final Deque<SymbolInfo> symbolStack = new ArrayDeque<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // do nothing
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        final var body = popBody(production);
        final SymbolInfo result;

        switch (production.index()) {
            case 8 -> {
                // M -> D id Semicolon
                final var type = body.get(0).type();
                final var name = body.get(1).text();
                symbolTable.get(name).setType(type);
                result = SymbolInfo.empty();
            }
            case 9 -> {
                // M -> id = E Semicolon
                ensureDeclared(body.get(0).text());
                result = SymbolInfo.empty();
            }
            case 10 -> {
                // M -> return E Semicolon
                result = SymbolInfo.empty();
            }
            case 13 -> {
                // D -> int
                result = new SymbolInfo("", SourceCodeType.Int);
            }
            case 14, 15, 16 -> {
                // E -> E + A | E - A | A
                result = new SymbolInfo("", SourceCodeType.Int);
            }
            case 17, 18 -> {
                // A -> A * B | B
                result = new SymbolInfo("", SourceCodeType.Int);
            }
            case 19, 21 -> {
                // B -> ( E ) | IntConst
                result = new SymbolInfo("", SourceCodeType.Int);
            }
            case 20 -> {
                // B -> id
                ensureDeclared(body.get(0).text());
                result = new SymbolInfo("", SourceCodeType.Int);
            }
            default -> result = SymbolInfo.empty();
        }

        symbolStack.push(result);
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        symbolStack.push(new SymbolInfo(currentToken.getText(), null));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
    }

    private List<SymbolInfo> popBody(Production production) {
        final var body = new ArrayList<SymbolInfo>();
        for (int i = 0; i < production.body().size(); i++) {
            body.add(0, symbolStack.pop());
        }
        return body;
    }

    private void ensureDeclared(String name) {
        if (symbolTable.get(name).getType() == null) {
            throw new RuntimeException("Variable used before declaration: " + name);
        }
    }

    private record SymbolInfo(String text, SourceCodeType type) {
        static SymbolInfo empty() {
            return new SymbolInfo("", null);
        }
    }
}
