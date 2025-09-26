package Lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
         
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

        return text;
        }
        
        return object.toString();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        
        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                // River name + River name → list of names
                if (left instanceof String && right instanceof String) {
                    return List.of(left, right);
                }

                // List + String → append river name
                if (left instanceof List && right instanceof String) {
                    List<Object> merged = new ArrayList<>((List<?>) left);
                    merged.add(right);
                    return merged;
                }

                // String + List → prepend river name
                if (left instanceof String && right instanceof List) {
                    List<Object> merged = new ArrayList<>();
                    merged.add(left);
                    merged.addAll((List<?>) right);
                    return merged;
                }

                // List + List → merge river names
                if (left instanceof List && right instanceof List) {
                    List<Object> merged = new ArrayList<>((List<?>) left);
                    merged.addAll((List<?>) right);
                    return merged;
                }
                throw new RuntimeError(expr.operator, "Operands must be numbers, strings, or river names.\".");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;

            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
        }

        // Unreachable.
        return null;
    }

    //River Statements
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitRiverDeclarationStmt(Stmt.RiverDeclaration stmt) {
        System.out.println("Declare river: " + stmt.name.lexeme + " as " + stmt.type.lexeme);
        return null;
    }

    @Override
    public Void visitRiverFlowStmt(Stmt.RiverFlow stmt) {
        System.out.println("River " + stmt.from.lexeme + " flows to " + stmt.to.lexeme);
        return null;
    }

    @Override
    public Void visitRiverCombinationStmt(Stmt.RiverCombination stmt) {
        String sources = "";
        for (Token source : stmt.sources) {
            sources += source.lexeme + " ";
        }
        System.out.println("River " + stmt.name.lexeme + " combines: " + sources.trim());
        return null;
    }

    @Override
    public Void visitRainfallDeclarationStmt(Stmt.RainfallDeclaration stmt) {
        System.out.println("Rainfall set to: " + stmt.value.literal + " mm");
        return null;
    }

    @Override
    public Void visitRiverDeclarationWithFlowStmt(Stmt.RiverDeclarationWithFlow stmt) {
        System.out.println("Declare river: " + stmt.name.lexeme + " as " + stmt.type.lexeme +
            " with flow rate " + stmt.flowRate.literal + " L/s per mm");
        return null;
    }

    @Override
    public Void visitRiverCombinationExprStmt(Stmt.RiverCombinationExpr stmt) {
        Object result = evaluate(stmt.expression);
        System.out.println("River " + stmt.name.lexeme + " is combination of: " + stringify(result));
        return null;
    }
}


