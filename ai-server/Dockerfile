# 1. ultralytics(YOLO) 공식 파이썬 이미지 사용 (필요한 리눅스 파일이 이미 100% 깔려있습니다!)
FROM ultralytics/ultralytics:latest-python

# 2. 작업할 폴더 설정
WORKDIR /app

# 3. 파이썬 라이브러리 설치 (apt-get 부분은 지워도 됩니다!)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 소스코드 전체 복사
COPY . .

# 5. 포트 열기
EXPOSE 8000

# 6. 서버 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]