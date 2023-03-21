# Templo

[![build](https://img.shields.io/github/actions/workflow/status/wlad031/templo/scala.yml?label=CI&logo=GitHub&style=flat-square)](https://github.com/wlad031/templo/actions)


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
