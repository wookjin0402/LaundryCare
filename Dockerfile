# 1. Node.js 환경 가져오기 (주석 앞에 # 필수!)
FROM node:22

# 2. 서버 코드를 넣을 폴더 만들기
WORKDIR /app

# 3. 필요한 패키지 목록 복사 및 설치
COPY package*.json ./
RUN npm install

# 4. 나머지 코드 전부 복사
COPY . .

# 5. 서버 실행 (3000번 포트)
EXPOSE 3000
CMD ["node", "app.js"]