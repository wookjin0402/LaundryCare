from fastapi import FastAPI, UploadFile, File
from ultralytics import YOLO
import cv2
import numpy as np

app = FastAPI()

# ---------------------------------------------------------
# 1. AI 모델 로딩
# ---------------------------------------------------------
print("AI 모델 5개 로딩 중...")
clothes_ai = YOLO("models/clothes_model.pt")   
label_ai = YOLO("models/laundry_model.pt")     
main_ai = YOLO("models/laundry.pt")            
stain_ai = YOLO("models/stain_model.pt")        
washer_ai = YOLO("models/washer_model.pt")     
print("로딩 완료!")

# ---------------------------------------------------------
# 2. 클래스 목록 및 번역 사전
# ---------------------------------------------------------
LABEL_CLASSES = [
    "bleach_oxygen", "dry_clean_p", "hand_wash", "iron_low", "iron_medium", 
    "machine_wash_cold", "no_bleach", "no_dry_clean", "no_iron", "no_tumble_dry", 
    "no_wash", "tumble_dry_low", "tumble_dry_medium", "wash_30", "wash_40",
    "dry_clean_petroleum", "dry_flat_shade", "dry_hang", "dry_hang_shade",
    "hand_wash_30", "no_flame", "no_wring", "wash_warm", "dry_clean", "iron",
    "tumble_dry_high", "wring",
    "bleach", "chlorine_bleach", "drip_dry", "drip_dry_in_shade", "dry_clean_a", 
    "dry_clean_any_solvent_except_trichloroethylene_delicate", "dry_clean_low_heat", 
    "dry_clean_no_steam", "dry_clean_petrol_only", "dry_clean_petrol_only_delicate", 
    "dry_clean_petrol_only_very_delicate", "dry_clean_reduced_moisture", 
    "dry_clean_short_cycle", "dry_flat", "dry_flat_in_shade", "iron_high", 
    "line_dry", "line_dry_in_shade", "machine_wash_30", "machine_wash_40", 
    "machine_wash_delicate", "machine_wash_normal", "machine_wash_permanent_press", 
    "natural_dry", "no_dry", "no_steam", "no_wet_clean", "shade_dry", "steam", 
    "tumble_dry_low_delicate", "tumble_dry_no_heat", "tumble_dry_normal", 
    "wash_30_delicate", "wash_30_very_delicate", "wash_40_delicate", 
    "wash_40_very_delicate", "wash_50", "wash_60", "wash_60_delicate", 
    "wash_60_very_delicate", "wash_70", "wash_95", "wash_95_delicate", 
    "wash_95_very_delicate", "wet_clean", "wet_clean_delicate", "wet_clean_very_delicate"
]

CLOTHES_CLASSES = [
    "Dress", "Hoodie", "Pants", "Shirt", "Short", "Skirt", "Sweater", "T-shirt", "Outer", "LongT-shirt"
]

STAIN_CLASSES = ["Stain"]

WASHER_CLASSES = ["Drum_washer", "Top_washer", "Washing-machine"]

CLASS_MAP = {
    # [기존 세탁 기호 매핑]
    "bleach_oxygen": "산소계 표백 가능", "dry_clean": "세탁소 드라이클리닝 필요",
    "dry_clean_p": "드라이클리닝(P)", "hand_wash": "손세탁", "iron": "다림질",
    "iron_low": "다림질(저온)", "iron_medium": "다림질(중온)",
    "machine_wash_cold": "세탁기(찬물)", "no_bleach": "표백 금지",
    "no_dry_clean": "드라이클리닝 금지", "no_iron": "다림질 금지",
    "no_tumble_dry": "기계건조 금지", "no_wash": "세탁 금지",
    "tumble_dry_low": "기계건조(저온)", "tumble_dry_medium": "기계건조(중온)",
    "tumble_dry_high": "기계건조(고온)", "wash_30": "물세탁(30도)", "wash_40": "물세탁(40도)",
    "dry_clean_petroleum": "드라이클리닝(석유계)", "dry_flat_shade": "그늘에 뉘어서",
    "dry_hang": "걸어서 건조", "dry_hang_shade": "그늘에 걸어서",
    "hand_wash_30": "손세탁(30도)", "no_flame": "화기 주의", "no_wring": "짜기 금지",
    "wring": "짜기 가능", "wash_warm": "온수 세탁",
    "bleach": "표백 가능", "chlorine_bleach": "염소계 표백 가능",
    "drip_dry": "물기 있는 채로 걸어서 건조", "drip_dry_in_shade": "그늘에 물기 있는 채로 걸어서 건조",
    "dry_clean_a": "드라이클리닝(A)",
    "dry_clean_any_solvent_except_trichloroethylene_delicate": "드라이클리닝(트리클로로에틸렌 제외, 약하게)",
    "dry_clean_low_heat": "드라이클리닝(저온)", "dry_clean_no_steam": "드라이클리닝(스팀 금지)",
    "dry_clean_petrol_only": "드라이클리닝(석유계)", "dry_clean_petrol_only_delicate": "드라이클리닝(석유계, 약하게)",
    "dry_clean_petrol_only_very_delicate": "드라이클리닝(석유계, 매우 약하게)",
    "dry_clean_reduced_moisture": "드라이클리닝(수분 적게)", "dry_clean_short_cycle": "드라이클리닝(짧은 주기)",
    "dry_flat": "뉘어서 건조", "dry_flat_in_shade": "그늘에 뉘어서 건조",
    "iron_high": "다림질(고온)", "line_dry": "걸어서 건조", "line_dry_in_shade": "그늘에 걸어서 건조",
    "machine_wash_30": "세탁기(30도)", "machine_wash_40": "세탁기(40도)",
    "machine_wash_delicate": "세탁기(약하게)", "machine_wash_normal": "세탁기(표준)",
    "machine_wash_permanent_press": "세탁기(구김 방지)", "natural_dry": "자연 건조",
    "no_dry": "건조 금지", "no_steam": "스팀 금지", "no_wet_clean": "웻클리닝 금지",
    "shade_dry": "그늘에 건조", "steam": "스팀 가능", "tumble_dry_low_delicate": "기계건조(저온, 약하게)",
    "tumble_dry_no_heat": "기계건조(열풍 금지)", "tumble_dry_normal": "기계건조(표준)",
    "wash_30_delicate": "물세탁(30도, 약하게)", "wash_30_very_delicate": "물세탁(30도, 매우 약하게)",
    "wash_40_delicate": "물세탁(40도, 약하게)", "wash_40_very_delicate": "물세탁(40도, 매우 약하게)",
    "wash_50": "물세탁(50도)", "wash_60": "물세탁(60도)", "wash_60_delicate": "물세탁(60도, 약하게)",
    "wash_60_very_delicate": "물세탁(60도, 매우 약하게)", "wash_70": "물세탁(70도)",
    "wash_95": "물세탁(95도)", "wash_95_delicate": "물세탁(95도, 약하게)",
    "wash_95_very_delicate": "물세탁(95도, 매우 약하게)", "wet_clean": "웻클리닝 가능",
    "wet_clean_delicate": "웻클리닝(약하게)", "wet_clean_very_delicate": "웻클리닝(매 약하게)",
    
    # [옷 종류 매핑]
    "Dress": "원피스", "Hoodie": "후드티", "Pants": "긴바지", "Shirt": "셔츠",
    "Short": "반바지", "Skirt": "치마", "Sweater": "스웨터", "T-shirt": "반팔티셔츠",
    "Outer": "아우터", "LongT-shirt": "긴팔티셔츠",

    # [얼룩 번역]
    "Stain": "얼룩",
    
    # [세탁기 모델 번역 추가]
    "Drum_washer": "드럼 세탁기",
    "Top_washer": "통돌이 세탁기",
    "Washing-machine": "세탁기(미분류)"
}

# 🌟 대분류 매핑 (긴바지 추가)
CATEGORY_MAP = {
    "반팔티셔츠": "상의", "긴팔티셔츠": "상의", "스웨터": "상의", 
    "셔츠": "상의", "후드티": "상의", "아우터": "상의",
    "바지": "하의", "청바지": "하의", "긴바지": "하의", "반바지": "하의", 
    "치마": "하의", "원피스": "하의"
}

# ---------------------------------------------------------
# 🌟 의류 색상 추출 함수 (초록색 포함) 🌟
# ---------------------------------------------------------
def get_clothing_color(cropped_img):
    if cropped_img is None or cropped_img.size == 0:
        return "알 수 없음"
    
    # 연산 속도 향상을 위해 100x100으로 이미지 리사이즈
    h, w = cropped_img.shape[:2]
    if h > 200 or w > 200:
        cropped_img = cv2.resize(cropped_img, (100, 100), interpolation=cv2.INTER_AREA)
        
    hsv = cv2.cvtColor(cropped_img, cv2.COLOR_BGR2HSV)
    
    # 초록색을 포함한 주요 색상들의 HSV 범위 정의
    COLOR_RANGES = {
        "흰색": (np.array([0, 0, 180]), np.array([180, 30, 255])),
        "검은색": (np.array([0, 0, 0]), np.array([180, 255, 50])),
        "회색": (np.array([0, 0, 50]), np.array([180, 30, 179])),
        "빨강": (np.array([0, 50, 50]), np.array([10, 255, 255])),
        "노랑": (np.array([11, 50, 50]), np.array([35, 255, 255])),
        "초록": (np.array([36, 50, 50]), np.array([85, 255, 255])),  
        "파랑": (np.array([86, 50, 50]), np.array([130, 255, 255])),
        "보라": (np.array([131, 50, 50]), np.array([170, 255, 255]))
    }
    
    color_counts = {}
    for color_name, (lower, upper) in COLOR_RANGES.items():
        mask = cv2.inRange(hsv, lower, upper)
        color_counts[color_name] = cv2.countNonZero(mask)
        
    max_color = max(color_counts, key=color_counts.get)
    
    if color_counts[max_color] < 20:
        return "알 수 없음"
        
    return max_color

# ---------------------------------------------------------
# 3. 통합 분석 API (의류 및 라벨용)
# ---------------------------------------------------------
@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    contents = await file.read()
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    initial_results = main_ai(img, conf=0.25)
    
    has_label = False
    has_clothes = False
    
    for r in initial_results:
        for box in r.boxes:
            class_name = main_ai.names[int(box.cls)]
            if class_name in LABEL_CLASSES:
                has_label = True
            elif class_name in CLOTHES_CLASSES:
                has_clothes = True

    detected_items = []
    
    if has_label:
        final_results = label_ai(img, conf=0.35, iou=0.6)
        scan_type = "LABEL_CARE"
        message = "세탁 기호가 인식되었습니다."
        target_model = label_ai
    elif has_clothes:
        final_results = clothes_ai(img, conf=0.6, iou=0.45)
        scan_type = "CLOSET_DIET"
        message = "의류가 인식되었습니다."
        target_model = clothes_ai
    else:
        return {"status": "fail", "scan_type": "UNKNOWN", "message": "다시 촬영해주세요.", "data": []}

    seen_names = set()
    for r in final_results:
        for box in r.boxes:
            raw_class_name = target_model.names[int(box.cls)]
            confidence = float(box.conf)
            korean_name = CLASS_MAP.get(raw_class_name, raw_class_name)
            
            if korean_name in seen_names:
                continue
            
            seen_names.add(korean_name)
            
            item_data = {
                "name": korean_name, 
                "confidence": round(confidence, 2),
                "box": box.xyxy[0].tolist(),
                "main_category": CATEGORY_MAP.get(korean_name, "미분류") # 🌟 대분류 필드 추가
            }
            
            if scan_type == "CLOSET_DIET":
                x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
                cropped_cloth = img[y1:y2, x1:x2]
                item_data["color"] = get_clothing_color(cropped_cloth)
                
            detected_items.append(item_data)
            
    return {
        "status": "success",
        "scan_type": scan_type,
        "message": message,
        "data": detected_items
    }

# ---------------------------------------------------------
# 4. 얼룩 스캔 전용 API
# ---------------------------------------------------------
@app.post("/analyze/stain")
async def analyze_stain(file: UploadFile = File(...)):
    contents = await file.read()
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    results = stain_ai(img, conf=0.25, iou=0.45) 
    
    detected_items = []
    
    for r in results:
        for box in r.boxes:
            raw_class_name = stain_ai.names[int(box.cls)]
            confidence = float(box.conf)
            korean_name = CLASS_MAP.get(raw_class_name, raw_class_name)
            
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            box_dict = {
                "x": int(x1),
                "y": int(y1),
                "w": int(x2 - x1),
                "h": int(y2 - y1) 
            }
            
            detected_items.append({
                "class": korean_name,            
                "confidence": round(confidence, 2),
                "box": box_dict                  
            })
            
    if len(detected_items) == 0:
        return {
            "status": "fail", 
            "scan_type": "STAIN_CARE", 
            "message": "얼룩을 발견하지 못했습니다. 다시 촬영해주세요.", 
            "results": [] 
        }
            
    return {
        "status": "success",
        "scan_type": "STAIN_CARE",
        "message": f"{len(detected_items)}개의 얼룩 위치를 발견했습니다. 어떤 얼룩인지 선택해주세요!",
        "results": detected_items 
    }

# ---------------------------------------------------------
# 5. 세탁기 스캔 전용 API
# ---------------------------------------------------------
@app.post("/analyze/washer")
async def analyze_washer(file: UploadFile = File(...)):
    contents = await file.read()
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    results = washer_ai(img, conf=0.5, iou=0.45) 
    
    detected_items = []
    seen_names = set()
    
    for r in results:
        for box in r.boxes:
            raw_class_name = washer_ai.names[int(box.cls)]
            confidence = float(box.conf)
            korean_name = CLASS_MAP.get(raw_class_name, raw_class_name)
            box_coords = box.xyxy[0].tolist()
            
            if korean_name in seen_names:
                continue
            
            seen_names.add(korean_name)
            detected_items.append({
                "name": korean_name, 
                "confidence": round(confidence, 2),
                "box": box_coords 
            })
            
    if len(detected_items) == 0:
        return {
            "status": "fail", 
            "scan_type": "WASHER_CARE", 
            "message": "세탁기를 발견하지 못했습니다. 화면에 잘 보이게 다시 촬영해주세요.", 
            "data": []
        }
            
    return {
        "status": "success",
        "scan_type": "WASHER_CARE",
        "message": "세탁기가 인식되었습니다. 정확한 코스 추천을 위해 브랜드를 선택해주세요!",
        "data": detected_items
    }