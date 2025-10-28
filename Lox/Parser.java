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
    
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    // Entry point for statements
    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            if (match(RAINFALL)) return rainfallDeclaration();
            if (match(RIVER)) return riverDeclaration();
            
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        
        if (match(EQUAL)) {
            initializer = expression();
        }
        
        consume(SEMICOLON, "Expect ';' after variable declaration.");

        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = equality();
        
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    // DSL-specific river syntax
    private Stmt riverDeclaration() {
        Token name = consume(IDENTIFIER, "Expect river name.");

        if (match(EQUAL)) {
            // Peek ahead to decide if it's a symbolic combination or a type declaration
            if (check(IDENTIFIER) && (checkNext(WITH) || checkNext(SEMICOLON))) {
                Token type = consume(IDENTIFIER, "Expect 'root' or 'output'.");
                if (match(WITH)) {
                    Token flowRate = consume(NUMBER, "Expect flow rate in L/s.");
                    consume(SEMICOLON, "Expect ';' after declaration.");
                    return new Stmt.RiverDeclarationWithFlow(name, type, flowRate);
                }
                consume(SEMICOLON, "Expect ';' after declaration.");
                return new Stmt.RiverDeclaration(name, type);
            }

            // Otherwise, treat it as a symbolic combination expression
            Expr expr = expression();
            consume(SEMICOLON, "Expect ';' after river combination.");
            return new Stmt.RiverCombinationExpr(name, expr);
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

    private Stmt rainfallDeclaration() {
        consume(EQUAL, "Expect '=' after 'rainfall'.");
        Token value = consume(NUMBER, "Expect rainfall value in mm.");
        consume(SEMICOLON, "Expect ';' after rainfall declaration.");
        return new Stmt.RainfallDeclaration(value);
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    // Expression Parsing
    private Expr expression() {
        return assignment();
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
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

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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