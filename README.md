# Templo

[![build](https://img.shields.io/github/actions/workflow/status/wlad031/templo/scala.yml?label=CI&logo=GitHub&style=flat-square)](https://github.com/wlad031/templo/actions)


## Syntax

Templo uses only `{{ ... }}` code blocks.

- text outside code blocks is treated as string literals
- code inside `{{ ... }}` is inserted as raw Lizp fragments
- final template is compiled into a single `(concat ...)` expression and evaluated once

Example:

```
Hello {{name}}!
```

Compiled Lizp:

```
(concat "Hello " name "!")
```

Cross-block continuation is supported:

```
{{(concat }} text {{ )}}
```

Compiled Lizp:

```
(concat (concat " text "))
```

## CLI

Templo is a command-line renderer that accepts a template file, optional data file,
and produces rendered output.

```bash
templo <template-file> [data-file] [output-file]
```

Or with flags:

```bash
templo --template <template-file> [--data <data-file>] [--out <output-file>]
```

If output file is omitted, Templo prints rendered text to stdout.
If data file is provided, its Lizp code is prepended before template evaluation.
