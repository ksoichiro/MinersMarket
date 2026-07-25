# Config Screen Local-Settings Notice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a permanent one-line notice at the top of the in-game config screen telling the player that the settings shown are their own local settings, with stronger wording when they are connected to a world hosted by someone else.

**Architecture:** The existing `HeaderEntry` list row gains a colour parameter and is reused to render the notice as the first entry of the settings list. `ConfigScreen.init()` picks the translation key and colour from a host-state test. The same change is applied to all 15 `ConfigScreen.java` copies and all 30 language files by a Python script, since this repository keeps one full source tree per Minecraft version.

**Tech Stack:** Java 17/21/25, Minecraft (Mojang mappings), Gradle 9.4.0, Python 3 for cross-module propagation.

**Spec:** `docs/superpowers/specs/2026-07-25-config-screen-host-only-note-design.md`

## Global Constraints

- The notice must fit one line. `ContainerObjectSelectionList` uses a fixed `ITEM_HEIGHT` of 25px and cannot wrap; `ROW_WIDTH` is 310px.
- All colour literals use full alpha (`0xFF......`). 1.21.9+ and 26.x render text incorrectly without it.
- The four existing category-header call sites (`new HeaderEntry(this.font, ...)` with two arguments) must remain unchanged.
- Editing, validation, and saving behaviour must not change. No inputs are disabled and the Done button logic is untouched.
- Exactly 15 `ConfigScreen.java` files and 30 language files must end up changed — no module may be missed.
- Comments are written in English.
- **Do not run `git commit` until the user explicitly approves it.** Commit steps in this plan are gated on that approval.

## File Structure

**Modified — one per common module (15 files):**

`common/<version>/src/main/java/com/minersmarket/config/client/ConfigScreen.java`
for `1.20.1`, `1.21.1`, `1.21.3`, `1.21.4`, `1.21.5`, `1.21.6`, `1.21.7`, `1.21.8`, `1.21.9`, `1.21.10`, `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`.

These 15 files fall into 6 byte-distinct variants, but every line this plan touches is identical across all of them. This was verified: each of the four anchor strings below occurs exactly once in each of the 15 files.

**Modified — two per common module (30 files):**

`common/<version>/src/main/resources/assets/minersmarket/lang/en_us.json`
`common/<version>/src/main/resources/assets/minersmarket/lang/ja_jp.json`

**Modified — documentation (4 files):**

- `README.md` — "In-Game Config Screen" section
- `docs/modrinth_description.md` — "🛠️ Server Configuration" section
- `docs/curseforge_description.md` — "🛠️ Server Configuration" section
- `CHANGELOG.md` — `[Unreleased]` section

**Created — scratchpad only, not committed:**

- `$SCRATCH/check_notice.py` — consistency checker (this task's substitute for a unit test)
- `$SCRATCH/apply_notice.py` — propagation script

Throughout this plan, `$SCRATCH` means
`/private/tmp/claude-501/-Users-ksoichiro-src-github-com-ksoichiro-MinersMarket/25d73572-c001-46b3-a5e8-c33f0b87975e/scratchpad`.

These two scripts stay in the scratchpad deliberately. This is a one-off migration, and `scripts/` in the repository is for build-time tooling, not for single-use edits.

## Testing Approach

The mod modules have no test source sets — `find . -type d -name test` returns only `gradle/shared/src/test`, which belongs to a git submodule. GUI code here cannot be unit tested without introducing a test harness, which is out of scope.

The executable check for this change is therefore a consistency checker script, written and run **before** the edits so it is seen to fail, then re-run after. It verifies every one of the 45 files received the change, that the colour constants and host test are wired up, and that the notice strings stay within their single-line budget. Compilation across all six source variants and in-game inspection complete the verification.

---

### Task 1: Consistency checker

Write the check first and watch it fail. It is the gate for Tasks 2 and 3.

**Files:**
- Create: `$SCRATCH/check_notice.py`

**Interfaces:**
- Consumes: nothing.
- Produces: a script run as `python3 $SCRATCH/check_notice.py` from the repository root. Exit code 0 means every file is correct; non-zero prints one `FAIL: <path>: <reason>` line per problem. Tasks 2, 3, and 4 re-run this exact command.

- [ ] **Step 1: Write the checker**

Create `$SCRATCH/check_notice.py`:

```python
#!/usr/bin/env python3
"""Verify the config-screen local-settings notice is present in every version module."""
import json
import pathlib
import sys

VERSIONS = [
    "1.20.1", "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7",
    "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2",
]

# The notice row cannot wrap, so the strings have a width budget. Character counts are a
# proxy for pixel width: Minecraft renders Japanese glyphs at 9px and Latin at roughly 6px
# against a 310px row.
MAX_LATIN_CHARS = 52
MAX_CJK_CHARS = 30

EXPECTED_EN = {
    "config.minersmarket.note_local": "Local settings: applied to worlds you host.",
    "config.minersmarket.note_not_host": "Not the host: changes won't affect this world.",
}
EXPECTED_JA = {
    "config.minersmarket.note_local": "ローカル設定です。自分がホストするワールドに適用されます",
    "config.minersmarket.note_not_host": "ホストではないため、この変更は現在のワールドに反映されません",
}

JAVA_REQUIRED = [
    "private static final int HEADER_TEXT_COLOR = 0xFFFFFF55;",
    "private static final int NOTE_TEXT_COLOR = 0xFFAAAAAA;",
    "private static final int NOTE_WARN_COLOR = 0xFFFFAA00;",
    "private boolean isJoinedRemoteWorld() {",
    "!this.minecraft.hasSingleplayerServer();",
    "HeaderEntry(Font font, Component label, int color) {",
    "this(font, label, HEADER_TEXT_COLOR);",
    "private final int color;",
    "boolean joinedRemoteWorld = isJoinedRemoteWorld();",
    '"config.minersmarket.note_not_host"',
    '"config.minersmarket.note_local"',
    "joinedRemoteWorld ? NOTE_WARN_COLOR : NOTE_TEXT_COLOR));",
    ", this.color);",
]

failures = []


def fail(path, reason):
    failures.append(f"FAIL: {path}: {reason}")


def check_java(version):
    path = pathlib.Path(
        f"common/{version}/src/main/java/com/minersmarket/config/client/ConfigScreen.java"
    )
    if not path.exists():
        fail(path, "missing")
        return
    text = path.read_text(encoding="utf-8")
    for needle in JAVA_REQUIRED:
        if needle not in text:
            fail(path, f"missing {needle!r}")
    # After the change the only remaining literal is the HEADER_TEXT_COLOR constant; a second
    # occurrence means a draw call still hardcodes the header colour instead of using the field.
    count = text.count("0xFFFFFF55")
    if count != 1:
        fail(path, f"expected exactly 1 occurrence of 0xFFFFFF55, found {count}")
    # The notice must be added before the first category header, not after it.
    notice_at = text.find("boolean joinedRemoteWorld = isJoinedRemoteWorld();")
    header_at = text.find('Component.translatable("config.minersmarket.category.game")')
    if notice_at != -1 and header_at != -1 and notice_at > header_at:
        fail(path, "notice row is added after the first category header")


def check_lang(version, filename, expected, max_chars):
    path = pathlib.Path(
        f"common/{version}/src/main/resources/assets/minersmarket/lang/{filename}"
    )
    if not path.exists():
        fail(path, "missing")
        return
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(path, f"invalid JSON: {exc}")
        return
    for key, value in expected.items():
        if key not in data:
            fail(path, f"missing key {key}")
        elif data[key] != value:
            fail(path, f"{key} is {data[key]!r}, expected {value!r}")
        elif len(data[key]) > max_chars:
            fail(path, f"{key} is {len(data[key])} chars, over the {max_chars} budget")


for version in VERSIONS:
    check_java(version)
    check_lang(version, "en_us.json", EXPECTED_EN, MAX_LATIN_CHARS)
    check_lang(version, "ja_jp.json", EXPECTED_JA, MAX_CJK_CHARS)

if failures:
    print("\n".join(failures))
    print(f"\n{len(failures)} problem(s) across {len(VERSIONS)} version modules.")
    sys.exit(1)

print(f"OK: {len(VERSIONS)} modules, 15 Java files and 30 language files verified.")
```

- [ ] **Step 2: Run the checker to verify it fails**

Run from the repository root:

```bash
python3 "$SCRATCH/check_notice.py"
```

Expected: exit code 1, with `missing key config.minersmarket.note_local` for all 30 language files and `missing '...'` lines for all 15 Java files. The total should be well over 100 problems across 15 modules. If it reports OK, the checker is broken — fix it before continuing.

---

### Task 2: Add the translation keys

**Files:**
- Create: `$SCRATCH/apply_notice.py`
- Modify: `common/<version>/src/main/resources/assets/minersmarket/lang/en_us.json` (15 files)
- Modify: `common/<version>/src/main/resources/assets/minersmarket/lang/ja_jp.json` (15 files)

**Interfaces:**
- Consumes: `$SCRATCH/check_notice.py` from Task 1.
- Produces: keys `config.minersmarket.note_local` and `config.minersmarket.note_not_host` in both languages of all 15 modules. Task 3 references these key names from Java.

Two variants of each language file exist (they differ only by the presence of `key.category.minersmarket.main`), but both contain the `config.minersmarket.reset` line used as the insertion anchor, so one script handles all 30.

- [ ] **Step 1: Write the language-file half of the propagation script**

Create `$SCRATCH/apply_notice.py`:

```python
#!/usr/bin/env python3
"""Propagate the config-screen local-settings notice across all version modules."""
import pathlib
import sys

VERSIONS = [
    "1.20.1", "1.21.1", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7",
    "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2",
]

LANG_ANCHOR = '  "config.minersmarket.reset":'

LANG_ADDITIONS = {
    "en_us.json": (
        '  "config.minersmarket.note_local": "Local settings: applied to worlds you host.",\n'
        '  "config.minersmarket.note_not_host": "Not the host: changes won\'t affect this world.",\n'
    ),
    "ja_jp.json": (
        '  "config.minersmarket.note_local": "ローカル設定です。自分がホストするワールドに適用されます",\n'
        '  "config.minersmarket.note_not_host": "ホストではないため、この変更は現在のワールドに反映されません",\n'
    ),
}


def patch_lang(version, filename):
    path = pathlib.Path(
        f"common/{version}/src/main/resources/assets/minersmarket/lang/{filename}"
    )
    text = path.read_text(encoding="utf-8")
    if "config.minersmarket.note_local" in text:
        print(f"skip (already patched): {path}")
        return
    lines = text.splitlines(keepends=True)
    anchors = [i for i, line in enumerate(lines) if line.startswith(LANG_ANCHOR)]
    if len(anchors) != 1:
        sys.exit(f"ERROR: {path}: expected 1 anchor line, found {len(anchors)}")
    index = anchors[0] + 1
    lines.insert(index, LANG_ADDITIONS[filename])
    path.write_text("".join(lines), encoding="utf-8")
    print(f"patched: {path}")


for version in VERSIONS:
    patch_lang(version, "en_us.json")
    patch_lang(version, "ja_jp.json")
```

- [ ] **Step 2: Run it**

```bash
python3 "$SCRATCH/apply_notice.py"
```

Expected: 30 `patched:` lines, no `ERROR:`.

- [ ] **Step 3: Confirm the language half of the checker now passes**

```bash
python3 "$SCRATCH/check_notice.py"
```

Expected: still exit code 1, but every remaining failure names a `ConfigScreen.java` path. No failure should name a `.json` path. If a JSON failure remains, it is a wrong string or a busted budget — fix the value in `apply_notice.py`, revert with `git checkout -- common`, and re-run.

- [ ] **Step 4: Spot-check the result by hand**

```bash
git diff --stat common
```

Expected: exactly 30 files changed, 2 insertions each, 60 insertions total.

---

### Task 3: Add the notice row to the config screen

**Files:**
- Modify: `$SCRATCH/apply_notice.py`
- Modify: `common/<version>/src/main/java/com/minersmarket/config/client/ConfigScreen.java` (15 files)

**Interfaces:**
- Consumes: the translation keys added in Task 2.
- Produces: `ConfigScreen.HeaderEntry(Font, Component, int)` — a three-argument constructor taking the text colour, alongside the existing two-argument constructor which now delegates with `HEADER_TEXT_COLOR`. Also `ConfigScreen.isJoinedRemoteWorld()` returning `boolean`.

Five edits are applied per file. The order matters: the `0xFFFFFF55` replacement runs **first**, while that literal still occurs exactly once. Adding the `HEADER_TEXT_COLOR` constant beforehand would introduce a second occurrence and make the replacement ambiguous.

- [ ] **Step 1: Append the Java half to the propagation script**

Append to `$SCRATCH/apply_notice.py`:

```python
JAVA_COLOR_ANCHOR = "    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;\n"

JAVA_COLOR_ADDITION = (
    "    private static final int HEADER_TEXT_COLOR = 0xFFFFFF55;\n"
    "    private static final int NOTE_TEXT_COLOR = 0xFFAAAAAA;\n"
    "    private static final int NOTE_WARN_COLOR = 0xFFFFAA00;\n"
)

JAVA_HELPER_ANCHOR = "    private void updateDoneButton() {\n"

JAVA_HELPER_ADDITION = (
    "    // The config is read only by server-side logic from the local file, and nothing\n"
    "    // syncs it over the network. On a world hosted by someone else these values are the\n"
    "    // player's own and do not affect that session, so the notice must say which case\n"
    "    // the player is in. Singleplayer and LAN hosting both keep an integrated server.\n"
    "    private boolean isJoinedRemoteWorld() {\n"
    "        return this.minecraft != null && this.minecraft.level != null\n"
    "                && !this.minecraft.hasSingleplayerServer();\n"
    "    }\n"
    "\n"
)

JAVA_INIT_ANCHOR = (
    '        this.list.addEntry(new HeaderEntry(this.font, '
    'Component.translatable("config.minersmarket.category.game")));\n'
)

JAVA_INIT_ADDITION = (
    "        boolean joinedRemoteWorld = isJoinedRemoteWorld();\n"
    "        this.list.addEntry(new HeaderEntry(this.font,\n"
    "                Component.translatable(joinedRemoteWorld\n"
    '                        ? "config.minersmarket.note_not_host"\n'
    '                        : "config.minersmarket.note_local"),\n'
    "                joinedRemoteWorld ? NOTE_WARN_COLOR : NOTE_TEXT_COLOR));\n"
    "\n"
)

JAVA_CTOR_OLD = (
    "        private final Font font;\n"
    "        private final Component label;\n"
    "\n"
    "        HeaderEntry(Font font, Component label) {\n"
    "            this.font = font;\n"
    "            this.label = label;\n"
    "        }\n"
)

JAVA_CTOR_NEW = (
    "        private final Font font;\n"
    "        private final Component label;\n"
    "        private final int color;\n"
    "\n"
    "        HeaderEntry(Font font, Component label) {\n"
    "            this(font, label, HEADER_TEXT_COLOR);\n"
    "        }\n"
    "\n"
    "        HeaderEntry(Font font, Component label, int color) {\n"
    "            this.font = font;\n"
    "            this.label = label;\n"
    "            this.color = color;\n"
    "        }\n"
)


def replace_once(path, text, old, new, label):
    count = text.count(old)
    if count != 1:
        sys.exit(f"ERROR: {path}: {label}: expected 1 occurrence, found {count}")
    return text.replace(old, new)


def patch_java(version):
    path = pathlib.Path(
        f"common/{version}/src/main/java/com/minersmarket/config/client/ConfigScreen.java"
    )
    text = path.read_text(encoding="utf-8")
    if "NOTE_TEXT_COLOR" in text:
        print(f"skip (already patched): {path}")
        return
    # Must run before HEADER_TEXT_COLOR is introduced, while 0xFFFFFF55 is still unique.
    # The colour is the final argument of the draw call in all three rendering API families
    # (render / renderContent / extractContent), so one substitution covers every variant.
    text = replace_once(path, text, ", 0xFFFFFF55);", ", this.color);", "draw colour")
    text = replace_once(
        path, text, JAVA_COLOR_ANCHOR, JAVA_COLOR_ANCHOR + JAVA_COLOR_ADDITION, "colour constants"
    )
    text = replace_once(
        path, text, JAVA_HELPER_ANCHOR, JAVA_HELPER_ADDITION + JAVA_HELPER_ANCHOR, "host test"
    )
    text = replace_once(
        path, text, JAVA_INIT_ANCHOR, JAVA_INIT_ADDITION + JAVA_INIT_ANCHOR, "notice row"
    )
    text = replace_once(path, text, JAVA_CTOR_OLD, JAVA_CTOR_NEW, "HeaderEntry constructor")
    path.write_text(text, encoding="utf-8")
    print(f"patched: {path}")


for version in VERSIONS:
    patch_java(version)
```

- [ ] **Step 2: Run it**

```bash
python3 "$SCRATCH/apply_notice.py"
```

Expected: 30 `skip (already patched):` lines for the language files, then 15 `patched:` lines for the Java files, and no `ERROR:`. An `ERROR:` means an anchor did not match in some module — stop and inspect that file rather than loosening the anchor.

- [ ] **Step 3: Run the checker to verify it passes**

```bash
python3 "$SCRATCH/check_notice.py"
```

Expected: exit code 0 and
`OK: 15 modules, 15 Java files and 30 language files verified.`

- [ ] **Step 4: Read one patched file to confirm it reads correctly**

Read `common/1.21.1/src/main/java/com/minersmarket/config/client/ConfigScreen.java` and confirm:
- the constants block sits directly under `INVALID_TEXT_COLOR`
- `init()` adds the notice row immediately before the `category.game` header row
- `HeaderEntry` has both constructors and the draw call ends with `, this.color);`
- indentation matches the surrounding code

Then read `common/26.2/src/main/java/com/minersmarket/config/client/ConfigScreen.java` and confirm the same, noting that its draw call is `guiGraphics.centeredText(...)` rather than `drawCenteredString(...)`.

- [ ] **Step 5: Confirm the diff size**

```bash
git diff --stat common
```

Expected: 45 files changed, 450 insertions, 15 deletions. That is 30 language files at 2 insertions each (60), plus 15 Java files at 26 insertions and 1 deletion each (390 / 15). The single deletion per Java file is the draw-call line whose colour literal was replaced; the constructor rewrite lands as pure insertions because the original lines are preserved verbatim.

---

### Task 4: Compile every source variant

**Files:** none modified.

**Interfaces:**
- Consumes: the changes from Tasks 2 and 3.
- Produces: confirmation that all six `ConfigScreen.java` variants compile. No later task depends on its output.

The 15 files fall into 6 byte-distinct variants; one version per variant is compiled. `compileJava` is the verification command this repository already uses (see `tasks.local.md`), and is much faster than a full `build`.

| Variant | Representative | Covers |
| --- | --- | --- |
| 1 | 1.21.1 | 1.21.1, 1.21.3–1.21.8 |
| 2 | 1.21.9 | 1.21.9, 1.21.10 |
| 3 | 1.21.11 | 1.21.11 |
| 4 | 26.1 | 26.1, 26.1.1, 26.1.2 |
| 5 | 26.2 | 26.2 |
| 6 | 1.20.1 | 1.20.1 |

- [ ] **Step 1: Compile variant 1 and variant 6**

```bash
./gradlew compileJava -Ptarget_mc_version=1.21.1
./gradlew compileJava -Ptarget_mc_version=1.20.1
```

Expected: `BUILD SUCCESSFUL` for both. Run these one at a time and pipe through `run-wrapped --tail 40` if the output is long.

- [ ] **Step 2: Compile variants 2 and 3**

```bash
./gradlew compileJava -Ptarget_mc_version=1.21.9
./gradlew compileJava -Ptarget_mc_version=1.21.11
```

Expected: `BUILD SUCCESSFUL` for both.

- [ ] **Step 3: Compile variants 4 and 5**

```bash
./gradlew compileJava -Ptarget_mc_version=26.1
./gradlew compileJava -Ptarget_mc_version=26.2
```

Expected: `BUILD SUCCESSFUL` for both. These are the highest-risk builds: they use Java 25 and the `GuiGraphicsExtractor` rendering API. A failure mentioning `hasSingleplayerServer` or `level` would mean the mapping check in the spec was wrong — report it rather than working around it.

- [ ] **Step 4: Report results**

State plainly which of the six compiled and which did not. Do not proceed to Task 5 with a failing variant.

---

### Task 5: Update documentation

**Files:**
- Modify: `README.md` (In-Game Config Screen section, around lines 163-166)
- Modify: `docs/modrinth_description.md` ("🛠️ Server Configuration" section, around line 62)
- Modify: `docs/curseforge_description.md` ("🛠️ Server Configuration" section, around line 62)
- Modify: `CHANGELOG.md` (`[Unreleased]` section, around line 15)

**Interfaces:**
- Consumes: the user-visible behaviour built in Tasks 2 and 3.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Update README.md**

Find this paragraph:

```
The screen validates input against the same rules as the file loader and writes changes
back to your local `minersmarket.toml`, preserving comments. Changes apply immediately,
with the same semantics as `/minersmarket config reload`. In multiplayer, this edits the
local file only — dedicated servers are still configured server-side as described above.
```

Append one sentence to it:

```
When you open the screen while connected to a world hosted by someone else, the values shown
are your own local ones rather than the host's, and editing them has no effect on that
session; the screen displays a notice saying so.
```

- [ ] **Step 2: Update both distribution descriptions**

In `docs/modrinth_description.md` and `docs/curseforge_description.md`, find:

```
- **Live Reload**: Apply changes on a running server with `/minersmarket config reload`
```

Insert this bullet directly after it, in **both** files (they are near-identical and the bullet is the same):

```
- **In-Game Config Screen**: Edit all settings from a built-in screen (Mods list, ModMenu, or a key binding) — applies to the world you host
```

- [ ] **Step 3: Update CHANGELOG.md**

In the `[Unreleased]` section, the config screen entry ends with this sub-bullet:

```
  - Saves to the local config file preserving comments and applies immediately (same semantics as `/minersmarket config reload`)
```

Add one sub-bullet after it, at the same indentation:

```
  - Shows a notice that the settings are local, with a warning when connected to a world hosted by another player
```

- [ ] **Step 4: Verify the documentation edits**

```bash
git diff --stat README.md docs/modrinth_description.md docs/curseforge_description.md CHANGELOG.md
```

Expected: 4 files changed, 1-3 insertions each. Read each diff hunk and confirm the surrounding Markdown structure is intact.

---

### Task 6: In-game verification

**Files:** none modified.

**Interfaces:**
- Consumes: everything above.
- Produces: the final confirmation that the notice renders correctly.

This project has previously hit screen-rendering differences specifically on 1.20.1 and on 1.21.9+/26.x, so those two families are the ones checked by hand.

- [ ] **Step 1: Build runnable clients**

```bash
./gradlew build -Ptarget_mc_version=1.20.1
./gradlew build -Ptarget_mc_version=26.2
```

Expected: `BUILD SUCCESSFUL` for both.

- [ ] **Step 2: Check the title-screen and singleplayer cases**

Launch the 1.20.1 client. Open the config screen from the mod list at the title screen, then again from inside a singleplayer world.

Expected in both cases: the first row of the settings list reads
`Local settings: applied to worlds you host.` (or the Japanese equivalent if the client language is Japanese), rendered in grey, on a single line that is not clipped at either end, positioned above the "Game" category header.

- [ ] **Step 3: Check the non-host case**

Start a second client instance. Open a world in one and choose Open to LAN, then join it from the other. On the **joining** client, open the config screen.

Expected: the first row reads `Not the host: changes won't affect this world.` in orange, on a single line. On the **hosting** client the row still shows the grey local-settings text, because LAN hosting keeps an integrated server.

- [ ] **Step 4: Repeat on 26.2**

Repeat Steps 2 and 3 with the 26.2 client. Pay particular attention to text colour — this is the version family where a missing alpha channel previously caused text to render incorrectly.

- [ ] **Step 5: Check the Japanese strings**

Switch the client language to Japanese and reopen the screen in both the host and non-host cases. Confirm both Japanese strings fit on one line without clipping.

- [ ] **Step 6: Report results**

State which checks passed and which did not, with what was actually observed. If any string is clipped, shorten it in both `apply_notice.py` and `check_notice.py`, revert with `git checkout -- common`, and re-run Tasks 2 through 4.

---

### Task 7: Commit

- [ ] **Step 1: Ask the user for approval to commit**

Per this repository's working agreement, do not commit without explicit instruction. Show `git status` and the proposed commit message, and wait for approval.

- [ ] **Step 2: Commit once approved**

```bash
git add common README.md docs CHANGELOG.md
git commit -m "$(cat <<'EOF'
feat: show local-settings notice on the config screen

The config is read from the local file by server-side logic only and is
never synced over the network, so a player joined to someone else's world
sees their own values with no effect on that session. Add a notice row at
the top of the settings list, with a warning variant for that case.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

Note that `git add docs` includes the design spec and this plan under `docs/superpowers/`. Confirm with the user whether those should be part of the commit, and adjust the paths if not.

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
| --- | --- |
| Notice content — keys, strings, colours, host-state test | 3 (Java), 2 (strings) |
| Rendering — `HeaderEntry` colour parameter, single substitution across API families | 3 |
| Width constraint | 1 (budget assertion), 6 (visual check) |
| Files affected — 15 Java, 30 language, 6 variants | 2, 3, 4 |
| Documentation — README, modrinth, curseforge | 5 |
| Verification — mapping check, per-variant build, in-game check | 4, 6 |
| Out of scope — no config sync, no disabled inputs, no load/save changes | not implemented anywhere; Global Constraints restates it |

The spec's mapping check was resolved before this plan was written: `hasSingleplayerServer` is present in the mapped `Minecraft` class for 1.20.1, 26.1, and 26.2, and `mc.level` is already used by `MarketMarkerRenderer` in every version family. Task 4 Step 3 still calls out a mapping failure as a reportable outcome.

CHANGELOG.md is updated in Task 5 although the spec does not mention it, because the `[Unreleased]` section already carries the config screen entry this change extends.

**Placeholder scan:** none. Every script, string, command, and expected output is written out in full.

**Type consistency:** `isJoinedRemoteWorld()` returns `boolean` and is called as `boolean joinedRemoteWorld = isJoinedRemoteWorld();` in Task 3. `HEADER_TEXT_COLOR`, `NOTE_TEXT_COLOR`, and `NOTE_WARN_COLOR` are declared in the constants block and used in the constructor delegation and the notice row respectively. The key names `config.minersmarket.note_local` and `config.minersmarket.note_not_host` are identical in Task 2's language additions, Task 3's Java, and Task 1's checker.
