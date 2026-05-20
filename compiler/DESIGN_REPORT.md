# Design Report
## Spreadsheet Formula Language — Compiler Front-End

**Course:** Compiler Design  
**Language:** Java 21  
**Technique:** Handwritten Scanner + Recursive-Descent Parser  

---

## 1. Language Description

The **Spreadsheet Formula Language (SFL)** is a simple expression language
inspired by Microsoft Excel and LibreOffice Calc.  A formula is any expression
prefixed with the equals sign (`=`) and may include:

- Integer literals (`5`, `100`)
- Cell references (`A1`, `ZZ99`)
- Cell ranges (`A1:A10`)
- Arithmetic expressions with `+`, `-`, `*`, `/`
- Grouping with parentheses
- Function calls (`SUM`, `MAX`, `MIN`, `IF`, and any identifier)
- Nested function calls

The language is **not** Turing-complete — it has no variables, no loops, and no
assignment.  Its purpose is to describe *what* to compute, not *how*.

---

## 2. Grammar Specification

The grammar is written in a form that is directly amenable to recursive-descent
parsing (no left recursion, no ambiguity):

```
Formula      →  '=' Expression EOF

Expression   →  Term ( ('+' | '-') Term )*

Term         →  Factor ( ('*' | '/') Factor )*

Factor       →  '-' Factor
             |  '(' Expression ')'
             |  CELL_REFERENCE (':' CELL_REFERENCE)?
             |  NUMBER
             |  IDENTIFIER '(' ArgumentList? ')'

ArgumentList →  Expression (',' Expression)*
```

### Left-Recursion Elimination

The original left-recursive rule:

```
Expression → Expression '+' Term | Term     ← left-recursive, cannot parse directly
```

is transformed into the iterative form:

```
Expression → Term ( '+' Term )*             ← right-factored, LL(1) compatible
```

This transformation preserves left-associativity (the loop builds nodes
left-to-right) while making the grammar suitable for recursive descent.

### Precedence Derivation

Precedence is a consequence of grammar depth:

```
Expression  (handles + and -)         ← lowest
  └── Term  (handles * and /)         ← medium
        └── Factor  (atoms, unary)    ← highest
```

An expression like `2 + 3 * 4` is parsed as:

```
Expression
└── BinaryExpr [+]
    ├── Number [2]        ← left Term
    └── BinaryExpr [*]   ← right Term (deeper = higher precedence)
        ├── Number [3]
        └── Number [4]
```

---

## 3. Token Specification

The scanner produces the following token types:

| Token | Regular Pattern | Notes |
|-------|-----------------|-------|
| `EQUALS` | `=` | Formula start sigil |
| `NUMBER` | `[0-9]+` | Integer, stored as lexeme |
| `CELL_REFERENCE` | `[A-Za-z]+[0-9]+` | Letters immediately followed by digits |
| `IDENTIFIER` | `[A-Za-z]+` | No trailing digits — function names |
| `PLUS` | `+` | |
| `MINUS` | `-` | Also used as unary negation |
| `STAR` | `*` | |
| `SLASH` | `/` | |
| `LPAREN` | `(` | |
| `RPAREN` | `)` | |
| `COMMA` | `,` | |
| `COLON` | `:` | Range separator |
| `EOF` | (end of input) | Sentinel |
| `ILLEGAL` | (anything else) | Unrecognised character |

### Token vs Lexeme Distinction

A **token type** is an abstract category (`NUMBER`).  
A **lexeme** is the concrete string matched from the source (`"42"`).

Two tokens may share the same type but have different lexemes.  The parser uses
token types for decisions; later phases use lexemes to recover values.

---

## 4. Scanner Design

### Algorithm

The scanner (`Lexer.java`) is a hand-coded finite-state automaton.  It maintains
a single integer `pos` (the read head) and dispatches on `source.charAt(pos)`:

```
while not at end:
    skip whitespace
    c ← source[pos]
    switch c:
        single-char tokens → emit immediately
        digit              → scanNumber()
        letter             → scanIdentifierOrCellRef()
        other              → emit ILLEGAL
emit EOF
```

### Cell Reference Disambiguation

After consuming a run of letters, the scanner peeks at the next character:
- **Digit immediately follows** → classify as `CELL_REFERENCE`
- **Otherwise** → classify as `IDENTIFIER`

This is a **context-sensitive lexical rule** handled at scan time, keeping the
parser's grammar context-free.

### Error Handling in the Scanner

Unrecognised characters (e.g. `@`, `#`, `$`) emit an `ILLEGAL` token rather
than throwing.  This allows the parser to produce a contextual error message
that includes the position and the surrounding tokens.

---

## 5. Parser Design

### Recursive Descent

Each non-terminal in the grammar corresponds to exactly one private method:

| Non-terminal | Parser Method |
|---|---|
| `Formula` | `parseFormula()` |
| `Expression` | `parseExpression()` |
| `Term` | `parseTerm()` |
| `Factor` | `parseFactor()` |
| `ArgumentList` | `parseArgumentList()` |

### Lookahead

The parser uses **one token of lookahead** (`current()`).  All grammar
decisions — whether to enter the `+/-` loop, whether to parse a range, whether
to call `parseArgumentList` — are made based on the type of the current token
alone.  This makes the grammar LL(1).

### How the Scanner and Parser Communicate

The scanner and parser are **decoupled by a data structure**:

1. `Lexer.tokenize()` is called once; it runs to completion and returns an
   immutable `List<Token>`.
2. `Parser` is constructed with that list.
3. The parser maintains an integer cursor and uses `advance()` / `current()` to
   traverse the list.

The scanner never calls the parser; the parser never calls the scanner.

---

## 6. AST Design

### Node Hierarchy

```
ASTNode (abstract)
├── FormulaNode          — root of every formula
├── NumberNode           — integer literal leaf
├── CellReferenceNode    — cell reference leaf  (col + row decomposition)
├── BinaryExpressionNode — binary operator node (left, right, operator char)
├── UnaryExpressionNode  — unary operator node  (operator char, operand)
├── FunctionCallNode     — function call (name, List<ASTNode> args)
├── RangeNode            — cell range (start CellRefNode, end CellRefNode)
└── ParenthesizedNode    — explicit grouping (inner node)
```

### AST vs Token Stream

| Aspect | Token Stream | AST |
|--------|-------------|-----|
| Shape | Flat list | Hierarchical tree |
| Contains | Every token including `(`, `)`, `,` | Logical structure only |
| Precedence | Implicit in order | Explicit in tree depth |
| Use | Parser input | Semantic analysis / code generation |

### Why Separate Node Classes?

Using a distinct Java class for each grammar construct enables:
- **Type-safe visitor patterns** in later phases
- **Clean `instanceof` checks** for semantic analysis
- **Node-specific fields** (e.g. `CellReferenceNode.column` and `.row`)
- **Independent serialisation** per node type

---

## 7. Error Handling Strategy

### Two Error Categories

1. **Lexical error** — the scanner emits `ILLEGAL` for unrecognised characters.
   The parser detects this in `parseFactor()` and throws `ParseException`.

2. **Syntax error** — the parser's `consume(expected, msg)` method throws
   `ParseException` whenever the current token does not match the expected type.

### ParseException

```java
public class ParseException extends RuntimeException {
    public final Token offendingToken;
    // message: "[Syntax Error] at position N: <explanation> (got TYPE "lexeme")"
}
```

Using an unchecked exception keeps all parsing methods clean (no `throws`
declarations) while still propagating errors up to the caller.

### Error Message Quality

Every error includes:
- **Position** — the zero-based character offset in the source
- **Expected construct** — what the grammar required
- **Actual token** — type and lexeme of what was found

Example:
```
[Syntax Error] at position 8: Trailing comma in argument list —
expected an expression after ',' (got RPAREN ")")
```

---

## 8. Operator Precedence — Detailed Example

Input: `=1+2*3`

Parse trace:
```
parseFormula()
  consume(EQUALS)
  parseExpression()
    parseTerm()          ← left operand of '+'
      parseFactor() → NumberNode(1)
    loop: see PLUS → consume, parseTerm()
      parseFactor() → NumberNode(2)
      loop: see STAR → consume, parseFactor() → NumberNode(3)
      return BinaryExpr[*](2, 3)
    return BinaryExpr[+](1, BinaryExpr[*](2, 3))
  consume(EOF)
```

Result:
```
Formula
└── BinaryExpr [+]
    ├── Number [1]
    └── BinaryExpr [*]
        ├── Number [2]
        └── Number [3]
```

`*` is deeper → it is evaluated first.  Correct.

---

## 9. Challenges and Decisions

### Challenge 1 — CELL_REFERENCE vs IDENTIFIER

**Problem:** `A1` and `SUM` both start with letters.  
**Solution:** Resolved at scan time.  The scanner peeks after the letter run:
if a digit immediately follows, it is a `CELL_REFERENCE`; otherwise `IDENTIFIER`.
This avoids any ambiguity in the parser grammar.

### Challenge 2 — Range inside Function Arguments

**Problem:** `SUM(A1:A5)` — the colon appears after a cell reference that has
already been tokenised.  
**Solution:** In `parseFactor()`, after a `CELL_REFERENCE` is consumed, the
parser checks whether the next token is `COLON`.  If so, it parses a second
cell reference and builds a `RangeNode`.  This is a one-token lookahead
decision.

### Challenge 3 — Trailing Comma Detection

**Problem:** `SUM(A1,)` should be rejected.  
**Solution:** After consuming each `COMMA` in `parseArgumentList()`, the parser
checks whether the next token is `RPAREN`.  If so, it throws `ParseException`
with a specific message about the trailing comma.

### Challenge 4 — Unary Minus Chains

**Problem:** `--5` (double negation) must parse correctly.  
**Solution:** `parseFactor()` for unary minus calls `parseFactor()` recursively,
making the rule right-recursive.  `--5` becomes `Unary[-](Unary[-](Number[5]))`.

---

## 10. Summary

This project demonstrates all core concepts of a compiler front-end:

- **Modular separation** of scanner, parser, and AST
- **Grammar design** with explicit precedence levels
- **LL(1) parsing** using one token of lookahead
- **Structural AST** that captures the program's meaning, not its syntax
- **Meaningful error reporting** with position and context

The implementation contains approximately **900 lines** of production-quality
Java across 18 source files, plus a full test suite covering both valid and
invalid inputs.
