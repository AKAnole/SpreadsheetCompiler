# Spreadsheet Formula Language — Compiler Front-End

> A university-level compiler front-end project in Java implementing a complete
> scanner, recursive-descent parser, AST construction, and error reporting for
> spreadsheet formula syntax.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Project Structure](#2-project-structure)
3. [Building and Running](#3-building-and-running)
4. [Language Specification](#4-language-specification)
5. [Token Specification](#5-token-specification)
6. [Grammar](#6-grammar)
7. [Architecture](#7-architecture)
8. [Demo Output](#8-demo-output)
9. [Design Report](#9-design-report)

---

## 1. Project Overview

This project implements the **front-end** of a compiler for a spreadsheet formula
language (similar to Excel/LibreOffice Calc formulas).  It does **not** evaluate
formulas — it only analyses their structure.

| Phase | Component | Output |
|-------|-----------|--------|
| Lexical analysis | `Lexer.java` | Token stream |
| Syntax analysis  | `Parser.java` | Abstract Syntax Tree |
| Error reporting  | `ParseException.java` | Error messages with position |

---

## 2. Project Structure

```
src/
├── lexer/
│   ├── TokenType.java        — Enum of all token categories
│   ├── Token.java            — Immutable token value object (type + lexeme + position)
│   └── Lexer.java            — Hand-written character-by-character scanner
│
├── parser/
│   ├── Parser.java           — Recursive-descent parser; builds the AST
│   └── ParseException.java   — Unchecked exception for syntax errors
│
├── ast/
│   ├── ASTNode.java          — Abstract base; defines toTree() and toJson()
│   ├── FormulaNode.java      — Root node  (Formula → '=' Expression)
│   ├── NumberNode.java       — Integer literal leaf
│   ├── CellReferenceNode.java— Cell reference leaf (A1, ZZ99)
│   ├── BinaryExpressionNode.java — Binary +, -, *, /
│   ├── UnaryExpressionNode.java  — Unary minus
│   ├── FunctionCallNode.java — Named function call with argument list
│   ├── RangeNode.java        — Cell range  A1:B3
│   └── ParenthesizedNode.java— Explicit grouping ( E )
│
├── tests/
│   ├── ValidTests.java       — 40+ valid formula test cases
│   ├── InvalidTests.java     — 20+ invalid formula test cases
│   └── TestRunner.java       — Orchestrates both test suites
│
└── Main.java                 — Entry point; REPL and demo driver
```

---

## 3. Building and Running

### Prerequisites

- JDK 17 or later (Java 21 recommended)

### Compile

```bash
mkdir -p out
javac -cp src -d out $(find src -name "*.java")
```

### Run — Full Test Suite + Interactive REPL

```bash
java -cp out Main
```

### Run — Curated Demo Only

```bash
java -cp out Main --demo
```

### Run — Tests Only

```bash
java -cp out Main --test
# or directly:
java -cp out tests.TestRunner
```

### Run — Compile a Single Formula

```bash
java -cp out Main "=SUM(A1:A5)+MAX(B1:B5)"
```

### Run — Interactive REPL Only

```bash
java -cp out Main --repl
```

---

## 4. Language Specification

### Formulas

Every formula begins with the `=` sigil:

```
=A1+B2
=SUM(A1,B2,5)
=(A1+B1)*2
=MAX(1,2,SUM(A1,B1))
=(SUM(A1:A10)+MAX(B1:B10))/2
```

### Supported Constructs

| Construct | Example | Notes |
|-----------|---------|-------|
| Integer literal | `42`, `0`, `1000` | Non-negative integers |
| Cell reference | `A1`, `AA10`, `ZZ99` | Letters + digits, no gap |
| Cell range | `A1:B10` | Used inside function arguments |
| Arithmetic | `+`, `-`, `*`, `/` | Standard precedence |
| Grouping | `(A1+B2)` | Parentheses override precedence |
| Unary minus | `-A1`, `-5` | Prefix negation |
| Function call | `SUM(A1,B2)` | Zero or more comma-separated args |
| Nested calls | `MAX(1,SUM(A1,B1))` | Arbitrary nesting depth |

---

## 5. Token Specification

| Token Type | Pattern | Example Lexemes |
|------------|---------|-----------------|
| `EQUALS` | `=` | `=` |
| `NUMBER` | `[0-9]+` | `42`, `0`, `1000` |
| `CELL_REFERENCE` | `[A-Za-z]+[0-9]+` | `A1`, `AA10`, `ZZ99` |
| `IDENTIFIER` | `[A-Za-z]+` (no trailing digit) | `SUM`, `MAX`, `IF` |
| `PLUS` | `+` | `+` |
| `MINUS` | `-` | `-` |
| `STAR` | `*` | `*` |
| `SLASH` | `/` | `/` |
| `LPAREN` | `(` | `(` |
| `RPAREN` | `)` | `)` |
| `COMMA` | `,` | `,` |
| `COLON` | `:` | `:` |
| `EOF` | end of input | `""` |
| `ILLEGAL` | any unrecognised char | `@`, `#`, `$` |

### Cell Reference vs Identifier Disambiguation

The scanner resolves the ambiguity at scan time by checking whether the letter
run is **immediately** followed by digits (no whitespace):

```
A1    → CELL_REFERENCE   (letters then digits, no gap)
SUM   → IDENTIFIER       (letters only)
A     → IDENTIFIER       (letters only)
AA10  → CELL_REFERENCE
```

---

## 6. Grammar

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

### Precedence Table (high to low)

| Level | Operators | Associativity |
|-------|-----------|---------------|
| 1 (highest) | Unary `-` | Right |
| 2 | `*`, `/` | Left |
| 3 (lowest) | `+`, `-` | Left |

Precedence is encoded **structurally** in the grammar — no explicit table is
needed at runtime.  Higher-precedence operators appear in deeper grammar rules
and therefore sit lower in the AST, meaning they are evaluated first.

---

## 7. Architecture

### Scanner → Parser Communication

```
Source string
     │
     ▼
  ┌────────┐   List<Token>   ┌────────┐   FormulaNode (AST)
  │ Lexer  │ ─────────────▶  │ Parser │ ─────────────────▶
  └────────┘                 └────────┘
```

1. `Lexer.tokenize()` runs to **completion** and returns an immutable
   `List<Token>`.
2. `Parser` is constructed with that list and processes it with an integer
   cursor.
3. The two phases are **completely decoupled** — the parser never calls back
   into the scanner.

### Why Recursive Descent?

- **Directly mirrors the grammar**: each non-terminal has one parsing method.
- **Easy to extend**: adding a new construct means adding/modifying one method.
- **Readable**: the code structure is self-documenting for anyone who knows the
  grammar.
- **Good error recovery**: a failed `consume()` throws immediately at the
  right position.

### Token vs Lexeme

| Concept | Meaning | Example |
|---------|---------|---------|
| **Token type** | Abstract category | `NUMBER` |
| **Lexeme** | Actual source text | `"42"` |

Two different lexemes can share the same type: `"1"` and `"100"` are both
`NUMBER` tokens.  The parser uses the type for grammar decisions; later phases
(evaluator, code generator) use the lexeme to recover the value.

### AST vs Token Stream

| Token Stream | AST |
|--------------|-----|
| Flat sequence | Hierarchical tree |
| Preserves whitespace position | Preserves structure only |
| Includes syntactic noise (parens, commas) | Groups children under nodes |
| Used by parser | Used by semantic analysis / code gen |

The AST for `=A1+B2*C3`:

```
Formula
└── BinaryExpr [+]
    ├── CellRef [A1]
    └── BinaryExpr [*]
        ├── CellRef [B2]
        └── CellRef [C3]
```

Note that `*` is deeper because it has higher precedence.

---

## 8. Demo Output

### Example 1 — Simple Valid: `=A1+B2`

**Token stream:**
```
EQUALS          lexeme="="   @pos 0
CELL_REFERENCE  lexeme="A1"  @pos 1
PLUS            lexeme="+"   @pos 3
CELL_REFERENCE  lexeme="B2"  @pos 4
EOF             lexeme=""    @pos 6
```

**AST:**
```
Formula
└── BinaryExpr [+]
    ├── CellRef [A1]
    └── CellRef [B2]
```

**JSON:**
```json
{
  "type": "Formula",
  "body": {
    "type": "BinaryExpression",
    "operator": "+",
    "left":  { "type": "CellReference", "ref": "A1", "col": "A", "row": 1 },
    "right": { "type": "CellReference", "ref": "B2", "col": "B", "row": 2 }
  }
}
```

---

### Example 2 — Complex Valid: `=(SUM(A1:A10)+MAX(B1:B10))/2`

**Token stream:**
```
EQUALS          "="     @0
LPAREN          "("     @1
IDENTIFIER      "SUM"   @2
LPAREN          "("     @5
CELL_REFERENCE  "A1"    @6
COLON           ":"     @8
CELL_REFERENCE  "A10"   @9
RPAREN          ")"     @12
PLUS            "+"     @13
IDENTIFIER      "MAX"   @14
LPAREN          "("     @17
CELL_REFERENCE  "B1"    @18
COLON           ":"     @20
CELL_REFERENCE  "B10"   @21
RPAREN          ")"     @24
RPAREN          ")"     @25
SLASH           "/"     @26
NUMBER          "2"     @27
EOF             ""      @28
```

**AST:**
```
Formula
└── BinaryExpr [/]
    ├── Parenthesized
    │   └── BinaryExpr [+]
    │       ├── FunctionCall [SUM]
    │       │   └── Range [A1:A10]
    │       │       ├── CellRef [A1]
    │       │       └── CellRef [A10]
    │       └── FunctionCall [MAX]
    │           └── Range [B1:B10]
    │               ├── CellRef [B1]
    │               └── CellRef [B10]
    └── Number [2]
```

---

### Example 3 — Invalid: `=SUM(A1,)`

**Error output:**
```
[Syntax Error] at position 8: Trailing comma in argument list — 
expected an expression after ',' (got RPAREN ")")
```

---

## 9. Design Report

### Language Description

The Spreadsheet Formula Language is a small expression language designed for
embedding in cell-based spreadsheets.  Every formula begins with `=` followed
by an arithmetic expression that may reference cells, use built-in functions,
and group sub-expressions with parentheses.

### Scanner Design

The scanner (`Lexer.java`) is a single-pass, character-by-character automaton.
It maintains a single integer cursor (`pos`) over the source string and
dispatches to specialised scanning methods (`scanNumber`, `scanIdentifierOrCellRef`)
based on the current character.

Key decisions:
- **No regular-expression library** — all character classification uses
  `Character.isDigit()`, `Character.isLetter()`, etc.
- **Cell-reference disambiguation at scan time** — the scanner peeks one
  character ahead after the letter run to decide `CELL_REFERENCE` vs
  `IDENTIFIER`.  This keeps the parser simpler.
- **ILLEGAL token** — unrecognised characters emit a token rather than
  throwing, allowing the parser to report a contextual error message.

### Parser Design

The parser (`Parser.java`) is a pure recursive-descent LL(1) parser.  Each
grammar non-terminal maps to exactly one private method:

| Grammar rule | Method |
|---|---|
| `Formula` | `parseFormula()` |
| `Expression` | `parseExpression()` |
| `Term` | `parseTerm()` |
| `Factor` | `parseFactor()` |
| `ArgumentList` | `parseArgumentList()` |

The parser maintains an integer cursor into the pre-built token list.
`current()` provides one token of lookahead without consuming; `advance()`
consumes and returns; `consume(expected, msg)` enforces an expectation and
throws `ParseException` on mismatch.

### AST Design

Each grammar construct has a dedicated node class extending `ASTNode`:

```
ASTNode (abstract)
├── FormulaNode
├── NumberNode
├── CellReferenceNode
├── BinaryExpressionNode
├── UnaryExpressionNode
├── FunctionCallNode
├── RangeNode
└── ParenthesizedNode
```

All nodes implement:
- `toTree(prefix)` — indented ASCII art for debugging
- `toJson(indent)` — JSON export for tooling integration

### Error Handling Strategy

Errors are divided into two phases:

1. **Lexical errors** — the scanner emits an `ILLEGAL` token.  The parser
   then raises `ParseException` when it encounters one.
2. **Syntax errors** — the parser's `consume()` method throws `ParseException`
   whenever the current token does not match the expected type.

`ParseException` stores the offending `Token` (type, lexeme, position) so that
error messages include the exact source location.

### Challenges and Decisions

| Challenge | Decision |
|-----------|----------|
| Cell ref vs identifier ambiguity | Resolved in scanner by peeking at post-letter character |
| Operator precedence | Encoded structurally in grammar levels (expression/term/factor) |
| Range inside function args | Handled in `parseFactor()` — CELL_REFERENCE followed by COLON |
| Trailing comma detection | Explicit check after consuming COMMA in `parseArgumentList()` |
| Reversed range A5:A1 | Lightweight semantic check in parser — same column, start row > end row |
| Unary minus | Right-recursive call to `parseFactor()` supports chains like `--5` |

---

*Generated as a compiler course project — demonstrates scanner/parser/AST pipeline.*
