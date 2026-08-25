#!/usr/bin/env python3
"""
ShadowRay VPN — Access Key Generator (کلید ورود)
=================================================
Usage:
  python3 tools/gen_key.py "vless://uuid@host:port?...#name"
  python3 tools/gen_key.py configs.txt        (one URI per line, or a sub body)

Prints an SR-XXXXX.XXXXX.XXXXX key to give to the user.
In the app: Configs → + → کلید ورود → paste → فعال‌سازی

Format: XOR each payload byte with 0x5A, Base32-encode (A-Z, 2-7 — no
confusable chars), group by 5 with dots. Case-insensitive when typed.
"""
import base64, sys, os

MAGIC = 0x5A


def encode_key(payload: str) -> str:
    data = payload.encode("utf-8")
    xored = bytes(b ^ MAGIC for b in data)
    b32 = base64.b32encode(xored).decode("ascii").rstrip("=")
    return "SR-" + ".".join(b32[i:i + 5] for i in range(0, len(b32), 5))


def decode_key(key: str) -> str:
    body = key.strip().upper()
    if body.startswith("SR-"):
        body = body[3:]
    b32 = "".join(c for c in body if c not in ". -_")
    if any(c in "0189" for c in b32):
        raise ValueError("invalid character in key (0,1,8,9 are never used)")
    b32 += "=" * (-len(b32) % 8)
    xored = base64.b32decode(b32)
    return bytes(b ^ MAGIC for b in xored).decode("utf-8")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(0)

    arg = sys.argv[1]
    payload = open(arg).read() if os.path.exists(arg) else arg

    key = encode_key(payload.strip())
    assert decode_key(key) == payload.strip(), "roundtrip failed"

    print()
    print("  ┌─────────────────────────────────────┐")
    print("   ShadowRay Access Key")
    print("  └─────────────────────────────────────┘")
    print()
    # wrap the key nicely at ~40 chars per line
    for i in range(0, len(key), 44):
        print("   " + key[i:i + 44])
    print()
    preview = payload.strip().replace("\n", " ⏎ ")
    print("   Payload:", preview[:70] + ("…" if len(preview) > 70 else ""))
    print(f"   Length : {len(payload.strip())} chars")
