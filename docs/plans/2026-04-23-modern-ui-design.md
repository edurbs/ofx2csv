# Modern UI Redesign — Design Document

**Date:** 2026-04-23
**Status:** Approved
**Approach:** Polished Desktop (Approach A)

## Context

The current OFX-to-Excel converter has a bare-bones JavaFX UI — 7 controls stacked in a plain VBox with zero custom styling. It works functionally but looks like a developer prototype. Daily-use office workers in a Brazilian fintech environment need a professional, efficient, and visually appealing tool.

## Visual Identity

**Style:** Modern Brazilian fintech (Nubank/Inter-inspired flat design)

**Color Palette:**
| Token | Value | Usage |
|-------|-------|-------|
| Background | `#F5F5F7` | Window background |
| Card | `#FFFFFF` | Surface panels |
| Primary | `#7C3AED` | Buttons, accents, links |
| Success | `#10B981` | Success states |
| Error | `#EF4444` | Error states |
| Text primary | `#1F2937` | Main text |
| Text muted | `#6B7280` | Secondary text |
| Row stripe | `#F9FAFB` | Alternating table rows |

**Typography:** System default. Bold for headers, regular for body.

**Window:** Fixed 750x550, centered, non-resizable. Title: "Conversor OFX → Excel".

## Layout

Card-based single-screen design with 4 zones:

```
+----------------------------------------------------------+
|  [icon] Conversor OFX → Excel                    [— □ ×] |
+----------------------------------------------------------+
|  +----------------------------------------------------+  |
|  |  DROP ZONE: Drag & drop OFX files                  |  |
|  |  Click to browse | file list with remove buttons   |  |
|  +----------------------------------------------------+  |
|                                                          |
|  DESTINATION: text field + folder button                  |
|                                                          |
|  +----------------------------------------------------+  |
|  |  PREVIEW TABLE: parsed transactions (scrollable)    |  |
|  |  Data Balancete | Historico | Débito | Créd | Soma  |  |
|  +----------------------------------------------------+  |
|                                                          |
|  [ Convert (N arquivos) ]                                |
|                                                          |
|  FEEDBACK BAR: success/error/status                      |
+----------------------------------------------------------+
```

## Interaction Design

### Drag-and-Drop Zone
- Accepts `.ofx`/`.OFX` files via drag-and-drop or click-to-browse
- Hover state: purple dashed border + light purple tint background
- Shows compact file list with individual remove ("x") buttons
- File count badge: "N arquivos selecionados"
- Rejects non-OFX files with inline error

### Destination Folder
- Single row: read-only text field + folder icon button
- Subtle checkmark when valid folder selected

### Convert Button
- Disabled (grayed) when no files or no destination
- Shows file count when ready: "Converter (N arquivos)"
- Loading state: spinner + "Convertendo..." text
- Purple background, white text, 12px border radius

### Feedback Bar (replaces Alert popups)
- Slim bar at bottom, slides in
- Success: green — "✓ N de N arquivos convertidos" + clickable "Abrir pasta"
- Error: red — "✗ Erro em N de M arquivos" + expandable details
- Auto-hides after 10s, dismissible with X

## Transaction Preview Table

- Auto-populates by parsing first selected OFX file
- Multiple files: dropdown/tabs to switch between previews
- Columns: Data Balancete, Historico, Débito, Crédito, Soma
- Monospace numbers, right-aligned monetary columns
- Striped rows (white / #F9FAFB alternating)
- Max height ~180px with vertical scroll
- Card header shows "N transações"
- Parse errors shown inline (not crash)

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+O | Open file picker |
| Ctrl+S | Open directory picker |
| Enter | Trigger conversion |
| Esc / Ctrl+W | Close application |
| Ctrl+L | Clear files and reset |

## Accessibility

- Focus indicators (blue outline) on all interactive elements
- Tooltips showing keyboard shortcuts on hover
- Mnemonics on labels (underlined letter)

## Technical Constraints

- JavaFX 25 CSS (no external libraries)
- Controller test API preserved: `setSelectedFiles()`, `setOutputDir()`, `getLastAlertMessage()`
- Service layer (`OfxParser`, `ExcelWriter`) unchanged
- Single FXML file + one CSS file
- Icons: Unicode emoji or SVG (no external icon library dependency)

## What Stays the Same

- Single-screen, no navigation
- Core conversion pipeline (OfxParser → ExcelWriter)
- Service layer architecture
- Existing tests continue to pass (updated for new IDs)
