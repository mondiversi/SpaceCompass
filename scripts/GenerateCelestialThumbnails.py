"""Build external reference previews; source maps and old release files stay unchanged.

Requires Pillow. Run from the repository root. --check validates reproducibility
without changing the pack. The globe shader matches the Android reference preview;
the Moon's time-dependent phase remains an application overlay, never baked in.
"""
import argparse
import hashlib
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

EXTENT = 128
GLOBES = {
    "sun.webp", "moon.webp", "mercury.webp", "venus_atmosphere.webp", "mars.webp",
    "jupiter.webp", "saturn.webp", "uranus.webp", "neptune.webp", "pluto.webp",
    "io_2048.webp", "europa_2048.webp", "titan.webp", "enceladus.webp", "lv426.webp",
}
ZERO_OFFSET = {"pluto.webp", "europa_2048.webp", "titan.webp", "enceladus.webp"}
UNMAPPED = {"pluto.webp", "europa_2048.webp"}


def preview(source: Path, logical_name: str | None = None) -> Image.Image:
    name = logical_name or source.name
    with Image.open(source) as original:
        image = original.convert("RGB")
    if name not in GLOBES:
        side = min(image.size)
        left, top = (image.width - side) // 2, (image.height - side) // 2
        diameter = EXTENT * 7 // 8
        photo = image.crop((left, top, left + side, top + side)).resize(
            (diameter, diameter), Image.Resampling.LANCZOS)
        output = Image.new("RGBA", (EXTENT, EXTENT))
        output.paste(photo, ((EXTENT - diameter) // 2,) * 2)
        mask = Image.new("L", (EXTENT * 4, EXTENT * 4))
        margin = EXTENT / 16
        ImageDraw.Draw(mask).ellipse((margin * 4, margin * 4,
            (EXTENT - margin) * 4, (EXTENT - margin) * 4), fill=255)
        output.putalpha(mask.resize(output.size, Image.Resampling.LANCZOS))
        return output
    image = image.resize((512, 256), Image.Resampling.LANCZOS)
    output = Image.new("RGBA", (EXTENT, EXTENT))
    pixels = output.load()
    radius = EXTENT * (17 if name == "saturn.webp" else 28) / 64
    offset = 0 if name in ZERO_OFFSET else .5
    for y in range(EXTENT):
        for x in range(EXTENT):
            nx, ny = (x + .5 - EXTENT / 2) / radius, (EXTENT / 2 - y - .5) / radius
            squared = nx * nx + ny * ny
            if squared > 1:
                if name == "saturn.webp" and 1.20 <= nx * nx + (ny / .28) ** 2 <= 3.10:
                    pixels[x, y] = (184, 167, 139, 255)
                continue
            nz = math.sqrt(1 - squared)
            u = (math.atan2(nz, nx) / (2 * math.pi) + offset) % 1
            v = math.acos(max(-1, min(1, ny))) / math.pi
            rgb = image.getpixel((min(image.width - 1, int(u * image.width)),
                min(image.height - 1, int(v * image.height))))
            if name in UNMAPPED and rgb == (0, 0, 0):
                rgb = (56, 51, 46)
            light = .7 + .3 * nz if name == "sun.webp" else \
                .25 + .75 * max(0, (-.4 * nx + .3 * ny + nz) / math.sqrt(1.25))
            pixels[x, y] = tuple(int(value * light) for value in rgb) + (255,)
    return output


def check_canonical_pack(pack: Path, manifest: dict):
    """Validate existing independent previews without invalidating published pin/URL metadata."""
    import io
    by_parent = {item["parent_image"]: item for item in manifest["thumbnails"]}
    for parent in manifest["images"]:
        source = pack / parent["name"]
        assert hashlib.sha256(source.read_bytes()).hexdigest() == parent["sha256"], source.name
        record = by_parent[parent["name"]]
        assert record["name"] == "thumb_" + parent["name"]
        output = io.BytesIO()
        preview(source, parent.get("legacy_name")).save(output, "WEBP", quality=90, method=6)
        actual = (pack / record["name"]).read_bytes()
        assert actual == output.getvalue(), record["name"]
        assert len(actual) == record["bytes"]
        assert hashlib.sha256(actual).hexdigest() == record["sha256"]
        assert parent["quality"] == record["quality"] == 90
        assert parent["method"] == record["method"] == 6
    print("Verified reproducible canonical WebP Q90/method6 previews:", len(by_parent))


def main():
    import io
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, default=Path("textures/celestial-textures-v1.6"))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    manifest_path = args.pack / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schema", 1) >= 2:
        check_canonical_pack(args.pack, manifest)
        return
    previous = {t["name"]: t for t in manifest.get("thumbnails", [])}
    thumbnails = []
    for parent in manifest["images"]:
        source = args.pack / parent["name"]
        assert hashlib.sha256(source.read_bytes()).hexdigest() == parent["sha256"], source.name
        data = io.BytesIO()
        preview(source).save(data, format="WEBP", quality=90, method=6)
        content = data.getvalue()
        name = "thumb_" + source.name
        item = dict(parent_image=source.name, name=name, bytes=len(content),
            sha256=hashlib.sha256(content).hexdigest(), width=EXTENT, height=EXTENT,
            quality=90, method=6, download_pack=None,
            render_mode="globe" if source.name in GLOBES else "photograph",
            source_sha256=parent["sha256"],
            processing="Reference miniature derived from the credited parent image; "
                "dynamic lunar phase is applied in the app.")
        if previous.get(name, {}).get("sha256") == item["sha256"]:
            item["download_pack"] = previous[name].get("download_pack")
        for key in ("credit", "license", "license_url", "source_page", "source_image"):
            if key in parent:
                item[key] = parent[key]
        thumbnails.append(item)
        if args.check:
            assert (args.pack / name).read_bytes() == content, name
        else:
            (args.pack / name).write_bytes(content)
    policy = """package me.mondiversi.spacecompass

/** Generated by scripts/GenerateCelestialThumbnails.py. Reference previews have independent pins.
 * Individual URLs and integrity pins belong to the versioned external graphics release. */
internal object SpaceCompassCelestialThumbnailPolicy {
    val images = listOf(
"""
    entries = []
    for t in thumbnails:
        download = "null" if t["download_pack"] is None else json.dumps(t["download_pack"])
        entries.append(f'        SpaceCompassCelestialTexture("{t["name"]}", {t["bytes"]}, '
            f'"{t["sha256"]}", 128, 128, downloadPack = {download})')
    policy += ",\n".join(entries) + """
    )
    private val byParent = images.associateBy { it.name.removePrefix("thumb_") }
    fun forImage(name: String?) = byParent[name]
}
"""
    policy_path = Path(__file__).resolve().parents[1] / "app/src/main/java/me/mondiversi/spacecompass/SpaceCompassCelestialThumbnailPolicy.kt"
    if args.check:
        assert manifest.get("thumbnails") == thumbnails
        # A historical pack can be reproduced without replacing the current APK's newer index.
        current_pack_policy = policy_path.with_name("SpaceCompassCelestialTexturePolicy.kt").read_text(encoding="utf-8")
        if f'const val PACK = "{args.pack.name}"' in current_pack_policy:
            assert policy_path.read_text(encoding="utf-8") == policy
    else:
        policy_path.write_text(policy, encoding="utf-8")
        manifest["thumbnails"] = thumbnails
        manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        credits_path = args.pack / "CREDITS.md"
        credits = credits_path.read_text(encoding="utf-8").split("\n## Reference thumbnails\n")[0].rstrip()
        credits += "\n\n## Reference thumbnails\n\n128 x 128 WebP, quality 90, method 6. " \
            "Each preview retains its parent image's authors, source and licence. " \
            "Globe previews are rendered reference views; the Moon's phase is applied at runtime.\n\n"
        credits += "\n".join(f"- `{t['name']}`: derivative of `{t['parent_image']}`; {t.get('credit', '')}."
            for t in thumbnails) + "\n"
        credits_path.write_text(credits, encoding="utf-8")
        checksums = "".join(f"{hashlib.sha256(f.read_bytes()).hexdigest()}  {f.name}\n"
            for f in sorted(args.pack.iterdir()) if f.is_file() and f.name != "SHA256SUMS.txt")
        (args.pack / "SHA256SUMS.txt").write_text(checksums, encoding="utf-8")
    print(json.dumps({"thumbnails": len(thumbnails), "bytes": sum(t["bytes"] for t in thumbnails),
        "check_only": args.check}))


if __name__ == "__main__":
    main()
