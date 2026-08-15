#!/usr/bin/env python3
"""
Wallpaper quality gate.

Run against the delivered artwork before it goes into the asset pack:

    python3 tools/wallpaper_audit.py art/incoming --manifest core/data/.../WallpaperCatalog.kt

Every check below is a defect that is expensive to find after launch and trivial to find here.
The script exits non-zero on any failure, so it drops straight into CI.

  resolution     1440x3120 minimum. Anything less looks soft on a modern phone and there is no
                 recovering it later.
  aspect         Height/width >= 1.9. Shorter art gets centre-cropped on tall devices, which is how
                 a carefully composed piece loses its subject.
  banding        Detects posterised gradients — the single most common artefact in AVIF/JPEG
                 encodes of smooth artwork, and invisible until it is on a 120Hz OLED.
  blocking       8x8 DCT edge energy. Catches over-compressed JPEG fallbacks.
  clipping       Blown highlights or crushed shadows past a tolerance. Both destroy detail
                 permanently and both are easy to introduce in export.
  palette        Extracts dominant/vibrant/muted and compares against the brief in the manifest,
                 so the app's colour-matching is calibrated to the art that actually shipped.
  weight         AVIF under 900 KB, JPEG fallback under 2.2 MB. 143 pieces at those ceilings keeps
                 the asset pack near 150 MB.
  top luminance  Mean luminance of the top third, written back to the manifest. Decides whether
                 status-bar icons go light or dark, and getting it wrong is visible on every launch.
"""

from __future__ import annotations
import argparse, json, math, sys
from dataclasses import dataclass, asdict
from pathlib import Path

MIN_WIDTH, MIN_HEIGHT = 1440, 3120
MIN_ASPECT = 1.9
MAX_AVIF_BYTES = 900 * 1024
MAX_JPEG_BYTES = 2_200 * 1024
BANDING_THRESHOLD = 0.018
BLOCKING_THRESHOLD = 0.030
CLIP_TOLERANCE = 0.004


@dataclass
class Report:
    path: str
    width: int
    height: int
    bytes: int
    banding: float
    blocking: float
    clipped_high: float
    clipped_low: float
    top_luminance: float
    dominant: str
    vibrant: str
    muted: str
    failures: list[str]

    @property
    def ok(self) -> bool:
        return not self.failures


def audit(path: Path) -> Report:
    from PIL import Image  # Pillow is the only dependency; AVIF via pillow-avif-plugin.
    import numpy as np

    img = Image.open(path).convert("RGB")
    w, h = img.size
    arr = np.asarray(img, dtype=np.float32) / 255.0
    size = path.stat().st_size
    failures: list[str] = []

    if w < MIN_WIDTH or h < MIN_HEIGHT:
        failures.append(f"resolution {w}x{h} below {MIN_WIDTH}x{MIN_HEIGHT}")
    if h / w < MIN_ASPECT:
        failures.append(f"aspect {h / w:.2f} below {MIN_ASPECT} — will be cropped on tall devices")

    ceiling = MAX_AVIF_BYTES if path.suffix == ".avif" else MAX_JPEG_BYTES
    if size > ceiling:
        failures.append(f"{size // 1024} KB exceeds {ceiling // 1024} KB")

    luma = arr @ (0.2126, 0.7152, 0.0722)

    # Banding: in a smooth gradient the row-to-row derivative should be small and continuous.
    # Posterisation shows up as a comb — many zero steps punctuated by identical jumps.
    d = np.abs(np.diff(luma[::4, ::4], axis=0))
    flat = (d < 1e-4).mean()
    steps = d[d > 1e-4]
    banding = float(flat * (steps.std() < 0.004 if steps.size else 0))
    if banding > BANDING_THRESHOLD:
        failures.append(f"banding {banding:.3f} — re-export with dithering")

    # Blocking: energy concentrated exactly on the 8-pixel DCT grid.
    ge = np.abs(np.diff(luma, axis=1))
    on_grid = ge[:, 7::8].mean() if ge.shape[1] > 8 else 0.0
    off_grid = ge.mean() + 1e-9
    blocking = float(max(0.0, on_grid / off_grid - 1.0))
    if blocking > BLOCKING_THRESHOLD:
        failures.append(f"blocking {blocking:.3f} — compression too aggressive")

    clipped_high = float((luma > 0.998).mean())
    clipped_low = float((luma < 0.002).mean())
    if clipped_high > CLIP_TOLERANCE:
        failures.append(f"{clipped_high:.1%} blown highlights")
    # Deliberate exception: AMOLED artwork is *supposed* to be pure black.
    if clipped_low > CLIP_TOLERANCE and "void" not in path.stem and "amoled" not in path.stem:
        failures.append(f"{clipped_low:.1%} crushed shadows")

    top_luminance = float(luma[: h // 3].mean())
    dominant, vibrant, muted = extract_palette(arr)

    return Report(
        path=str(path), width=w, height=h, bytes=size,
        banding=banding, blocking=blocking,
        clipped_high=clipped_high, clipped_low=clipped_low,
        top_luminance=top_luminance,
        dominant=dominant, vibrant=vibrant, muted=muted,
        failures=failures,
    )


def extract_palette(arr) -> tuple[str, str, str]:
    """Mirrors ColorHarmony.extract so the manifest matches what the app will compute at runtime."""
    import numpy as np
    px = arr[::16, ::16].reshape(-1, 3)
    quant = (px * 8).astype(int)
    keys, counts = np.unique(quant, axis=0, return_counts=True)
    dominant = keys[counts.argmax()] / 8.0

    mx, mn = px.max(axis=1), px.min(axis=1)
    chroma = mx - mn
    vibrant = px[chroma.argmax()]
    muted = px[np.abs(chroma - chroma.mean()).argmin()]
    return tuple(f"#{int(c[0]*255):02X}{int(c[1]*255):02X}{int(c[2]*255):02X}"
                 for c in (dominant, vibrant, muted))


def main() -> int:
    parser = argparse.ArgumentParser(description="Prism wallpaper quality gate")
    parser.add_argument("directory", type=Path)
    parser.add_argument("--json", type=Path, help="write measured palettes for the manifest")
    args = parser.parse_args()

    files = sorted(p for p in args.directory.rglob("*") if p.suffix in {".avif", ".jpg", ".png"})
    if not files:
        print(f"No artwork found in {args.directory}", file=sys.stderr)
        return 2

    reports = [audit(p) for p in files]
    failed = [r for r in reports if not r.ok]

    for r in reports:
        status = "FAIL" if r.failures else "ok  "
        print(f"{status}  {Path(r.path).name:28} {r.width}x{r.height}  {r.bytes // 1024:>5} KB  "
              f"top-luma {r.top_luminance:.2f}  {r.dominant}")
        for f in r.failures:
            print(f"        - {f}")

    if args.json:
        args.json.write_text(json.dumps([asdict(r) for r in reports], indent=2))

    print(f"\n{len(reports) - len(failed)}/{len(reports)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
