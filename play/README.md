# Play listing assets

Do not commit screenshots that show real task titles. Capture on a throwaway list if you keep PNGs in git.

| File | Use |
|---|---|
| `icon-512.png` | Play high-res icon |
| `feature-graphic-1024x500.png` | Play feature graphic |

## Screenshots to capture (phone + tablet)

1. Pending list with a few due / overdue items (fake titles)
2. Task editor (cadence + minutes)
3. Settings (Google row unlinked is fine)
4. Free-window / minutes chips if they read well

Phone: at least two, JPEG or 24-bit PNG, 320–3840px on each side.  
Tablet (this device is fine): 7" and/or 10" slot in Console.

On the attached tablet:

```bat
adb -s 461a0663 exec-out screencap -p > play\tablet-1.png
```
