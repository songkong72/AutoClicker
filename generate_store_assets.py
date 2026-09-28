import os
from PIL import Image, ImageDraw, ImageFont

def create_store_assets():
    os.makedirs(r"E:\project\workspace\AutoClicker\store_assets", exist_ok=True)
    
    # -------------------------------------------------------------
    # 1. App Icon (512 x 512 px)
    # -------------------------------------------------------------
    icon_size = 512
    img_icon = Image.new("RGBA", (icon_size, icon_size), (15, 23, 42, 255)) # #0F172A
    draw = ImageDraw.Draw(img_icon)
    
    # Rounded corners background
    # Outer subtle glow ring
    cx, cy = 256, 256
    
    # Background soft circle
    draw.ellipse([cx - 210, cy - 210, cx + 210, cy + 210], fill=(30, 41, 59, 255)) # #1E293B
    draw.ellipse([cx - 190, cy - 190, cx + 190, cy + 190], fill=(15, 23, 42, 255)) # #0F172A
    
    # Main Target Neon Blue Ring
    draw.ellipse([cx - 160, cy - 160, cx + 160, cy + 160], outline=(37, 99, 235, 255), width=16) # #2563EB
    draw.ellipse([cx - 156, cy - 156, cx + 156, cy + 156], outline=(56, 189, 248, 255), width=6)  # #38BDF8 glow
    
    # Inner White Precision Ring
    draw.ellipse([cx - 90, cy - 90, cx + 90, cy + 90], outline=(255, 255, 255, 220), width=8)
    
    # Crosshair lines (Precision 4-way)
    ch_len = 50
    w = 8
    # Top
    draw.line([cx, cy - 160 + 10, cx, cy - 90 - 10], fill=(255, 255, 255, 255), width=w)
    # Bottom
    draw.line([cx, cy + 90 + 10, cx, cy + 160 - 10], fill=(255, 255, 255, 255), width=w)
    # Left
    draw.line([cx - 160 + 10, cy, cx - 90 - 10, cy], fill=(255, 255, 255, 255), width=w)
    # Right
    draw.line([cx + 90 + 10, cy, cx + 160 - 10, cy], fill=(255, 255, 255, 255), width=w)
    
    # Outer tick marks
    draw.line([cx, 35, cx, 65], fill=(56, 189, 248, 255), width=6)
    draw.line([cx, 512 - 65, cx, 512 - 35], fill=(56, 189, 248, 255), width=6)
    draw.line([35, cy, 65, cy], fill=(56, 189, 248, 255), width=6)
    draw.line([512 - 65, cy, 512 - 35, cy], fill=(56, 189, 248, 255), width=6)
    
    # Center Crimson Power Dot
    draw.ellipse([cx - 32, cy - 32, cx + 32, cy + 32], fill=(239, 68, 68, 255)) # #EF4444
    draw.ellipse([cx - 12, cy - 12, cx + 12, cy + 12], fill=(255, 255, 255, 255)) # White specular highlight
    
    icon_path = r"E:\project\workspace\AutoClicker\store_assets\app_icon_512.png"
    img_icon.save(icon_path, "PNG")
    print(f"Created app icon: {icon_path}")
    
    # Also save to android mipmap/drawable for actual app icon!
    res_icon_path = r"E:\project\workspace\AutoClicker\android-app\app\src\main\res\drawable\ic_store_icon.png"
    img_icon.save(res_icon_path, "PNG")
    
    # -------------------------------------------------------------
    # 2. Feature Graphic (1024 x 500 px)
    # -------------------------------------------------------------
    fw, fh = 1024, 500
    img_feat = Image.new("RGBA", (fw, fh), (15, 23, 42, 255))
    draw_f = ImageDraw.Draw(img_feat)
    
    # Modern gradient/accent background elements
    draw_f.ellipse([800, -100, 1200, 300], fill=(30, 58, 138, 120)) # Deep blue ambient glow
    draw_f.ellipse([-100, 250, 400, 750], fill=(30, 41, 59, 150))
    
    # Draw mini target graphic on right side
    tcx, tcy = 760, 250
    draw_f.ellipse([tcx - 140, tcy - 140, tcx + 140, tcy + 140], fill=(30, 41, 59, 200))
    draw_f.ellipse([tcx - 110, tcy - 110, tcx + 110, tcy + 110], outline=(37, 99, 235, 255), width=10)
    draw_f.ellipse([tcx - 60, tcy - 60, tcx + 60, tcy + 60], outline=(255, 255, 255, 220), width=6)
    draw_f.line([tcx, tcy - 110 + 8, tcx, tcy - 60 - 8], fill=(255, 255, 255, 255), width=6)
    draw_f.line([tcx, tcy + 60 + 8, tcx, tcy + 110 - 8], fill=(255, 255, 255, 255), width=6)
    draw_f.line([tcx - 110 + 8, tcy, tcx - 60 - 8, tcy], fill=(255, 255, 255, 255), width=6)
    draw_f.line([tcx + 60 + 8, tcy, tcx + 110 - 8, tcy], fill=(255, 255, 255, 255), width=6)
    draw_f.ellipse([tcx - 22, tcy - 22, tcx + 22, tcy + 22], fill=(239, 68, 68, 255))
    draw_f.ellipse([tcx - 8, tcy - 8, tcx + 8, tcy + 8], fill=(255, 255, 255, 255))
    
    # Left Side Typography
    # Try using system font or default
    try:
        font_badge = ImageFont.truetype("malgun.ttf", 22)
        font_title = ImageFont.truetype("malgunbd.ttf", 52)
        font_sub = ImageFont.truetype("malgun.ttf", 26)
        font_tag = ImageFont.truetype("malgun.ttf", 20)
    except:
        font_badge = font_title = font_sub = font_tag = ImageFont.load_default()
        
    # Badge
    draw_f.rounded_rectangle([90, 85, 300, 125], radius=8, fill=(37, 99, 235, 255))
    draw_f.text((105, 93), "⚡ 모바일 게임 전용", fill=(255, 255, 255, 255), font=font_badge)
    
    # Title
    draw_f.text((90, 150), "오토클리커 Pro", fill=(255, 255, 255, 255), font=font_title)
    
    # Subtitle
    draw_f.text((90, 230), "초간편 치료 연타 & 스마트 매크로", fill=(56, 189, 248, 255), font=font_sub)
    
    # Bullet points
    bullets = [
        "✓ 루팅 불필요 · 원스위치 초간편 권한",
        "✓ 100% 무한 연타 · 볼륨키 긴급 정지",
        "✓ 독립 분리형 과녁 & 컨트롤 바"
    ]
    by = 300
    for b in bullets:
        draw_f.text((90, by), b, fill=(203, 213, 225, 255), font=font_tag)
        by += 40
        
    feat_path = r"E:\project\workspace\AutoClicker\store_assets\feature_graphic_1024x500.png"
    img_feat.save(feat_path, "PNG")
    print(f"Created feature graphic: {feat_path}")

if __name__ == "__main__":
    create_store_assets()
