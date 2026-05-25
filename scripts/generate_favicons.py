from PIL import Image
import sys

src = "src/main/resources/static/images/logo.png"
if len(sys.argv) > 1:
    src = sys.argv[1]

out_dir = "src/main/resources/static"

sizes = [16, 32, 48, 64, 128, 256]

im = Image.open(src).convert("RGBA")

for s in sizes:
    im2 = im.resize((s, s), Image.LANCZOS)
    out_png = f"{out_dir}/favicon-{s}.png"
    im2.save(out_png)
    print("Saved", out_png)

# save ico with multiple sizes
ico_sizes = [(16,16),(32,32),(48,48),(64,64)]
ico_path = f"{out_dir}/favicon.ico"
im.save(ico_path, sizes=[(w,h) for w,h in ico_sizes])
print("Saved", ico_path)
