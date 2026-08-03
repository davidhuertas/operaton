#!/usr/bin/env python3
"""Filter Operaton's full openapi.json down to a single tag's operations plus the
transitive closure of schemas they reference.

Rationale: generating Spring MVC delegates against Operaton's full ~377-endpoint spec hits
several unrelated, pre-existing bugs in Operaton's own spec examples (multi-line XML samples -
e.g. Decision Definition's DMN XML, Decision Requirements Definition's DRD XML, Process
Definition's BPMN XML, Identity's examples - embed raw newlines that openapi-generator drops
verbatim into Java string literals, breaking compilation). Filtering to only the tag this
adapter actually implements avoids the whole class of bugs instead of excluding generated
files one at a time as they're discovered.

Usage: filter_openapi_spec.py <input-openapi.json> <output-openapi.json> <tag-name>
"""
import json
import sys


def find_schema_refs(node, found):
    if isinstance(node, dict):
        ref = node.get("$ref")
        if isinstance(ref, str) and ref.startswith("#/components/schemas/"):
            found.add(ref.rsplit("/", 1)[-1])
        for value in node.values():
            find_schema_refs(value, found)
    elif isinstance(node, list):
        for item in node:
            find_schema_refs(item, found)


def main():
    src, dst, tag = sys.argv[1], sys.argv[2], sys.argv[3]

    with open(src, encoding="utf-8") as f:
        spec = json.load(f)

    kept_paths = {}
    for path, path_item in spec.get("paths", {}).items():
        kept_ops = {
            method: op
            for method, op in path_item.items()
            if isinstance(op, dict) and tag in op.get("tags", [])
        }
        if kept_ops:
            kept_paths[path] = kept_ops

    all_schemas = spec.get("components", {}).get("schemas", {})
    needed = set()
    find_schema_refs(kept_paths, needed)

    changed = True
    while changed:
        changed = False
        for name in list(needed):
            schema = all_schemas.get(name)
            if schema is None:
                continue
            before = len(needed)
            find_schema_refs(schema, needed)
            if len(needed) != before:
                changed = True

    spec["paths"] = kept_paths
    if "components" in spec:
        spec["components"]["schemas"] = {
            name: all_schemas[name] for name in needed if name in all_schemas
        }
    if "tags" in spec:
        spec["tags"] = [t for t in spec["tags"] if t.get("name") == tag]

    with open(dst, "w", encoding="utf-8") as f:
        json.dump(spec, f)

    print(f"Filtered spec to tag '{tag}': {len(kept_paths)} paths, "
          f"{len(spec['components']['schemas'])} schemas -> {dst}")


if __name__ == "__main__":
    main()
