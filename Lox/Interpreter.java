package Lox;

import static Lox.TokenType.BANG;
import static Lox.TokenType.BANG_EQUAL;
import static Lox.TokenType.EQUAL_EQUAL;
import static Lox.TokenType.GREATER;
import static Lox.TokenType.LESS;
import static Lox.TokenType.MINUS;
import static Lox.TokenType.PLUS;
import static Lox.TokenType.SLASH;
import static Lox.TokenType.STAR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Map<String, Object> environment = new HashMap<>();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            System.err.println("[Runtime Error] " + error.getMessage());
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    // Expression evaluation
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS: return -(double) right;
            case BANG: return !isTruthy(right);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                return left.toString() + right.toString();
            case MINUS: return (double) left - (double) right;
            case STAR: return (double) left * (double) right;
            case SLASH: return (double) left / (double) right;
            case EQUAL_EQUAL: return isEqual(left, right);
            case BANG_EQUAL: return !isEqual(left, right);
            case GREATER: return (double) left > (double) right;
            case LESS: return (double) left < (double) right;
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        if (environment.containsKey(expr.name.lexeme)) {
            return environment.get(expr.name.lexeme);
        }
        throw new RuntimeError(expr.name, "Undefined variable '" + expr.name.lexeme + "'.");
    }

    // River Statement execution
    @Override
    public Void visitRainfallDeclarationStmt(Stmt.RainfallDeclaration stmt) {
        environment.put("rainfall", stmt.value.literal);
        System.out.println("Rainfall set to " + stmt.value.literal + " mm");
        return null;
    }

    @Override
    public Void visitRiverDeclarationStmt(Stmt.RiverDeclaration stmt) {
        environment.put(stmt.name.lexeme, stmt.type.lexeme);
        System.out.println("River " + stmt.name.lexeme + " declared as " + stmt.type.lexeme);
        return null;
    }

    @Override
    public Void visitRiverDeclarationWithFlowStmt(Stmt.RiverDeclarationWithFlow stmt) {
        environment.put(stmt.name.lexeme, stmt.flowRate.literal);
        System.out.println("River " + stmt.name.lexeme + " declared with flow " + stmt.flowRate.literal + " L/s");
        return null;
    }

    @Override
    public Void visitRiverFlowStmt(Stmt.RiverFlow stmt) {
        System.out.println("River " + stmt.from.lexeme + " flows to " + stmt.to.lexeme);
        return null;
    }

    @Override
    public Void visitRiverCombinationStmt(Stmt.RiverCombination stmt) {
        List<String> sources = stmt.sources.stream().map(t -> t.lexeme).toList();
        System.out.println("River " + stmt.name.lexeme + " combines: " + String.join(", ", sources));
        return null;
    }

    @Override
    public Void visitRiverCombinationExprStmt(Stmt.RiverCombinationExpr stmt) {
        Object result = evaluate(stmt.expression);
        environment.put(stmt.name.lexeme, result);
        System.out.println("River " + stmt.name.lexeme + " set to combination result: " + result);
        return null;
    }

    // Statement execution
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println("= " + value);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.put(stmt.name.lexeme, value);
        return null;
    }

    // Utility methods
    private boolean isTruthy(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (boolean) obj;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}
