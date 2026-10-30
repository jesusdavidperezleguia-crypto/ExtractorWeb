from PIL import Image
from pathlib import Path

root = Path(r"C:\Users\Public\ExtractorWeb")
res = root / "app" / "src" / "main" / "res"
logo_path = root / "logo_real.png"

original = Image.open(logo_path).convert("RGBA")
size = max(original.width, original.height)

square = Image.new("RGBA", (size, size), (11, 18, 35, 255))
square.alpha_composite(
    original,
    ((size - original.width) // 2, (size - original.height) // 2)
)

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

for folder, pixels in sizes.items():
    out = res / folder
    out.mkdir(parents=True, exist_ok=True)

    icon = square.resize((pixels, pixels), Image.Resampling.LANCZOS)

    for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
        destination = out / name
        if destination.exists():
            icon.save(destination, "WEBP", quality=100, method=6)

canvas_size = 432
foreground = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
inner = square.resize((280, 280), Image.Resampling.LANCZOS)
foreground.alpha_composite(
    inner,
    ((canvas_size - 280) // 2, (canvas_size - 280) // 2)
)

drawable = res / "drawable"
drawable.mkdir(parents=True, exist_ok=True)
foreground.save(drawable / "ic_launcher_foreground.png")

print("Iconos actualizados correctamente.")
