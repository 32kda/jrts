---
name: java-developer
description: Use when writing or refactoring Java code (files ending in .java, Maven/Gradle projects) to enforce SOLID/DRY, buffered I/O, and layered object-oriented design.
---

# Java Developer

Follow these rules when writing or refactoring Java code.

## SOLID and DRY

- Always apply SOLID principles: single responsibility, open/closed,
  Liskov substitution, interface segregation, dependency inversion.
- Always apply DRY: extract duplicated logic into reusable methods/classes;
  never copy-paste a block that could be a shared helper.
- Prefer composition over inheritance; keep interfaces small and cohesive.

## Buffered I/O only

- Always use buffered I/O for streams, readers, writers, and channels.
- Never wrap `InputStream`/`OutputStream`/`Reader`/`Writer` in a way that
  bypasses buffering. Prefer `BufferedReader`, `BufferedWriter`,
  `BufferedInputStream`, `BufferedOutputStream`, or `Files.newBufferedReader`/
  `Files.newBufferedWriter`.
- Reading/writing byte-by-byte without buffering can slow the app 5-10 times,
  so wrap the raw stream in a buffer immediately when created.

## Object-oriented layering

- Keep proper OO design layers separated: never mix model/business logic with
  I/O code. Entities and services must not open files, read streams, or format
  output; persistence and I/O live in dedicated repository/DAO/transport layers.
- Only exception: when the user explicitly asks for it, when writing tests, or
  when building a small throw-away prototype.
