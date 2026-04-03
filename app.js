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

// 1. 서버 작동 확인용 메인 페이지
app.get('/', (req, res) => {
  res.send('<h1>Laundry Care 서버가 정상 작동 중입니다! 🚀</h1>');
});

// 2. [전체 조회] 모든 의류 데이터 가져오기 (관리자용/테스트용)
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

// 3. [개인별 조회] 특정 사용자의 의류 목록만 가져오기 (실제 앱용)
// 앱에서 /api/clothes/user_01 로 요청하면 user_01의 옷만 보여줍니다.
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

app.listen(port, () => {
  console.log(`서버가 http://localhost:${port} 에서 실행 중입니다.`);
});