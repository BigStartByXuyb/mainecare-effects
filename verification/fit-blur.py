import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter

photo = np.asarray(Image.open('demo/src/main/assets/backdrop.png').convert('RGB'), dtype=np.float64) / 255.0
web = np.asarray(Image.open('verification/results/ws-warp-photo-d0.png').convert('RGB'), dtype=np.float64) / 255.0
H, W = photo.shape[:2]

# Crop-pixel -> CSS stage -> warp image fraction (card warp box at stage 25,121,
# background-position -24,-131.35, image 390x866.6667 CSS).
def img_coords(X, Y):
    u = (X - 63) / 2.625
    v = (Y - 315) / 2.625
    fx = (23.0 + u) / 390.0
    fy = (130.35 + v) / 866.6666667
    return fx * (W - 1), fy * (H - 1)

xs = np.arange(400, 600)
ys = np.arange(450, 650)
gx, gy = np.meshgrid(xs, ys)
ix, iy = img_coords(gx, gy)
ix = np.clip(ix, 0, W - 1); iy = np.clip(iy, 0, H - 1)
x0 = np.floor(ix).astype(int); y0 = np.floor(iy).astype(int)
x1 = np.minimum(x0 + 1, W - 1); y1 = np.minimum(y0 + 1, H - 1)
tx = (ix - x0)[..., None]; ty = (iy - y0)[..., None]
target = web[ys[0]:ys[-1] + 1, xs[0]:xs[-1] + 1]

def to_linear(c):
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
def to_srgb(c):
    c = np.clip(c, 0, 1)
    return np.where(c <= 0.0031308, c * 12.92, 1.055 * c ** (1 / 2.4) - 0.055)

def sample(img):
    a = img[y0, x0] * (1 - tx) * (1 - ty) + img[y0, x1] * tx * (1 - ty)
    b = img[y1, x0] * (1 - tx) * ty + img[y1, x1] * tx * ty
    return a + b

for space in ('srgb', 'linear'):
    src = photo if space == 'srgb' else to_linear(photo)
    for sigma_css in (20, 24, 26, 28, 30, 34, 38, 44):
        sigma = sigma_css * (W / 390.0)
        blurred = gaussian_filter(src, sigma=(sigma, sigma, 0), mode='nearest')
        out = blurred if space == 'srgb' else to_srgb(blurred)
        pred = sample(out)
        mae = np.abs(pred - target).mean()
        print(f'{space} sigma_css={sigma_css:>4} mae={mae:.3f}')
