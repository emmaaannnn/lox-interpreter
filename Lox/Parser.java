package Lox;

import java.util.List;
import java.util.ArrayList;

import static Lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    Stmt parse() {
        try {
            return declaration();
        } catch (ParseError error) {
            return null;
        }
    }

    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    List<Stmt> parseStatements() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // Entry point for statements
    private Stmt declaration() {
        try {
            if (match(RIVER)) return riverDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        // Extend with other statement types if needed
        return new Stmt.Expression(expression());
    }

    // DSL-specific river syntax
    private Stmt riverDeclaration() {
        Token name = consume(IDENTIFIER, "Expect river name.");

        if (match(EQUAL)) {
            Token type = consume(IDENTIFIER, "Expect 'root' or 'output'.");
            consume(SEMICOLON, "Expect ';' after declaration.");
            return new Stmt.RiverDeclaration(name, type);
        }

        if (match(FLOWS_TO)) {
            Token target = consume(IDENTIFIER, "Expect target river or dam.");
            consume(SEMICOLON, "Expect ';' after flow statement.");
            return new Stmt.RiverFlow(name, target);
        }

        if (match(COMBINE)) {
            List<Token> sources = new ArrayList<>();
            do {
                sources.add(consume(IDENTIFIER, "Expect river name."));
            } while (match(COMMA));
            consume(SEMICOLON, "Expect ';' after combination.");
            return new Stmt.RiverCombination(name, sources);
        }

        throw error(peek(), "Invalid river statement.");
    }


    // Expression Parsing
    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }
        
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}