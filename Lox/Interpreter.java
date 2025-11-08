package Lox;

import static Lox.TokenType.BANG_EQUAL;
import static Lox.TokenType.EQUAL_EQUAL;
import static Lox.TokenType.GREATER;
import static Lox.TokenType.GREATER_EQUAL;
import static Lox.TokenType.LESS;
import static Lox.TokenType.LESS_EQUAL;
import static Lox.TokenType.MINUS;
import static Lox.TokenType.PLUS;
import static Lox.TokenType.SLASH;
import static Lox.TokenType.STAR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    // Global rainfall (mm). Default 1.0 unless a rainfall declaration sets it.
    private double rainfall = 1.0;

    // Representation of a river node (or dam node). We keep everything simple and in-memory.
    private static class RiverNode {
        final String name;
        Double baseFlow = null;       // declared with "root with X" (L/s per mm)
        Token typeToken = null;       // token holding 'root' or 'output' etc.
        Expr expr = null;             // symbolic expression if declared as "river X = <expr>;"
        final List<String> incoming = new ArrayList<>(); // names of upstream rivers/dams feeding this node
        RiverNode(String name) { this.name = name; }
    }

    // Simple dam model: cap (max outflow) or multiplier. If both null -> pass-through dam.
    private static class Dam {
        final String name;
        Double cap = null;
        Double multiplier = null;
        Dam(String name) { this.name = name; }
        double apply(double flow) {
            double v = flow;
            if (multiplier != null) v = v * multiplier;
            if (cap != null && v > cap) v = cap;
            return v;
        }
    }

    // All declared rivers/dams
    private final Map<String, RiverNode> nodes = new HashMap<>();
    private final Map<String, Dam> dams = new HashMap<>();

    // Memoization while computing flows
    private final Map<String, Double> memo = new HashMap<>();
    private final Set<String> computing = new HashSet<>(); // detect cycles

    void interpret(List<Stmt> statements) {
        // First pass: collect declarations and build graph (incoming lists)
        for (Stmt s : statements) {
            if (s == null) continue;
            s.accept(this); // visitor methods will fill nodes, dams, rainfall
        }

        // Second pass: evaluate flows for every named river (nodes keys)
        memo.clear();
        computing.clear();

        // Ensure deterministic output order by iterating keys in insertion order-ish:
        System.out.println("Final flows (L/s) assuming rainfall = " + rainfall + " mm:");
        for (String name : new ArrayList<>(nodes.keySet())) {
            try {
                double flow = computeOutflow(name);
                System.out.printf("%s = %.4f%n", name, flow);
            } catch (RuntimeError e) {
                System.err.println("Runtime error computing " + name + ": " + e.getMessage());
            }
        }

        // Also print dams if present
        if (!dams.isEmpty()) {
            System.out.println("\nDams:");
            for (Dam d : dams.values()) {
                System.out.print(d.name + " -> ");
                StringBuilder sb = new StringBuilder();
                if (d.multiplier != null) sb.append("multiplier=").append(d.multiplier).append(" ");
                if (d.cap != null) sb.append("cap=").append(d.cap).append(" ");
                if (sb.length() == 0) sb.append("pass-through");
                System.out.println(sb.toString());
            }
        }
    }

    // Compute outflow for a node (river or dam) recursively.
    private double computeOutflow(String name) {
        if (memo.containsKey(name)) return memo.get(name);
        if (!nodes.containsKey(name) && !dams.containsKey(name)) {
            // Unknown identifier: treat as 0 but warn
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, name, null, 0),
                    "Undefined river/dam: " + name);
        }

        if (computing.contains(name)) {
            throw new RuntimeError(new Token(TokenType.IDENTIFIER, name, null, 0),
                    "Cycle detected involving " + name);
        }
        computing.add(name);

        double result;
        if (dams.containsKey(name) && !nodes.containsKey(name)) {
            // Dam without an associated river node: dam's inflow comes from incoming list if set.
            RiverNode dn = nodes.get(name); // maybe null
            double inflow = 0.0;
            if (dn != null) {
                for (String src : dn.incoming) inflow += computeOutflow(src);
            }
            result = dams.get(name).apply(inflow);
        } else {
            RiverNode n = nodes.get(name);
            // Priority rules:
            // - If an explicit expression was given (river X = <expr>), evaluate it and use that.
            // - Otherwise, flow = baseFlow * rainfall (if root) + sum(incoming flows).
            if (n.expr != null) {
                Object val = evaluateExpr(n.expr);
                if (!(val instanceof Double)) {
                    throw new RuntimeError(new Token(TokenType.IDENTIFIER, name, null, 0),
                            "Expression for " + name + " did not evaluate to a number.");
                }
                result = (Double) val;
            } else {
                double sum = 0.0;
                if (n.baseFlow != null) sum += n.baseFlow * rainfall;
                for (String src : n.incoming) {
                    sum += computeOutflow(src);
                }
                result = sum;
            }

            // If a dam exists with the same name, apply it (treat dam as modifier attached to the node)
            if (dams.containsKey(name)) {
                result = dams.get(name).apply(result);
            }
        }

        memo.put(name, result);
        computing.remove(name);
        return result;
    }

    // Evaluate arithmetic/variable expressions. Variables resolve to river/dam outflows.
    private Object evaluateExpr(Expr expr) {
        return expr.accept(this);
    }

    // Expr.Visitor implementations
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value instanceof Double ? expr.value : ((Number)expr.value).doubleValue();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluateExpr(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluateExpr(expr.right);
        if (expr.operator.type == TokenType.MINUS) {
            return -((Double) right);
        } else if (expr.operator.type == TokenType.BANG) {
            return isTruthy(right) ? 0.0 : 1.0;
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluateExpr(expr.left);
        Object right = evaluateExpr(expr.right);

        // arithmetic only for now
        double a = ((Number) left).doubleValue();
        double b = ((Number) right).doubleValue();

        switch (expr.operator.type) {
            case PLUS: return a + b;
            case MINUS: return a - b;
            case STAR: return a * b;
            case SLASH: return a / b;
            case GREATER: return a > b ? 1.0 : 0.0;
            case GREATER_EQUAL: return a >= b ? 1.0 : 0.0;
            case LESS: return a < b ? 1.0 : 0.0;
            case LESS_EQUAL: return a <= b ? 1.0 : 0.0;
            case EQUAL_EQUAL: return a == b ? 1.0 : 0.0;
            case BANG_EQUAL: return a != b ? 1.0 : 0.0;
            default:
                throw new RuntimeError(expr.operator, "Unsupported binary operator in evaluation.");
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        String name = expr.name.lexeme;
        // Variables refer to river/dam names -> compute their outflow
        return computeOutflow(name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        // assignment is not used heavily in river domain; support by evaluating value and storing as a var in environment
        Object value = evaluateExpr(expr.value);
        // We keep assignments out of the river map, they are regular variables in a simple environment.
        // For now throw: assignment of rivers is not supported.
        throw new RuntimeError(expr.name, "Assignment is not supported for river nodes in this interpreter.");
    }

    // Helper truthiness
    private boolean isTruthy(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof Double) return ((Double) obj) != 0.0;
        return true;
    }

    // Stmt.Visitor implementations - these build the graph and/or perform side effects.
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        // Evaluate expression but do not print by default.
        evaluateExpr(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluateExpr(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // Simple variables (not rivers). Evaluate initializer and store as a node with baseFlow = value
        if (stmt.initializer != null) {
            Object val = evaluateExpr(stmt.initializer);
            double d = ((Number) val).doubleValue();
            RiverNode n = getOrCreateNode(stmt.name.lexeme);
            n.baseFlow = d; // treat variable as a small river node for convenience
        }
        return null;
    }

    @Override
    public Void visitRainfallDeclarationStmt(Stmt.RainfallDeclaration stmt) {
        // Token value contains a NUMBER literal
        Object lit = stmt.value.literal;
        if (lit instanceof Number) {
            rainfall = ((Number) lit).doubleValue();
        } else {
            try {
                rainfall = Double.parseDouble(stmt.value.lexeme);
            } catch (NumberFormatException e) {
                throw new RuntimeError(stmt.value, "Invalid rainfall value.");
            }
        }
        return null;
    }

    @Override
    public Void visitRiverDeclarationStmt(Stmt.RiverDeclaration stmt) {
        RiverNode n = getOrCreateNode(stmt.name.lexeme);
        n.typeToken = stmt.type;
        return null;
    }

    @Override
    public Void visitRiverDeclarationWithFlowStmt(Stmt.RiverDeclarationWithFlow stmt) {
        RiverNode n = getOrCreateNode(stmt.name.lexeme);
        n.typeToken = stmt.type;
        // flowRate token is a NUMBER token
        Object lit = stmt.flowRate.literal;
        if (lit instanceof Number) {
            n.baseFlow = ((Number) lit).doubleValue();
        } else {
            try {
                n.baseFlow = Double.parseDouble(stmt.flowRate.lexeme);
            } catch (NumberFormatException e) {
                throw new RuntimeError(stmt.flowRate, "Invalid flow rate.");
            }
        }
        return null;
    }

    @Override
    public Void visitRiverCombinationExprStmt(Stmt.RiverCombinationExpr stmt) {
        RiverNode n = getOrCreateNode(stmt.name.lexeme);
        n.expr = stmt.expression;
        return null;
    }

    @Override
    public Void visitRiverCombinationStmt(Stmt.RiverCombination stmt) {
        RiverNode n = getOrCreateNode(stmt.name.lexeme);
        for (Token t : stmt.sources) {
            String src = t.lexeme;
            n.incoming.add(src);
            // ensure source node exists
            getOrCreateNode(src);
        }
        return null;
    }

    @Override
    public Void visitRiverFlowStmt(Stmt.RiverFlow stmt) {
        String from = stmt.name.lexeme;
        String to = stmt.target.lexeme;

        // record nodes
        RiverNode srcNode = getOrCreateNode(from);
        RiverNode dstNode = getOrCreateNode(to);
        // incoming edge for destination
        dstNode.incoming.add(from);
        return null;
    }

    // Helpers
    private RiverNode getOrCreateNode(String name) {
        RiverNode n = nodes.get(name);
        if (n == null) {
            n = new RiverNode(name);
            nodes.put(name, n);
        }
        return n;
    }

    private String stringify(Object obj) {
        if (obj == null) return "nil";
        if (obj instanceof Double) {
            double d = (Double) obj;
            if (d == (long) d) return String.format("%d", (long) d);
            return Double.toString(d);
        }
        return obj.toString();
    }

    @Override
    public Void visitDamDeclarationStmt(Stmt.DamDeclaration stmt) {
        // Implement the logic for dam declaration
        Dam dam = new Dam(stmt.name.lexeme);
        if (stmt.cap != null) {
            dam.cap = stmt.cap.value; // Assuming cap is a number
        }
        if (stmt.multiplier != null) {
            dam.multiplier = stmt.multiplier.value; // Assuming multiplier is a number
        }
        dams.put(dam.name, dam);
        return null;
    }
}