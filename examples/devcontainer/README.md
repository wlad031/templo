# Devcontainer Templates

This folder contains Templo templates for the devcontainer files used in this repo,
plus an example values file.

## Files

- `templates/devcontainer.json.tmpl`
- `templates/Dockerfile.tmpl`
- `templates/post-create.sh.tmpl`
- `templates/load-env.sh.tmpl`
- `example-values.lz`

## Render Examples

```bash
templo examples/devcontainer/templates/devcontainer.json.tmpl examples/devcontainer/example-values.lz .devcontainer/devcontainer.json
templo examples/devcontainer/templates/Dockerfile.tmpl examples/devcontainer/example-values.lz .devcontainer/Dockerfile
templo examples/devcontainer/templates/post-create.sh.tmpl examples/devcontainer/example-values.lz .devcontainer/post-create.sh
templo examples/devcontainer/templates/load-env.sh.tmpl examples/devcontainer/example-values.lz .devcontainer/load-env.sh
```
