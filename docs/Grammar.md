# River Language Grammer
## Statements

### Rainfall Declaration
Declare rainfall in millimeters:
```river
rainfall = 120.5;
```

### River Declaration
Declare a river with a type (`root` or `output`):
```river
river Amazon = root;
river Nile = output;
```

Declare a river with a flow rate:
```river
river Amazon = root with 500.0;
```

### River Flow
Specify that a river flows to another river or dam:
```river
river Amazon flows Nile;
river Nile flows dam1;
```

### River Combination
Combine multiple rivers into one:
```river
river Delta combine Amazon, Nile, Yangtze;
```

### River Combination Expression
Assign a symbolic combination expression to a river:
```river
river Delta = Amazon + Nile * 2;
```

### Expression Statement
General expressions (arithmetic, grouping, literals):
```river
(1 + 2) * 3;
```

## Expressions

- Literals: numbers, strings, `true`, `false`, `nil`
- Arithmetic: `+`, `-`, `*`, `/`
- Comparison: `>`, `>=`, `<`, `<=`
- Equality: `==`, `!=`
- Grouping: `( ... )`
- Unary: `-`, `!`

