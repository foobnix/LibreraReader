#!/usr/bin/env python3
"""Writes the TIFF fixtures of NativeTiffDeviceTest with libtiff's tiffcp, each beside a PNG of what
libtiff (through Pillow) reads out of it: the pixels the decoder has to agree with.

Run from this folder: python3 make-fixtures.py (needs Pillow and libtiff's tools)."""
import random
import subprocess
from PIL import Image, ImageDraw

random.seed(7)

# An RGB image of an odd size, so strips and tiles do not divide it evenly
W, H = 97, 61
rgb = Image.new("RGB", (W, H))
px = rgb.load()
for y in range(H):
    for x in range(W):
        px[x, y] = (x * 255 // (W - 1), y * 255 // (H - 1), (x * y) % 256)
d = ImageDraw.Draw(rgb)
d.ellipse((10, 8, 50, 40), fill=(250, 20, 30))
d.line((0, 60, 96, 0), fill=(0, 0, 0), width=2)
rgb.save("_rgb.tif")

# A black and white page wider than the longest fax run code, of runs of every length
BW, BH = 2700, 24
bw = Image.new("1", (BW, BH), 1)
bpx = bw.load()
for y in range(BH):
    x = 0
    black = False
    while x < BW:
        run = random.choice([1, 2, 3, 7, 63, 64, 65, 200, 1700, 1800, 2600]) if y % 3 else random.randint(0, 30)
        for i in range(x, min(BW, x + run)):
            bpx[i, y] = 0 if black else 1
        x += run
        black = not black
bw.save("_bw.tif")

gray16 = Image.new("I;16", (W, H))
g = gray16.load()
for y in range(H):
    for x in range(W):
        g[x, y] = (x * 677 + y * 1031) % 65536
gray16.save("_gray16.tif")

rgb.convert("CMYK").save("cmyk.tif")
# a fax as a fax machine writes it, white being zero: Pillow writes black as zero, so the page
# goes in inverted and the tag is turned round after
from PIL import ImageOps
ImageOps.invert(bw.convert("L")).convert("1").save("bw-g4-miniswhite.tif", compression="group4")
subprocess.run(["tiffset", "-s", "262", "0", "bw-g4-miniswhite.tif"], check=True)
rgb.quantize(16).save("palette.tif")
rgba = rgb.convert("RGBA")
rgba.putalpha(Image.linear_gradient("L").resize((W, H)))
rgba.save("rgba-lzw.tif", compression="tiff_lzw")


def tiffcp(src, dst, *opts):
    subprocess.run(["tiffcp", *opts, src, dst], check=True)


tiffcp("_rgb.tif", "rgb-lzw-predictor.tif", "-c", "lzw:2")
tiffcp("_rgb.tif", "rgb-zip-predictor.tif", "-c", "zip:2")
tiffcp("_rgb.tif", "rgb-planar-lzw.tif", "-p", "separate", "-c", "lzw")
tiffcp("_rgb.tif", "rgb-tiled-packbits.tif", "-t", "-w", "32", "-l", "32", "-c", "packbits")
tiffcp("_rgb.tif", "rgb-jpeg.tif", "-c", "jpeg", "-r", "16")
tiffcp("_rgb.tif", "rgb-tiled-jpeg.tif", "-t", "-w", "32", "-l", "32", "-c", "jpeg")
tiffcp("_rgb.tif", "rgb-bigtiff.tif", "-8", "-c", "lzw")
tiffcp("_gray16.tif", "gray16-lzw-predictor.tif", "-c", "lzw:2")
tiffcp("_bw.tif", "bw-g3-1d.tif", "-c", "g3:1d")
tiffcp("_bw.tif", "bw-g3-2d-fill.tif", "-c", "g3:2d:fill", "-r", "5")
tiffcp("_bw.tif", "bw-g4.tif", "-c", "g4")
tiffcp("_bw.tif", "bw-g4-lsb2msb.tif", "-c", "g4", "-f", "lsb2msb")

# A book of three pages, with a thumbnail of the first between them: a reduced copy, not a page
rgb.resize((24, 15)).save("_thumb.tif")
subprocess.run(["tiffset", "-s", "254", "1", "_thumb.tif"], check=True)
subprocess.run(["tiffcp", "bw-g4.tif", "_thumb.tif", "rgb-lzw-predictor.tif", "rgb-jpeg.tif", "book.tif"], check=True)

# 12 bits a sample, uncompressed, written by hand - Pillow does not write 12 bits: libtiff reads
# it, but its RGBA interface does not draw it, so this one stands for a TIFF Librera cannot show.
# It has no PNG beside it.
import struct
GW, GH = 97, 61
rows = b""
for y in range(GH):
    acc = n = 0
    row = bytearray()
    for x in range(GW):
        acc = (acc << 12) | (((x * 4095) // (GW - 1) ^ (y * 37)) & 0xFFF)
        n += 12
        while n >= 8:
            n -= 8
            row.append((acc >> n) & 0xFF)
    if n:
        row.append((acc << (8 - n)) & 0xFF)
    rows += bytes(row)
tags = [(256, 3, GW), (257, 3, GH), (258, 3, 12), (259, 3, 1), (262, 3, 1), (273, 4, 8),
        (277, 3, 1), (278, 3, GH), (279, 4, len(rows))]
ifd = struct.pack("<H", len(tags))
for tag, kind, value in tags:
    ifd += struct.pack("<HHI", tag, kind, 1)
    ifd += struct.pack("<HH", value, 0) if kind == 3 else struct.pack("<I", value)
ifd += struct.pack("<I", 0)
with open("gray12.tif", "wb") as f:
    f.write(b"II*\x00" + struct.pack("<I", 8 + len(rows)) + rows + ifd)

import glob
import os
for f in glob.glob("*.tif"):
    if f.startswith("_") or f in ("book.tif", "gray12.tif"):
        if f.startswith("_"):
            os.remove(f)
        continue
    im = Image.open(f)
    if im.mode == "I;16":
        # what an 8 bit screen shows of 16 bits: the high byte
        im = im.point(lambda v: v / 256).convert("L")
    im.convert("RGBA").save(f[:-4] + ".png")
