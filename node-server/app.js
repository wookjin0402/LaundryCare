const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
// 메모리 저장소 설정 (사진을 컴퓨터 하드에 저장하지 않고, 
// 기억장치에만 잠깐 들고 있다가 바로 AI 서버로 던지기 위한 설정입니다)
const upload = multer({ storage: multer.memoryStorage() });
const express = require('express');
const admin = require('firebase-admin'); 
const serviceAccount = require('./serviceAccountKey.json'); 

const app = express();
const port = 3000;

// JSON 형식의 데이터를 주고받을 수 있게 설정
app.use(express.json());

// 파이어베이스 초기화
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// ==========================================
// 1. 서버 작동 확인용 메인 페이지
// ==========================================
app.get('/', (req, res) => {
  res.send('<h1>Laundry Care 서버가 정상 작동 중입니다! 🚀</h1>');
});

// ==========================================
// 2. [전체 조회] 모든 의류 데이터 가져오기 (관리자용/테스트용)
// ==========================================
app.get('/api/clothes', async (req, res) => {
  try {
    const snapshot = await db.collection('clothes').get();
    const list = [];
    snapshot.forEach(doc => {
      list.push({ id: doc.id, ...doc.data() });
    });
    res.status(200).json(list);
  } catch (error) {
    res.status(500).send("전체 데이터 로드 실패: " + error);
  }
});

// ==========================================
// 3. [개인별 조회] 특정 사용자의 의류 목록만 가져오기 (실제 앱용)
// ==========================================
app.get('/api/clothes/:uid', async (req, res) => {
  try {
    const uid = req.params.uid; // 주소창에 입력된 UID를 읽어옵니다.
    
    // DB의 'clothes' 상자에서 'ownerId' 필드가 일치하는 것만 필터링!
    const snapshot = await db.collection('clothes')
                           .where('ownerId', '==', uid)
                           .get();
    
    if (snapshot.empty) {
      return res.status(404).json({ message: "등록된 옷이 없어요!" });
    }

    const userClothes = [];
    snapshot.forEach(doc => {
      userClothes.push({ id: doc.id, ...doc.data() });
    });
    
    res.status(200).json(userClothes);
  } catch (error) {
    res.status(500).send("사용자 데이터 조회 실패: " + error);
  }
});

// ==========================================
// 4. [새 옷 등록] (POST)
// ==========================================
app.post('/api/clothes', async (req, res) => {
  try {
    // 1. 앱에서 보내준 핵심 정보들을 받습니다.
    const { uid, category, material, laundrySymbols, imageUrl, labelImageUrl } = req.body;

    // 2. 파이어베이스에 저장할 '새 옷 데이터' 묶음을 만듭니다. (초기값 자동 세팅)
    const newCloth = {
      ownerId: uid,
      category: category || "미분류",
      imageUrl: imageUrl || "",
      labelImageUrl: labelImageUrl || "",
      material: material || "알 수 없음",
      laundrySymbols: laundrySymbols || [],
      
      // --- 설계도에 맞춘 초기값 자동 부여 ---
      wearCount: 0,             // 처음 등록 시 착용 횟수는 0
      lifeScore: 100,           // 새 옷이니까 100점 시작
      status: "정상",           // 옷 상태 초기값
      lastWashedAt: null,       // 아직 한 번도 세탁 안 함
      createdAt: admin.firestore.FieldValue.serverTimestamp() // 현재 서버 시간
    };

    // 3. 'clothes' 컬렉션에 데이터 추가 (자동으로 랜덤 문서 ID 생성됨)
    const docRef = await db.collection('clothes').add(newCloth);
    
    // 4. 성공 응답 전송
    res.status(201).json({ 
      message: "옷이 성공적으로 등록되었습니다! 👕", 
      id: docRef.id // 파이어베이스가 새로 만들어준 문서 ID
    });

  } catch (error) {
    console.error("옷 등록 에러:", error);
    res.status(500).json({ error: "옷 데이터 저장에 실패했습니다." });
  }
});

// ==========================================
// 5. [AI 연동] 사진 찍어서 분석하고 DB에 바로 저장하기
// ==========================================
// upload.single('image'): 앱에서 'image'라는 이름으로 사진을 보낸다는 뜻입니다.
app.post('/api/clothes/analyze', upload.single('image'), async (req, res) => {
  try {
    const { uid, category, material } = req.body;
    const file = req.file; // 앱에서 보낸 사진 파일!

    if (!file) {
      return res.status(400).json({ error: "옷 사진이 없습니다!" });
    }

    // [1단계] 주방(AI 서버)으로 보낼 택배 상자 포장하기
    const formData = new FormData();
    formData.append('file', file.buffer, file.originalname);

    // [2단계] 오토바이(axios) 태워서 AI 서버(8000번)로 배달!
    console.log("🤖 AI 서버에 분석을 요청합니다...");
    const aiResponse = await axios.post('http://ai-server:8000/analyze', formData, {
      headers: { ...formData.getHeaders() }
    });

    // [3단계] AI 셰프가 찾은 세탁 기호 결과물 열어보기
    const aiResults = aiResponse.data.results; 
    // 예: [{"class": "wash_01", "confidence": 0.95}, ...]
    
    // 결과물에서 기호 이름(class)만 쏙쏙 뽑아서 배열로 만듭니다.
    const detectedSymbols = aiResults.map(item => item.class); 
    console.log("✅ AI 분석 완료! 찾은 기호들:", detectedSymbols);

    // [4단계] 파이어베이스 장부에 기록할 최종 데이터 만들기
    const newCloth = {
      ownerId: uid || "test_user",
      category: category || "미분류",
      material: material || "알 수 없음",
      laundrySymbols: detectedSymbols, // 💡 AI가 분석한 결과를 여기에 쏙!
      
      // 우리가 짰던 DB 설계도 초기값들
      wearCount: 0,
      lifeScore: 100,
      status: "정상",
      lastWashedAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    // [5단계] 파이어베이스 'clothes' 컬렉션에 저장
    const docRef = await db.collection('clothes').add(newCloth);

    // [6단계] 손님(모바일 앱)에게 최종 결과 알려주기
    res.status(201).json({
      message: "AI 분석 및 옷장 등록이 완료되었습니다! 🎉",
      id: docRef.id,
      ai_detected: detectedSymbols // 앱 화면에 띄워줄 수 있게 같이 보냄
    });

  } catch (error) {
    console.error("AI 연동 에러:", error);
    res.status(500).json({ error: "분석 및 저장 중 문제가 발생했습니다." });
  }
});

// ==========================================
// 서버 실행
// ==========================================
app.listen(port, () => {
  console.log(`Node.js 백엔드 서버가 http://localhost:${port} 에서 실행 중입니다. 🚀`);
});