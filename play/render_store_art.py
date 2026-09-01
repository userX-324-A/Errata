"""Render Play icon, feature graphic, and legacy launcher mipmaps.

Geometry matches drawable/ic_launcher_foreground.xml (108 viewport).
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
PAPER = (0xF4, 0xEF, 0xE6, 255)
# Darker than in-app paper so the launcher tile reads on a light wallpaper.
PLATE = (0xE0, 0xD2, 0xBE, 255)
TERRACOTTA = (0xB8, 0x5C, 0x38, 255)
INK = (0x1C, 0x19, 0x14, 255)
MUTED = (0x5C, 0x56, 0x4C, 255)

CARET = [(54, 28), (80, 68), (70, 68), (54, 42), (38, 68), (28, 68)]
UNDER = [(40, 74), (68, 74), (68, 80), (40, 80)]
VIEW = 108.0

MIPMAPS = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def _scale(points: list[tuple[int, int]], size: int) -> list[tuple[float, float]]:
    s = size / VIEW
    return [(x * s, y * s) for x, y in points]


def draw_mark(draw: ImageDraw.ImageDraw, size: int, color: tuple[int, int, int, int]) -> None:
    draw.polygon(_scale(CARET, size), fill=color)
    draw.polygon(_scale(UNDER, size), fill=color)


def square_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), PLATE)
    draw_mark(ImageDraw.Draw(img), size, TERRACOTTA)
    return img


def mark_only(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_mark(ImageDraw.Draw(img), size, TERRACOTTA)
    return img


def round_icon(size: int) -> Image.Image:
    square = square_icon(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(square, mask=mask)
    return out


def render_hires(size: int, round_clip: bool = False) -> Image.Image:
    supersample = size * 4
    src = round_icon(supersample) if round_clip else square_icon(supersample)
    return src.resize((size, size), Image.Resampling.LANCZOS)


def feature_graphic() -> Image.Image:
    w, h = 1024, 500
    img = Image.new("RGBA", (w, h), PAPER)
    draw = ImageDraw.Draw(img)
    fonts = ROOT / "app" / "src" / "main" / "res" / "font"
    title = ImageFont.truetype(str(fonts / "fraunces_semibold.ttf"), 92)
    tag = ImageFont.truetype(str(fonts / "atkinson_regular.ttf"), 28)

    mark = 168
    mark_img = mark_only(mark)
    title_text = "Errata"
    tag_text = "Recurring upkeep. On this device."
    title_bbox = draw.textbbox((0, 0), title_text, font=title)
    tag_bbox = draw.textbbox((0, 0), tag_text, font=tag)
    title_w, title_h = title_bbox[2] - title_bbox[0], title_bbox[3] - title_bbox[1]
    tag_w = tag_bbox[2] - tag_bbox[0]
    gap = 28
    text_w = max(title_w, tag_w)
    cluster_w = mark + gap + text_w
    cluster_h = max(mark, title_h + 18 + (tag_bbox[3] - tag_bbox[1]))
    left = (w - cluster_w) // 2
    top = (h - cluster_h) // 2
    img.paste(mark_img, (left, top + (cluster_h - mark) // 2), mark_img)

    text_x = left + mark + gap
    title_y = top + (cluster_h - (title_h + 18 + (tag_bbox[3] - tag_bbox[1]))) // 2 - title_bbox[1]
    draw.text((text_x, title_y), title_text, font=title, fill=INK)
    tag_y = title_y + title_bbox[3] + 14
    draw.text((text_x, tag_y), tag_text, font=tag, fill=MUTED)
    return img


def main() -> None:
    play = ROOT / "play"
    play.mkdir(exist_ok=True)
    render_hires(512).save(play / "icon-512.png")
    feature_graphic().save(play / "feature-graphic-1024x500.png")

    res = ROOT / "app" / "src" / "main" / "res"
    for density, size in MIPMAPS.items():
        folder = res / f"mipmap-{density}"
        folder.mkdir(exist_ok=True)
        render_hires(size).save(folder / "ic_launcher.png")
        render_hires(size, round_clip=True).save(folder / "ic_launcher_round.png")
    print("wrote play icon, feature graphic, and launcher mipmaps")


if __name__ == "__main__":
    main()
