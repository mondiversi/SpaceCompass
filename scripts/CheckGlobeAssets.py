from pathlib import Path
import hashlib,json

root=Path(__file__).resolve().parent.parent
for asset,manifest,size_key,hash_key in [
    ("app/src/main/assets/globe/globe-navigation.js","docs/GLOBE_ASSETS.json","bytes","sha256"),
    ("app/src/main/assets/screensaver/mondiversi-space.html","docs/SCREENSAVER_ASSETS.json","renderer_bytes","renderer_sha256")]:
    data=(root/asset).read_bytes()
    pins=json.loads((root/manifest).read_text(encoding="utf-8"))
    assert b"\r\n" not in data, "Renderer assets must use canonical LF line endings: "+asset
    assert len(data)==pins[size_key] and hashlib.sha256(data).hexdigest()==pins[hash_key],asset
print("Verified canonical offline Globe and screensaver renderer integrity pins.")
