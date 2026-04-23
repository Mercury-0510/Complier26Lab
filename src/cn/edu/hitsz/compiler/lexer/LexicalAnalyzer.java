package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;
    private String sourceCode = "";
    private List<Token> tokens = List.of();

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }


    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        sourceCode = FileUtils.readFile(path);
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        final var result = new ArrayList<Token>();
        final var text = sourceCode;
        int i = 0;

        while (i < text.length()) {
            final var ch = text.charAt(i);
            // 跳过空白符
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }

            // 识别单词
            if (Character.isLetter(ch) || ch == '_') {
                final int start = i;
                i++;
                while (i < text.length()) {
                    final var c = text.charAt(i);
                    if (Character.isLetterOrDigit(c) || c == '_') {
                        i++;
                    } else {
                        break;
                    }
                }
                
                // 识别关键字或加入id
                final var lexeme = text.substring(start, i);
                if (TokenKind.isAllowed(lexeme)) {
                    result.add(Token.simple(lexeme));
                } else {
                    if (!symbolTable.has(lexeme)) {
                        symbolTable.add(lexeme);
                    }
                    result.add(Token.normal("id", lexeme));
                }
                continue;
            }

            // 识别数字
            if (Character.isDigit(ch)) {
                final int start = i;
                i++;
                while (i < text.length() && Character.isDigit(text.charAt(i))) {
                    i++;
                }

                result.add(Token.normal("IntConst", text.substring(start, i)));
                continue;
            }

            // 识别符号
            final var punct = ch == ';' ? "Semicolon" : String.valueOf(ch);
            if (TokenKind.isAllowed(punct)) {
                result.add(Token.simple(punct));
                i++;
                continue;
            }

            throw new RuntimeException("Unrecognized character: '" + ch + "'");
        }

        result.add(Token.eof());
        tokens = result;
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
