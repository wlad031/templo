# Templo

[![build](https://img.shields.io/github/workflow/status/wlad031/templo/Scala%20CI?label=CI&logo=GitHub&style=flat-square)](https://github.com/wlad031/templo/actions)
[![codecov](https://img.shields.io/codecov/c/github/wlad031/templo?label=cov&logo=Codecov&style=flat-square)](https://codecov.io/gh/wlad031/templo)

## Syntax

Syntax is pretty simple, code blocks are surrounded with curly brackets:

```
{{foo "hello" } 
world that many times: { i } 
{}}
```

This example will be translated into the following function call:
```
(foo "hello" "world that many times: " i)
```
