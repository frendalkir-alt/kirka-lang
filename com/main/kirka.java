package com.main;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

// ==================== ТОКЕНЫ ====================
enum TokenType {
    NUMBER, STRING, IDENTIFIER, TRUE, FALSE,
    ASSIGN, EQUALS, NOT_EQUALS, LESS, GREATER, LESS_EQ, GREATER_EQ,
    PLUS, MINUS, MULTIPLY, DIVIDE,
    AND, OR, NOT,
    LPAREN, RPAREN, LBRACE, RBRACE, COMMA,
    IF, ELSE, WHILE, FUNCTION, RETURN, PRINT, INPUT,   // добавили INPUT
    EOF
}

class Token {
    TokenType type;
    String value;
    int line;
    Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }
    @Override
    public String toString() {
        return type + "(" + value + ")@" + line;
    }
}

// ==================== ЛЕКСЕР ====================
class Lexer {
    private final String source;
    private int pos = 0;
    private char currentChar;
    private int line = 1;

    Lexer(String source) {
        this.source = source;
        currentChar = source.length() > 0 ? source.charAt(0) : '\0';
    }

    private void advance() {
        pos++;
        if (pos < source.length()) {
            currentChar = source.charAt(pos);
            if (currentChar == '\n') line++;
        } else {
            currentChar = '\0';
        }
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(currentChar)) advance();
    }

    private void skipComment() {
        if (currentChar == '/' && peek() == '/') {
            while (currentChar != '\n' && currentChar != '\0') advance();
        }
    }

    private char peek() {
        if (pos + 1 < source.length()) return source.charAt(pos + 1);
        return '\0';
    }

    private Token number() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(currentChar)) {
            sb.append(currentChar);
            advance();
        }
        return new Token(TokenType.NUMBER, sb.toString(), line);
    }

    private Token string() {
        advance();
        StringBuilder sb = new StringBuilder();
        while (currentChar != '"' && currentChar != '\0') {
            if (currentChar == '\\') {
                advance();
                if (currentChar == 'n') sb.append('\n');
                else if (currentChar == 't') sb.append('\t');
                else sb.append(currentChar);
            } else {
                sb.append(currentChar);
            }
            advance();
        }
        if (currentChar == '"') advance();
        return new Token(TokenType.STRING, sb.toString(), line);
    }

    private Token identifierOrKeyword() {
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
            sb.append(currentChar);
            advance();
        }
        String word = sb.toString();
        switch (word) {
            case "true": return new Token(TokenType.TRUE, word, line);
            case "false": return new Token(TokenType.FALSE, word, line);
            case "if": return new Token(TokenType.IF, word, line);
            case "else": return new Token(TokenType.ELSE, word, line);
            case "while": return new Token(TokenType.WHILE, word, line);
            case "function": return new Token(TokenType.FUNCTION, word, line);
            case "return": return new Token(TokenType.RETURN, word, line);
            case "print": return new Token(TokenType.PRINT, word, line);
            case "input": return new Token(TokenType.INPUT, word, line); // новое
            default: return new Token(TokenType.IDENTIFIER, word, line);
        }
    }

    Token nextToken() {
        while (true) {
            skipWhitespace();
            if (currentChar == '\0') return new Token(TokenType.EOF, "", line);
            if (currentChar == '/' && peek() == '/') {
                skipComment();
                continue;
            }
            if (Character.isDigit(currentChar)) return number();
            if (currentChar == '"') return string();
            if (Character.isLetter(currentChar) || currentChar == '_') return identifierOrKeyword();

            if (currentChar == '=') {
                if (peek() == '=') { advance(); advance(); return new Token(TokenType.EQUALS, "==", line); }
                else { advance(); return new Token(TokenType.ASSIGN, "=", line); }
            }
            if (currentChar == '!' && peek() == '=') { advance(); advance(); return new Token(TokenType.NOT_EQUALS, "!=", line); }
            if (currentChar == '<') {
                if (peek() == '=') { advance(); advance(); return new Token(TokenType.LESS_EQ, "<=", line); }
                else { advance(); return new Token(TokenType.LESS, "<", line); }
            }
            if (currentChar == '>') {
                if (peek() == '=') { advance(); advance(); return new Token(TokenType.GREATER_EQ, ">=", line); }
                else { advance(); return new Token(TokenType.GREATER, ">", line); }
            }
            if (currentChar == '&' && peek() == '&') { advance(); advance(); return new Token(TokenType.AND, "&&", line); }
            if (currentChar == '|' && peek() == '|') { advance(); advance(); return new Token(TokenType.OR, "||", line); }
            if (currentChar == '!') { advance(); return new Token(TokenType.NOT, "!", line); }
            if (currentChar == '+') { advance(); return new Token(TokenType.PLUS, "+", line); }
            if (currentChar == '-') { advance(); return new Token(TokenType.MINUS, "-", line); }
            if (currentChar == '*') { advance(); return new Token(TokenType.MULTIPLY, "*", line); }
            if (currentChar == '/') { advance(); return new Token(TokenType.DIVIDE, "/", line); }
            if (currentChar == '(') { advance(); return new Token(TokenType.LPAREN, "(", line); }
            if (currentChar == ')') { advance(); return new Token(TokenType.RPAREN, ")", line); }
            if (currentChar == '{') { advance(); return new Token(TokenType.LBRACE, "{", line); }
            if (currentChar == '}') { advance(); return new Token(TokenType.RBRACE, "}", line); }
            if (currentChar == ',') { advance(); return new Token(TokenType.COMMA, ",", line); }

            throw new RuntimeException("Unexpected character '" + currentChar + "' at line " + line);
        }
    }
}

// ==================== АБСТРАКТНОЕ СИНТАКСИЧЕСКОЕ ДЕРЕВО ====================
interface AstNode {}
interface ExprNode extends AstNode {}

class NumberNode implements ExprNode { int value; NumberNode(int v) { value = v; } }
class StringNode implements ExprNode { String value; StringNode(String v) { value = v; } }
class BooleanNode implements ExprNode { boolean value; BooleanNode(boolean v) { value = v; } }
class VariableNode implements ExprNode { String name; VariableNode(String n) { name = n; } }

class BinOpNode implements ExprNode {
    TokenType operator;
    ExprNode left, right;
    BinOpNode(TokenType op, ExprNode l, ExprNode r) { operator = op; left = l; right = r; }
}

class UnaryNode implements ExprNode {
    TokenType operator;
    ExprNode operand;
    UnaryNode(TokenType op, ExprNode o) { operator = op; operand = o; }
}

class CallNode implements ExprNode {
    String name;
    List<ExprNode> arguments;
    CallNode(String n, List<ExprNode> args) { name = n; arguments = args; }
}

// Новый узел для input
class InputNode implements ExprNode {
    ExprNode prompt; // может быть null
    InputNode(ExprNode p) { prompt = p; }
}

interface StmtNode extends AstNode {}

class PrintNode implements StmtNode {
    List<ExprNode> expressions;
    PrintNode(List<ExprNode> exprs) { expressions = exprs; }
}

class AssignNode implements StmtNode {
    String variable;
    ExprNode expression;
    AssignNode(String v, ExprNode e) { variable = v; expression = e; }
}

class IfNode implements StmtNode {
    ExprNode condition;
    List<StmtNode> thenBlock;
    List<StmtNode> elseBlock;
    IfNode(ExprNode c, List<StmtNode> t, List<StmtNode> e) { condition = c; thenBlock = t; elseBlock = e; }
}

class WhileNode implements StmtNode {
    ExprNode condition;
    List<StmtNode> body;
    WhileNode(ExprNode c, List<StmtNode> b) { condition = c; body = b; }
}

class ReturnNode implements StmtNode {
    ExprNode expression;
    ReturnNode(ExprNode e) { expression = e; }
}

class FunctionDeclNode implements StmtNode {
    String name;
    List<String> parameters;
    List<StmtNode> body;
    FunctionDeclNode(String n, List<String> params, List<StmtNode> b) {
        name = n; parameters = params; body = b;
    }
}

class BlockNode implements StmtNode {
    List<StmtNode> statements;
    BlockNode(List<StmtNode> stmts) { statements = stmts; }
}

class ExprStmtNode implements StmtNode {
    ExprNode expr;
    ExprStmtNode(ExprNode e) { expr = e; }
}

// ==================== ПАРСЕР ====================
class Parser {
    private final Lexer lexer;
    private Token currentToken;

    Parser(Lexer lexer) {
        this.lexer = lexer;
        currentToken = lexer.nextToken();
    }

    private void eat(TokenType type) {
        if (currentToken.type == type) {
            currentToken = lexer.nextToken();
        } else {
            throw new RuntimeException("Unexpected token " + currentToken.value +
                    " at line " + currentToken.line + ", expected " + type);
        }
    }

    // ===== ВЫРАЖЕНИЯ =====
    private ExprNode expression() { return logicalOr(); }

    private ExprNode logicalOr() {
        ExprNode node = logicalAnd();
        while (currentToken.type == TokenType.OR) {
            Token op = currentToken;
            eat(TokenType.OR);
            node = new BinOpNode(op.type, node, logicalAnd());
        }
        return node;
    }

    private ExprNode logicalAnd() {
        ExprNode node = equality();
        while (currentToken.type == TokenType.AND) {
            Token op = currentToken;
            eat(TokenType.AND);
            node = new BinOpNode(op.type, node, equality());
        }
        return node;
    }

    private ExprNode equality() {
        ExprNode node = comparison();
        while (currentToken.type == TokenType.EQUALS || currentToken.type == TokenType.NOT_EQUALS) {
            Token op = currentToken;
            eat(op.type);
            node = new BinOpNode(op.type, node, comparison());
        }
        return node;
    }

    private ExprNode comparison() {
        ExprNode node = additive();
        while (currentToken.type == TokenType.LESS || currentToken.type == TokenType.GREATER ||
               currentToken.type == TokenType.LESS_EQ || currentToken.type == TokenType.GREATER_EQ) {
            Token op = currentToken;
            eat(op.type);
            node = new BinOpNode(op.type, node, additive());
        }
        return node;
    }

    private ExprNode additive() {
        ExprNode node = multiplicative();
        while (currentToken.type == TokenType.PLUS || currentToken.type == TokenType.MINUS) {
            Token op = currentToken;
            eat(op.type);
            node = new BinOpNode(op.type, node, multiplicative());
        }
        return node;
    }

    private ExprNode multiplicative() {
        ExprNode node = unary();
        while (currentToken.type == TokenType.MULTIPLY || currentToken.type == TokenType.DIVIDE) {
            Token op = currentToken;
            eat(op.type);
            node = new BinOpNode(op.type, node, unary());
        }
        return node;
    }

    private ExprNode unary() {
        if (currentToken.type == TokenType.NOT || currentToken.type == TokenType.MINUS) {
            Token op = currentToken;
            eat(op.type);
            ExprNode operand = unary();
            return new UnaryNode(op.type, operand);
        }
        return primary();
    }

    private ExprNode primary() {
        if (currentToken.type == TokenType.NUMBER) {
            int val = Integer.parseInt(currentToken.value);
            eat(TokenType.NUMBER);
            return new NumberNode(val);
        } else if (currentToken.type == TokenType.STRING) {
            String val = currentToken.value;
            eat(TokenType.STRING);
            return new StringNode(val);
        } else if (currentToken.type == TokenType.TRUE) {
            eat(TokenType.TRUE);
            return new BooleanNode(true);
        } else if (currentToken.type == TokenType.FALSE) {
            eat(TokenType.FALSE);
            return new BooleanNode(false);
        } else if (currentToken.type == TokenType.IDENTIFIER) {
            String name = currentToken.value;
            eat(TokenType.IDENTIFIER);
            if (currentToken.type == TokenType.LPAREN) {
                return parseCall(name);
            }
            return new VariableNode(name);
        } else if (currentToken.type == TokenType.INPUT) {
            eat(TokenType.INPUT);
            eat(TokenType.LPAREN);
            ExprNode prompt = null;
            if (currentToken.type != TokenType.RPAREN) {
                prompt = expression();
            }
            eat(TokenType.RPAREN);
            return new InputNode(prompt);
        } else if (currentToken.type == TokenType.LPAREN) {
            eat(TokenType.LPAREN);
            ExprNode node = expression();
            eat(TokenType.RPAREN);
            return node;
        } else {
            throw new RuntimeException("Unexpected token in primary: " + currentToken + " at line " + currentToken.line);
        }
    }

    private CallNode parseCall(String name) {
        eat(TokenType.LPAREN);
        List<ExprNode> args = new ArrayList<>();
        if (currentToken.type != TokenType.RPAREN) {
            args.add(expression());
            while (currentToken.type == TokenType.COMMA) {
                eat(TokenType.COMMA);
                args.add(expression());
            }
        }
        eat(TokenType.RPAREN);
        return new CallNode(name, args);
    }

    // ===== ИНСТРУКЦИИ =====
    private StmtNode statement() {
        if (currentToken.type == TokenType.FUNCTION) {
            return functionDecl();
        } else if (currentToken.type == TokenType.IF) {
            return ifStmt();
        } else if (currentToken.type == TokenType.WHILE) {
            return whileStmt();
        } else if (currentToken.type == TokenType.RETURN) {
            return returnStmt();
        } else if (currentToken.type == TokenType.PRINT) {
            return printStmt();
        } else if (currentToken.type == TokenType.LBRACE) {
            return new BlockNode(parseBlock());
        } else if (currentToken.type == TokenType.IDENTIFIER) {
            String name = currentToken.value;
            eat(TokenType.IDENTIFIER);
            if (currentToken.type == TokenType.ASSIGN) {
                eat(TokenType.ASSIGN);
                ExprNode expr = expression();
                return new AssignNode(name, expr);
            } else if (currentToken.type == TokenType.LPAREN) {
                CallNode call = parseCall(name);
                return new ExprStmtNode(call);
            } else {
                throw new RuntimeException("Expected '=' or '(' after identifier at line " + currentToken.line);
            }
        } else {
            throw new RuntimeException("Unexpected token at start of statement: " + currentToken + " line " + currentToken.line);
        }
    }

    private FunctionDeclNode functionDecl() {
        eat(TokenType.FUNCTION);
        String name = currentToken.value;
        eat(TokenType.IDENTIFIER);
        eat(TokenType.LPAREN);
        List<String> params = new ArrayList<>();
        if (currentToken.type != TokenType.RPAREN) {
            params.add(currentToken.value);
            eat(TokenType.IDENTIFIER);
            while (currentToken.type == TokenType.COMMA) {
                eat(TokenType.COMMA);
                params.add(currentToken.value);
                eat(TokenType.IDENTIFIER);
            }
        }
        eat(TokenType.RPAREN);
        eat(TokenType.LBRACE);
        List<StmtNode> body = new ArrayList<>();
        while (currentToken.type != TokenType.RBRACE && currentToken.type != TokenType.EOF) {
            body.add(statement());
        }
        eat(TokenType.RBRACE);
        return new FunctionDeclNode(name, params, body);
    }

    private IfNode ifStmt() {
        eat(TokenType.IF);
        eat(TokenType.LPAREN);
        ExprNode cond = expression();
        eat(TokenType.RPAREN);
        List<StmtNode> thenBlock = parseBlock();
        List<StmtNode> elseBlock = null;
        if (currentToken.type == TokenType.ELSE) {
            eat(TokenType.ELSE);
            elseBlock = parseBlock();
        }
        return new IfNode(cond, thenBlock, elseBlock);
    }

    private WhileNode whileStmt() {
        eat(TokenType.WHILE);
        eat(TokenType.LPAREN);
        ExprNode cond = expression();
        eat(TokenType.RPAREN);
        List<StmtNode> body = parseBlock();
        return new WhileNode(cond, body);
    }

    private ReturnNode returnStmt() {
        eat(TokenType.RETURN);
        ExprNode expr = null;
        if (currentToken.type != TokenType.RBRACE && currentToken.type != TokenType.EOF) {
            expr = expression();
        }
        return new ReturnNode(expr);
    }

    private PrintNode printStmt() {
        eat(TokenType.PRINT);
        List<ExprNode> exprs = new ArrayList<>();
        exprs.add(expression());
        while (currentToken.type == TokenType.COMMA) {
            eat(TokenType.COMMA);
            exprs.add(expression());
        }
        return new PrintNode(exprs);
    }

    private List<StmtNode> parseBlock() {
        eat(TokenType.LBRACE);
        List<StmtNode> stmts = new ArrayList<>();
        while (currentToken.type != TokenType.RBRACE && currentToken.type != TokenType.EOF) {
            stmts.add(statement());
        }
        eat(TokenType.RBRACE);
        return stmts;
    }

    public BlockNode parseProgram() {
        List<StmtNode> stmts = new ArrayList<>();
        while (currentToken.type != TokenType.EOF) {
            stmts.add(statement());
        }
        return new BlockNode(stmts);
    }
}

// ==================== ИНТЕРПРЕТАТОР ====================
class Interpreter {
    private final Map<String, Object> globals = new HashMap<>();
    private final Map<String, FunctionDeclNode> functions = new HashMap<>();
    private final Stack<Map<String, Object>> callStack = new Stack<>();
    private boolean returnFlag = false;
    private Object returnValue = null;
    private Map<String, Object> currentScope;
    private final Scanner scanner = new Scanner(System.in); // для ввода

    Interpreter() {
        currentScope = globals;
    }

    private void pushScope() {
        callStack.push(currentScope);
        currentScope = new HashMap<>();
    }

    private void popScope() {
        currentScope = callStack.pop();
    }

    private Object getVariable(String name) {
        if (currentScope.containsKey(name)) return currentScope.get(name);
        if (globals.containsKey(name)) return globals.get(name);
        throw new RuntimeException("Undefined variable: " + name);
    }

    private void setVariable(String name, Object value) {
        if (currentScope.containsKey(name)) {
            currentScope.put(name, value);
        } else if (globals.containsKey(name)) {
            globals.put(name, value);
        } else {
            currentScope.put(name, value);
        }
    }

    private Object evaluate(ExprNode node) {
        if (node instanceof NumberNode) {
            return ((NumberNode) node).value;
        } else if (node instanceof StringNode) {
            return ((StringNode) node).value;
        } else if (node instanceof BooleanNode) {
            return ((BooleanNode) node).value;
        } else if (node instanceof VariableNode) {
            return getVariable(((VariableNode) node).name);
        } else if (node instanceof InputNode) {
            InputNode in = (InputNode) node;
            if (in.prompt != null) {
                System.out.print(evaluate(in.prompt).toString());
            }
            return scanner.nextLine();
        } else if (node instanceof BinOpNode) {
            BinOpNode bin = (BinOpNode) node;
            Object left = evaluate(bin.left);
            Object right = evaluate(bin.right);
            if (bin.operator == TokenType.PLUS) {
                if (left instanceof Integer && right instanceof Integer)
                    return (Integer) left + (Integer) right;
                else
                    return left.toString() + right.toString();
            } else if (bin.operator == TokenType.MINUS) {
                checkNumbers(left, right); return (Integer) left - (Integer) right;
            } else if (bin.operator == TokenType.MULTIPLY) {
                checkNumbers(left, right); return (Integer) left * (Integer) right;
            } else if (bin.operator == TokenType.DIVIDE) {
                checkNumbers(left, right); return (Integer) left / (Integer) right;
            } else if (bin.operator == TokenType.EQUALS) {
                return left.equals(right);
            } else if (bin.operator == TokenType.NOT_EQUALS) {
                return !left.equals(right);
            } else if (bin.operator == TokenType.LESS) {
                compareNumbers(left, right); return (Integer) left < (Integer) right;
            } else if (bin.operator == TokenType.GREATER) {
                compareNumbers(left, right); return (Integer) left > (Integer) right;
            } else if (bin.operator == TokenType.LESS_EQ) {
                compareNumbers(left, right); return (Integer) left <= (Integer) right;
            } else if (bin.operator == TokenType.GREATER_EQ) {
                compareNumbers(left, right); return (Integer) left >= (Integer) right;
            } else if (bin.operator == TokenType.AND) {
                return (Boolean) left && (Boolean) right;
            } else if (bin.operator == TokenType.OR) {
                return (Boolean) left || (Boolean) right;
            } else {
                throw new RuntimeException("Unknown binary operator: " + bin.operator);
            }
        } else if (node instanceof UnaryNode) {
            UnaryNode un = (UnaryNode) node;
            Object val = evaluate(un.operand);
            if (un.operator == TokenType.NOT) return !(Boolean) val;
            else if (un.operator == TokenType.MINUS) {
                if (val instanceof Integer) return -(Integer) val;
                else throw new RuntimeException("Unary '-' only for numbers");
            } else {
                throw new RuntimeException("Unknown unary operator: " + un.operator);
            }
        } else if (node instanceof CallNode) {
            CallNode call = (CallNode) node;
            // Встроенная функция toNumber
            if (call.name.equals("toNumber")) {
                if (call.arguments.size() != 1)
                    throw new RuntimeException("toNumber expects exactly one argument");
                Object arg = evaluate(call.arguments.get(0));
                if (arg instanceof Integer) return arg;
                if (arg instanceof String) {
                    try { return Integer.parseInt((String) arg); }
                    catch (NumberFormatException e) { throw new RuntimeException("Can't convert to number: " + arg); }
                }
                throw new RuntimeException("toNumber expects string or number");
            }
            // Пользовательские функции
            if (!functions.containsKey(call.name))
                throw new RuntimeException("Undefined function: " + call.name);
            FunctionDeclNode func = functions.get(call.name);
            if (func.parameters.size() != call.arguments.size())
                throw new RuntimeException("Argument count mismatch for " + call.name);
            List<Object> argValues = new ArrayList<>();
            for (ExprNode arg : call.arguments) argValues.add(evaluate(arg));
            pushScope();
            for (int i = 0; i < func.parameters.size(); i++)
                currentScope.put(func.parameters.get(i), argValues.get(i));
            returnFlag = false;
            returnValue = null;
            try {
                for (StmtNode stmt : func.body) {
                    execute(stmt);
                    if (returnFlag) break;
                }
            } finally {
                popScope();
            }
            return returnValue;
        } else {
            throw new RuntimeException("Unknown expression node: " + node.getClass());
        }
    }

    private void checkNumbers(Object a, Object b) {
        if (!(a instanceof Integer) || !(b instanceof Integer))
            throw new RuntimeException("Arithmetic requires numbers");
    }

    private void compareNumbers(Object a, Object b) {
        if (!(a instanceof Integer) || !(b instanceof Integer))
            throw new RuntimeException("Comparison requires numbers");
    }

    private void execute(StmtNode stmt) {
        if (stmt instanceof BlockNode) {
            BlockNode block = (BlockNode) stmt;
            for (StmtNode s : block.statements) {
                execute(s);
                if (returnFlag) break;
            }
        } else if (stmt instanceof AssignNode) {
            AssignNode assign = (AssignNode) stmt;
            setVariable(assign.variable, evaluate(assign.expression));
        } else if (stmt instanceof PrintNode) {
            PrintNode print = (PrintNode) stmt;
            List<String> parts = new ArrayList<>();
            for (ExprNode expr : print.expressions)
                parts.add(evaluate(expr).toString());
            System.out.println(String.join(" ", parts));
        } else if (stmt instanceof IfNode) {
            IfNode ifNode = (IfNode) stmt;
            if ((Boolean) evaluate(ifNode.condition)) {
                for (StmtNode s : ifNode.thenBlock) { execute(s); if (returnFlag) break; }
            } else if (ifNode.elseBlock != null) {
                for (StmtNode s : ifNode.elseBlock) { execute(s); if (returnFlag) break; }
            }
        } else if (stmt instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) stmt;
            while ((Boolean) evaluate(whileNode.condition)) {
                for (StmtNode s : whileNode.body) { execute(s); if (returnFlag) break; }
                if (returnFlag) break;
            }
        } else if (stmt instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) stmt;
            returnFlag = true;
            returnValue = (ret.expression != null) ? evaluate(ret.expression) : null;
        } else if (stmt instanceof FunctionDeclNode) {
            functions.put(((FunctionDeclNode) stmt).name, (FunctionDeclNode) stmt);
        } else if (stmt instanceof ExprStmtNode) {
            evaluate(((ExprStmtNode) stmt).expr);
        } else {
            throw new RuntimeException("Unknown statement: " + stmt.getClass());
        }
    }

    public void run(BlockNode program) {
        for (StmtNode stmt : program.statements) {
            execute(stmt);
            if (returnFlag) break;
        }
    }
}

// ==================== ТОЧКА ВХОДА ====================
public class kirka {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java kirka <source-file>");
            System.exit(1);
        }
        try {
            String source = new String(Files.readAllBytes(Paths.get(args[0])));
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer);
            BlockNode program = parser.parseProgram();
            Interpreter interpreter = new Interpreter();
            interpreter.run(program);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Runtime error: " + e.getMessage());
            System.exit(1);
        }
    }
}