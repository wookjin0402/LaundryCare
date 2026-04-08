from fastapi import FastAPI, UploadFile, File
from ultralytics import YOLO
import cv2
import numpy as np
import uvicorn

app = FastAPI()

# YOLOv8 모델 로드 (처음 실행 시 자동으로 다운로드됩니다)
model = YOLO("yolov8n.pt") 

@app.get("/")
def read_root():
    return {"message": "AI 분석 서버가 작동 중입니다! 🤖"}

@app.post("/analyze")
async def analyze_image(file: UploadFile = File(...)):
    # 1. 받은 파일 읽기
    contents = await file.read()
    nparr = np.frombuffer(contents, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    # 2. YOLO AI 분석 실행
    results = model(img)
    
    # 3. 결과 정리
    detections = []
    for r in results:
        for box in r.boxes:
            detections.append({
                "class": model.names[int(box.cls)],
                "confidence": round(float(box.conf), 2)
            })

    return {"results": detections}

if __name__ == "__main__":
    # 포트를 8000으로 설정 (Node.js는 3000번이므로 겹치지 않게!)
    uvicorn.run(app, host="0.0.0.0", port=8000)