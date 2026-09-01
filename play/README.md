# Play listing assets

Do not commit screenshots that show real task titles. Capture on a throwaway list if you keep PNGs in git.

Listing theme is **Light** (paper/ink). The high-res icon is a terracotta proof mark on a slightly darker paper tile — not a moss check.

| File | Use |
|---|---|
| `icon-512.png` | Play high-res icon |
| `feature-graphic-1024x500.png` | Play feature graphic (Fraunces wordmark + same mark) |

Regenerate icon, feature graphic, and legacy mipmaps:

```bat
python play\render_store_art.py
```

## Screenshots to capture (phone + tablet)

Use a throwaway pin list. Current UI to show:

1. Pending list with a few due / overdue items and free-window chips (15 / 30 / 45)
2. Task editor (cadence + minutes chips)
3. Starter catalog or empty-state pin (checkboxes + tap-a-row)
4. Settings (Google row unlinked is fine)
5. Tablet two-pane: list beside the editor (medium/expanded, nav rail)

Phone: at least two, JPEG or 24-bit PNG, 320–3840px on each side.  
Tablet: 7" and/or 10" slot in Console. Upload steps: [`docs/08-publish.md`](../docs/08-publish.md).

On the attached tablet:

```bat
adb -s 461a0663 exec-out screencap -p > play\tablet-1.png
```
