# Strict String-operation and built-in-parser audit

## Policy and scope

This audit covers every current production Scala file under Slowparse, Lizp, and Templo (17 files total). Built-in parsing/tokenization and `String` slicing, normalization, parsing, tokenization, and accumulation are prohibited except for these approved categories:

1. Generated Lizp-source assembly.
2. Path and filename normalization.
3. Documented `String`/character-buffer adapters at Slowparse public API boundaries.
4. `String` materialization required for public parser results, typed AST/text values, generated Lizp source, or public diagnostics.

All parser state is structural: parser combinators, spans, character vectors, or typed values. Tests, generated sources, and unrelated build/configuration files are outside the source audit.

## Revisions audited

The audit was performed against these current worktrees (including the remediation changes not yet committed in Lizp and Templo):

- Slowparse: `b35785d29791b1fad753a7673122b0c6a3c0bb59`
- Lizp: `7fffdf2b431153204bc0f62acd8c532d4c2c5b18` plus working-tree remediation
- Templo: `1601438f9160387d248f20c77cdb88edafa86727` plus working-tree remediation

Slowparse `0.2.9`, containing `anyCharValue`, was built, tested, and published directly to the Gitea Maven registry. Lizp now consumes that additive release.

## Executable search

The following intentionally broad search was run against each repository's `src/main/**/*.scala` files. It covers parser entry points, String conversion/manipulation, source adapters, interpolation/concatenation, and structural collection operations so exclusions are explicit.

```bash
rg -n --glob '*.scala' \
  'StringBuilder|String\.valueOf|\.(substring|slice|split|trim|strip|replace|startsWith|endsWith|contains|matches|to(Int|Long|Double|Float|Boolean|Char|UpperCase|LowerCase|String)|charAt|head|tail|drop|take|mkString|toSet|safeHead|safeSlice|length|isEmpty|nonEmpty|iterator)\b|\+\+|\+|s"|Paths\.get|Path\.of|Files\.readString|Source\.from(File|Input)|BigDecimal\(|new String\(|Read\.reads|parser\(' \
  <repo>/src/main
```

| Repository | Files inventoried | Candidate lines | Exclusions |
|---|---:|---:|---|
| Slowparse | 1 | 54 | `Vector`/`Set` cursor operations; documented public input/result adapters; parser diagnostics. |
| Lizp | 9 | 68 | Typed AST/text materialization, path/resource boundaries, structural collections, and project parser invocation. |
| Templo | 7 | 118 | Typed YAML/Lizp values, structural `Vector[Char]` grammar operations, generated Lizp source, and path boundaries. |

The candidate-line counts are deliberately not violation counts. Manual classification of every production file below leaves **zero unapproved findings**.

## Remediation evidence

- **Slowparse:** `parser.scala` represents input as a `Vector[Char]` cursor and exposes `anyCharValue`; public `String` input/result materialization remains documented at the `P` API boundary. `charRange` no longer reconstructs character tokens through `String` capture and reparsing.
- **Lizp:** `parser.scala` receives character tokens through `anyCharValue`, parses booleans with grammar branches, folds digits into numeric state, and materializes only typed symbol/string values. `config.scala`, `interpreter.scala`, and `types.scala` no longer normalize or slice String tokens. `repl.scala` uses `ArrayBuffer[Char]` until its input boundary.
- **Templo:** `Parser.scala` uses combinators for template delimiters. `YamlPrelude.scala` replaces `slowyaml4s` with a project-owned `Vector[Char]` grammar and typed YAML `Value` AST, including mappings, block/flow sequences and mappings, quoted scalars, comments, document markers, and literal/folded block scalars. `slowyaml4s` was removed from `build.sbt`.

## Production-file inventory and classification

### Slowparse

| File | Status | Candidate classification |
|---|---|---|
| `src/main/scala/dev/vgerasimov/slowparse/parser.scala` | Compliant | Cursor/choice collections are structural. `mkString` and materialization occur only at documented public result/diagnostic boundaries. |

### Lizp

| File | Status | Candidate classification |
|---|---|---|
| `src/main/scala/dev/vgerasimov/lizp/common.scala` | Compliant | No prohibited token/String handling. |
| `src/main/scala/dev/vgerasimov/lizp/config.scala` | Approved | CLI path decoding and public diagnostics are path/text boundaries. |
| `src/main/scala/dev/vgerasimov/lizp/interpreter.scala` | Compliant | Typed values and combinator parsing only. |
| `src/main/scala/dev/vgerasimov/lizp/main.scala` | Approved | CLI paths/source names and public output are boundaries. |
| `src/main/scala/dev/vgerasimov/lizp/natives.scala` | Approved | Project-parser invocation and typed Lizp text rendering. |
| `src/main/scala/dev/vgerasimov/lizp/parser.scala` | Compliant | Combinators and typed numeric/character state; no String token adapter remains. |
| `src/main/scala/dev/vgerasimov/lizp/reader.scala` | Approved | Resource/file decoding and path normalization boundaries. |
| `src/main/scala/dev/vgerasimov/lizp/repl.scala` | Compliant | `ArrayBuffer[Char]` accumulation; one handler-input materialization boundary. |
| `src/main/scala/dev/vgerasimov/lizp/types.scala` | Compliant | Combinators build typed parameters; public typed text materialization only. |

### Templo

| File | Status | Candidate classification |
|---|---|---|
| `src/main/scala/dev/vgerasimov/templo/common.scala` | Compliant | No prohibited token/String handling. |
| `src/main/scala/dev/vgerasimov/templo/Interpreter.scala` | Approved | Generated Lizp-source assembly. |
| `src/main/scala/dev/vgerasimov/templo/Main.scala` | Approved | CLI path/file-name normalization, file decoding, and output boundaries. |
| `src/main/scala/dev/vgerasimov/templo/Parser.scala` | Compliant | Combinators construct typed template blocks; diagnostics are public text. |
| `src/main/scala/dev/vgerasimov/templo/Templo.scala` | Compliant | No prohibited token/String handling. |
| `src/main/scala/dev/vgerasimov/templo/types.scala` | Compliant | No prohibited token/String handling. |
| `src/main/scala/dev/vgerasimov/templo/YamlPrelude.scala` | Compliant | Character-vector cursor, typed YAML AST, structural escaping; materialization only for typed scalar/key values and generated Lizp prelude text. |

## Verification

All gates ran in the `templo-runner:latest` devcontainer with bounded 240-second commands.

| Repository | Command | Result |
|---|---|---|
| Slowparse | `sbt scalafmt Compile/scalafmtCheck test` | Production formatting check passed; 48 tests passed. |
| Lizp | `sbt scalafmt Compile/scalafmtCheck test` | Production formatting check passed; 59 tests passed using Slowparse `0.2.9`. |
| Templo | `sbt scalafmtAll scalafmtCheckAll test` | Formatting checks passed; 46 tests passed. |

Regression coverage includes Slowparse parser conversion boundaries, Lizp parser token conversion, Templo template parsing and generated source paths, filename normalization, YAML comments/document markers, flow collections, block scalars, nesting, arrays, scalar conversion, and rejection of non-object roots.

## Verdict

Every current production Scala file is inventoried. Broad candidate searches produce only the documented approved categories or structural collection operations. **Remaining unapproved findings: 0.**
