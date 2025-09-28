# COMP3000 Report  
**SID:** 45865035  
**Name:** Emmanuel Almonte  

---

## Table A

| **Question** | **Answer** |
|--------------|------------|
| **What literal in your language represents a river that gets 10L/s of flow on the first day after 1mm of rainfall?** | ```rainfall = 1;``` <br> ```river test_river = root with 10;``` |
| **What symbol in your language is used to show two rivers combine?** | The `+` sign is used to combine rivers. <br> Example: <br> ```river combined1 = A + B;``` <br> ```river combined2 = A + B + C;``` |
| **Is the above symbol a "unary", "binary", or "literal"?** | A **binary operator** takes two operands: <br> - `A + B` → combines river A with river B <br> - `A + B + C` → parsed as `(A + B) + C` |
| **What folder is the "working folder" to compile your parser?** | `lox-interpreter` |
| **What command(s) will compile your parser?** | ```javac Lox/Parser.java``` – parser logic <br> ```javac Lox/Expr.java Lox/Stmt.java``` – AST definitions <br> ```javac Lox/Interpreter.java``` – runtime evaluator <br> ```javac Lox/Scanner.java Lox/Token.java Lox/TokenType.java``` – lexer <br> ```javac Lox/RuntimeError.java Lox/Lox.java``` – entry point and error handling |
| **In your language, how long does it take all the water to work through a river system after 1 day of rain?** | **Assumptions:** <br> • All rainfall enters the river system within 10 days (Assumption 6) <br> • All water that enters a river flows out (Assumption 5) <br> • Flow is instant across connections (no modeled delay) <br><br> → All water from a single day of rain works through the system within **10 days**. |
| **Does your language include statements or is it an expression language?** | My language **includes statements**. <br> Example: <br> ```rainfall = 1;``` <br> ```river googong = root with 10;``` <br> ```river molongolo = googong + jerrabombarra;``` <br><br> Each statement builds a system state. |
| **Which chapter of the book have you used as the starting point for your solution?** | Chapter 4. *Scanning* <br> Chapter 5. *Representing Code* <br> Chapter 6. *Parsing Expressions* |

---

## Example Programs

### example1.riv:
```riv
rainfall = 1;

river googong = root with 10;
river jerrabombarra = root with 5;
river molongolo = googong + jerrabombarra;
```

#### Explanation:
This program models a simple two-river merge. Both googong and jerrabombarra are root rivers that receive rainfall directly. molongolo represents their confluence, combining the two flows into a single river.

### example2.riv: 
```riv
rainfall = 1;

river yass = root with 8;
river googong = root with 10;
river jerrabombarra = root with 5;

river molongolo = googong + jerrabombarra;
river burrinjuck = molongolo + yass;
```
#### Explanation:
This example demonstrates a nested combination. molongolo is a midstream river formed by merging googong and jerrabombarra. Then, burrinjuck is created by merging molongolo with another root river, yass. This models a multi-stage river system.

### example3.riv: 
```riv
rainfall = 1;

river googong = root with 10;
river jerrabombarra = root with 5;

river lower = googong + jerrabombarra;
river upper = lower + lower;
```

#### Explanation:
This program tests symbolic reuse. lower is defined as the combination of googong and jerrabombarra. Then, upper is formed by merging lower with itself. This checks whether the parser can handle repeated identifiers correctly in combinations.

---

## How to Compile and Run
Follow these steps to compile and run programs written in the river DSL:

### Step 1 – Navigate to the working folder
```bash
cd lox-interpreter
```

### Step 2 – Compile all Java source files
```bash
javac Lox/*.java
``` 

### Step 3 – Run the interpreter with an example .riv file
```bash
java Lox.Lox examples/example1.riv
``` 

### Step 4 – Verify output
Check the console output to confirm that the parser successfully recognized and built the AST for program.

---

## How I Created My Language
This project defines a **domain-specific language (DSL)** for modeling symbolic river systems.  
The design philosophy is:

- **Declarative** – users describe the structure of the river system rather than simulate physical water flow.  
- **Composable** – rivers can be combined symbolically, allowing flexible modeling of flows.  
- **Minimalist** – only essential constructs are included: rainfall, root rivers, and combinations.  

### Key Design Choices
- **Statements over expressions** – each line represents a statement that contributes to the overall system configuration.  
- **Symbolic combination** – the `+` operator is overloaded to represent the merging of rivers rather than arithmetic addition.  
- **Global rainfall** – rainfall is declared once and applies universally to all root rivers.  
- **No control flow** – the language is purely declarative, with no branching or iterative constructs such as conditionals or loops.  

---

### Grammer Highlights
```
program        → statement* ;
statement      → rainfallDecl | riverDecl ;
rainfallDecl   → "rainfall" "=" number ";" ;
riverDecl      → "river" IDENT "=" riverExpr ";" ;
riverExpr      → "root" "with" number
               | IDENT ( "+" IDENT )* ;
```

This grammar supports root declarations, symbolic combinations, and rainfall assignment.

---

## Changes Compared to Nystrom
My parser is based on **Chapters 4–6 of *Crafting Interpreters*** but diverges in several key ways to better fit the domain of symbolic river modeling.

---

### 1. Custom Grammar
- **Nystrom:** supports arithmetic, logic, and control flow.  
- **Mine:** focuses on domain-specific constructs (`river`, `root`, `with`, `combine`, `rainfall`).  

### 2. Statement Types
- **Nystrom:** provides `print`, `var`, and expression statements.  
- **Mine:** replaces these with `rainfallDecl` and `riverDecl` to reflect river system configuration.  

### 3. Expression Semantics
- **Nystrom:** the `+` operator represents arithmetic addition.  
- **Mine:** the `+` operator is symbolic, representing the merging of river flows.  

### 4. Runtime Evaluation
- **Nystrom:** includes `Interpreter.java` and `RuntimeError.java` to evaluate code at runtime.  
- **Mine:** excludes runtime evaluation. The parser builds an AST but does not walk it to compute values.  

### 5. AST Nodes
- **Nystrom:** defines generic nodes such as `Binary`, `Literal`, and `Grouping` in `Expr.java`.  
- **Mine:** defines custom nodes like `RiverDeclaration`, `RiverCombinationExpr`, and `RootRiverExpr` to model domain-specific constructs.  

### 6. File Extension and Input
- **Nystrom:** uses `.lox` files.  
- **Mine:** uses `.riv` files stored in an `examples/` folder.  

---

## Team operation
As a team, we collaborated on the **base Lox implementation**, discussing how best to approach the assessment requirements. From this shared foundation, each member adapted and extended the standard Lox code in their own way to suit their individual language design goals.