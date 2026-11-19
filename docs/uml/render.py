#!/usr/bin/env python3
"""Render các file .puml thành PNG.

Cách dùng:  python docs/uml/render.py [--out docs/uml/png] [file.puml ...]
 - Nếu có plantuml.jar (biến môi trường PLANTUML_JAR hoặc tools/plantuml.jar) thì render offline.
 - Ngược lại gọi server PlantUML (--server, mặc định https://www.plantuml.com/plantuml).
Dòng `!include _style.puml` được nhúng nội dung trước khi render.
"""
import argparse, os, re, subprocess, sys, urllib.request, urllib.error, zlib, pathlib

HERE = pathlib.Path(__file__).resolve().parent
ALPHA = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"

def _b64(data: bytes) -> str:
    out = []
    for i in range(0, len(data), 3):
        b = data[i:i + 3] + b"\0" * (3 - len(data[i:i + 3]))
        n = (b[0] << 16) | (b[1] << 8) | b[2]
        out += [ALPHA[(n >> 18) & 63], ALPHA[(n >> 12) & 63], ALPHA[(n >> 6) & 63], ALPHA[n & 63]]
    return "".join(out)

def encode(text: str) -> str:
    c = zlib.compressobj(9, zlib.DEFLATED, -15)
    return _b64(c.compress(text.encode("utf-8")) + c.flush())

def inline(path: pathlib.Path) -> str:
    text = path.read_text(encoding="utf-8")
    def rep(m):
        return (path.parent / m.group(1)).resolve().read_text(encoding="utf-8") if (path.parent / m.group(1)).exists() \
            else (HERE / m.group(1)).read_text(encoding="utf-8")
    return re.sub(r"^!include\s+(\S+\.puml)\s*$", rep, text, flags=re.M)

def render_server(text, server):
    # POST không có trên server công cộng cho PNG, dùng GET với mã hóa deflate
    url = f"{server}/png/{encode(text)}"
    req = urllib.request.Request(url, headers={"User-Agent": "uml-render"})
    import time
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return r.read()
        except urllib.error.HTTPError:
            break
        except Exception:
            if attempt == 3: raise
            time.sleep(2)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.read()
    except urllib.error.HTTPError as e:
        raise RuntimeError("PlantUML lỗi: %s (dòng %s)" % (e.headers.get("X-PlantUML-Diagram-Error"),
                           e.headers.get("X-PlantUML-Diagram-Error-Line")))

def main():
    sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser()
    ap.add_argument("files", nargs="*")
    ap.add_argument("--out", default=str(HERE / "png"))
    ap.add_argument("--server", default="https://www.plantuml.com/plantuml")
    a = ap.parse_args()
    files = [pathlib.Path(f) for f in a.files] or [p for p in HERE.rglob("fig*.puml")]
    out = pathlib.Path(a.out); out.mkdir(parents=True, exist_ok=True)
    jar = os.environ.get("PLANTUML_JAR") or str(HERE.parent.parent / "tools" / "plantuml.jar")
    from concurrent.futures import ThreadPoolExecutor
    def one(f):
        text = inline(f)
        dst = out / (f.stem + ".png")
        try:
            if os.path.exists(jar):
                r = subprocess.run(["java", "-jar", jar, "-charset", "UTF-8", "-pipe", "-tpng"],
                                   input=text.encode("utf-8"), capture_output=True, check=True)
                dst.write_bytes(r.stdout)
            else:
                dst.write_bytes(render_server(text, a.server))
            return "OK  %s" % dst
        except Exception as e:
            return "ERR %s %s" % (f, e)
    with ThreadPoolExecutor(4) as ex:
        res = list(ex.map(one, sorted(files)))
    print(chr(10).join(res))
    sys.exit(0 if all(r.startswith("OK") for r in res) else 1)

if __name__ == "__main__":
    main()
