package Lox;

import java.util.List;

abstract class Stmt {
    interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);
        R visitRiverDeclarationStmt(RiverDeclaration stmt);
        R visitRiverFlowStmt(RiverFlow stmt);
        R visitRiverCombinationStmt(RiverCombination stmt);
        R visitRainfallDeclarationStmt(RainfallDeclaration stmt);
        R visitRiverDeclarationWithFlowStmt(RiverDeclarationWithFlow stmt);
        R visitRiverCombinationExprStmt(RiverCombinationExpr stmt);
    }

    static class Expression extends Stmt {
        final Expr expression;

        Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    static class RiverCombinationExpr extends Stmt {
        final Token name;
        final Expr expression;

        RiverCombinationExpr(Token name, Expr expression) {
            this.name = name;
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRiverCombinationExprStmt(this);
        }
    }

    static class RiverDeclaration extends Stmt {
        final Token name;
        final Token type; // 'root' or 'output'

        RiverDeclaration(Token name, Token type) {
            this.name = name;
            this.type = type;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRiverDeclarationStmt(this);
        }
    }

    static class RiverFlow extends Stmt {
        final Token from;
        final Token to;

        RiverFlow(Token from, Token to) {
            this.from = from;
            this.to = to;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRiverFlowStmt(this);
        }
    }

    static class RiverCombination extends Stmt {
        final Token name;
        final List<Token> sources;

        RiverCombination(Token name, List<Token> sources) {
            this.name = name;
            this.sources = sources;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRiverCombinationStmt(this);
        }
    }

    static class RainfallDeclaration extends Stmt {
        final Token value;

        RainfallDeclaration(Token value) {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRainfallDeclarationStmt(this);
        }
    }

    static class RiverDeclarationWithFlow extends Stmt {
        final Token name;
        final Token type; // 'root'
        final Token flowRate; // number token

        RiverDeclarationWithFlow(Token name, Token type, Token flowRate) {
            this.name = name;
            this.type = type;
            this.flowRate = flowRate;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRiverDeclarationWithFlowStmt(this);
        }
    }

    abstract <R> R accept(Visitor<R> visitor);
}