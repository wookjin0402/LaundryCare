const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const express = require('express');
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');
const careDB = require('./care_guide'); 

const upload = multer({ storage: multer.memoryStorage() });
const app = express();
const port = 3000;

// OpenWeather API 설정
const WEATHER_API_KEY = '2ce6f426122bca89c97043cc1cf25b6a';
app.use(express.json());

// 파이어베이스 초기화
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});
const db = admin.firestore();

// 날씨 API 호출 함수
async function getWeatherData(lat, lon) {
    if (!lat || !lon) return null;
    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${WEATHER_API_KEY}&units=metric&lang=kr`;
    try {
        const response = await axios.get(url);
        return response.data;
    } catch (error) {
        console.error("날씨 API 호출 실패:", error.message);
        return null;
    }
}

// 스마트 가이드 생성 함수
function generateSmartGuide(detectedItems, weather) {
    let finalGuide = {
        title: "기본 세탁 가이드",
        summary: "옷의 상태와 라벨을 확인하여 안전하게 세탁하세요.",
        careSteps: [],
        warnings: [],
        weatherAdvice: null,
        raw_tags: []
    };

    let foundClothes = null;
    let foundLabels = [];

    detectedItems.forEach(item => {
        finalGuide.raw_tags.push(item.name);
        if (careDB.clothes[item.name]) foundClothes = item.name;
        else if (careDB.labels[item.name]) foundLabels.push(item.name);
    });

    if (foundClothes) {
        const clothesInfo = careDB.clothes[foundClothes];
        finalGuide.title = `✨ ${foundClothes} 맞춤 케어 가이드`;
        finalGuide.summary = clothesInfo.material_tip;
        finalGuide.careSteps.push({ step: "세탁 방법", desc: clothesInfo.wash_rule });
        finalGuide.careSteps.push({ step: "건조 및 보관", desc: clothesInfo.dry_rule });
    }

    if (foundLabels.length > 0) {
        foundLabels.forEach(label => {
            let labelDesc = careDB.labels[label];
            let isWarning = label.includes("금지") || label.includes("주의") || label.includes("필요");
            finalGuide.warnings.push({
                icon_name: label,
                desc: labelDesc,
                is_critical: isWarning
            });
        });
    }

    if (weather) {
        const temp = weather.main.temp;
        const humidity = weather.main.humidity;
        const condition = weather.weather[0].main;

        let advice = {
            recommendation: "기본 세탁 가능",
            dryMethod: "실내 건조",
            savingTip: ""
        };

        if (condition === "Rain" || condition === "Snow") {
            advice.recommendation = "세탁 보류 권장";
            advice.dryMethod = "제습기 또는 건조기 필수";
            advice.savingTip = "비나 눈이 오는 날은 세탁물이 잘 마르지 않고 냄새가 날 수 있습니다.";
        } else if (humidity < 50 && condition === "Clear") {
            advice.recommendation = "세탁하기 딱 좋은 날씨";
            advice.dryMethod = "자연 건조(햇빛) 추천";
            advice.savingTip = "전기료를 아끼기 위해 건조기 대신 자연 건조를 해보세요!";
        }

        if (temp > 22 && (foundClothes === "스웨터" || foundClothes === "패딩")) {
            finalGuide.warnings.push({
                icon_name: "계절 보관 알림",
                desc: "날씨가 더워지고 있습니다. 두꺼운 옷은 세탁 후 제습제와 함께 보관하세요.",
                is_critical: false
            });
        }
        if (temp < 5 && foundClothes === "스웨터") {
             finalGuide.warnings.push({
                icon_name: "정전기 주의",
                desc: "건조한 겨울 날씨에는 니트 보풀과 정전기가 심해질 수 있으니 린스를 활용해 헹궈주세요.",
                is_critical: false
            });
        }
        finalGuide.weatherAdvice = advice;
    }

    if (foundClothes === "스웨터" && !foundLabels.includes("기계건조 금지")) {
        finalGuide.warnings.push({
            icon_name: "기계건조 주의",
            desc: "라벨에 표기가 없더라도 스웨터류는 건조기 사용을 피하는 것이 좋습니다.",
            is_critical: true
        });
    }

    return finalGuide;
}

app.get('/', (req, res) => {
    res.send('<h1>Laundry Care 서버가 정상 작동 중입니다! 🚀 (단일/다중 의류 대조 테스트 버전)</h1>');
});

// [API] 옷/라벨 스캔
app.post('/api/clothes/analyze', upload.fields([
    { name: 'clothImage', maxCount: 1 },
    { name: 'labelImage', maxCount: 1 }
]), async (req, res) => {
    try {
        const { uid, category, material, lat, lon } = req.body;
        const files = req.files || {};
        const clothFile = files['clothImage'] ? files['clothImage'][0] : null;
        const labelFile = files['labelImage'] ? files['labelImage'][0] : null;

        if (!clothFile && !labelFile) return res.status(400).json({ error: "사진이 필요합니다!" });

        const weatherData = await getWeatherData(lat, lon);

        async function sendToAiServer(file) {
            if (!file) return [];
            const formData = new FormData();
            formData.append('file', file.buffer, { filename: file.originalname, contentType: file.mimetype });
            const response = await axios.post('http://ai-server:8000/analyze', formData, { headers: { ...formData.getHeaders() } });
            return response.data.data || [];
        }

        const [clothAiResults, labelAiResults] = await Promise.all([ sendToAiServer(clothFile), sendToAiServer(labelFile) ]);
        const combinedAiData = [...clothAiResults, ...labelAiResults];
        const detectedSymbols = combinedAiData.map(item => item.name);
        const smartGuide = generateSmartGuide(combinedAiData, weatherData);

        let currentScanType = (clothFile && labelFile) ? "COMPREHENSIVE_CARE" : (clothFile ? "CLOSET_DIET" : "LABEL_CARE");

        // 🌟 DB 저장 시 lastWashedAt과 lastWornAt 필드 초기화 추가 🌟
        const docRef = await db.collection('clothes').add({
            ownerId: uid || "test_user",
            category: category || (clothFile && detectedSymbols.length > 0 ? detectedSymbols[0] : "미분류"),
            material: material || "알 수 없음",
            laundrySymbols: detectedSymbols,
            scanType: currentScanType,
            weatherInfo: weatherData ? { temp: weatherData.main.temp, condition: weatherData.weather[0].main } : null,
            lastWashedAt: null,  // 세탁 이력 초기화
            lastWornAt: null,    // 착용 이력 초기화
            wearCount: 0, 
            lifeScore: 100, 
            status: "정상", 
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        res.status(201).json({
            status: "success", scan_type: currentScanType, id: docRef.id,
            weather: weatherData ? { city: weatherData.name, temp: weatherData.main.temp, desc: weatherData.weather[0].description } : null,
            ai_detected: detectedSymbols, guide: smartGuide
        });
    } catch (error) {
        res.status(500).json({ error: "분석 및 저장 중 문제가 발생했습니다." });
    }
});

// [API] 얼룩 스캔
app.post('/api/stains/analyze', upload.single('stainImage'), async (req, res) => {
    try {
        const file = req.file;
        if (!file) return res.status(400).json({ error: "얼룩 사진이 필요합니다!" });
        const formData = new FormData();
        formData.append('file', file.buffer, { filename: file.originalname, contentType: file.mimetype });
        const response = await axios.post('http://ai-server:8000/analyze/stain', formData, { headers: { ...formData.getHeaders() } });
        res.status(200).json(response.data);
    } catch (error) {
        res.status(500).json({ error: "얼룩 스캔 중 문제가 발생했습니다." });
    }
});

// [API] 얼룩 가이드
app.post('/api/stains/guide', async (req, res) => {
    try {
        const { uid, clothId, stainType } = req.body;
        if (!stainType || !careDB.stains[stainType]) return res.status(400).json({ error: "유효하지 않은 얼룩 종류입니다." });
        const guide = careDB.stains[stainType];
        res.status(200).json({ status: "success", stain_type: stainType, cause: guide.cause, care_tip: guide.care_tip });
    } catch (error) {
        res.status(500).json({ error: "가이드 생성 중 문제가 발생했습니다." });
    }
});

// [API] 세탁기 스캔
app.post('/api/washers/analyze', upload.single('washerImage'), async (req, res) => {
    try {
        const file = req.file;
        if (!file) return res.status(400).json({ error: "세탁기 사진이 필요합니다!" });
        const formData = new FormData();
        formData.append('file', file.buffer, { filename: file.originalname, contentType: file.mimetype });
        const response = await axios.post('http://ai-server:8000/analyze/washer', formData, { headers: { ...formData.getHeaders() } });
        res.status(200).json(response.data);
    } catch (error) {
        res.status(500).json({ error: "세탁기 스캔 중 문제가 발생했습니다." });
    }
});

// ---------------------------------------------------------
// 🌟 [핵심] 옷장 의류(반팔티셔츠 등) ↔ 세탁기 스펙(F21VDW) 정밀 대조 API 🌟
// ---------------------------------------------------------
app.post('/api/washers/guide', async (req, res) => {
    try {
        // 프론트엔드에서 세탁기 정보와 '옷장에 등록된 옷(배열)'을 보냅니다.
        const { washerType, brand, modelName, clothesList } = req.body;

        if (!washerType || !careDB.washers[washerType]) {
            return res.status(400).json({ error: "유효하지 않은 세탁기 종류입니다." });
        }

        // 세탁기 스펙 매핑 (입력된 모델명이 없으면 default 사용)
        const brandInfo = careDB.washers[washerType].brands[brand] || careDB.washers[washerType].brands["기타"];
        let deviceSpec = brandInfo[modelName] || brandInfo["default"];
        
        let finalCourse = "표준세탁 코스"; 
        let finalTemp = "40도";
        let specialNotes = [];
        let systemWarnings = [];

        // 옷 데이터가 없을 경우 방어
        if (!clothesList || clothesList.length === 0) {
            return res.status(400).json({ error: "옷장에서 선택된 세탁할 의류 정보가 없습니다." });
        }

        let requiresDelicate = false;
        let requiresCold = false;
        let requiresNoTumbleDry = false;
        let requiresNoSteam = false;

        // 1. 옷장 데이터 순회 분석
        clothesList.forEach(cloth => {
            const type = cloth.type;
            const labels = cloth.labels || [];

            // 옷 종류 분석 (반팔티셔츠는 일반적으로 delicate 대상이 아님)
            if (type === "스웨터" || type === "니트" || type === "원피스") requiresDelicate = true;

            // 라벨 분석
            if (labels.includes("기계건조 금지")) requiresNoTumbleDry = true;
            if (labels.includes("스팀 금지") || type === "스웨터") requiresNoSteam = true;
            if (labels.includes("세탁기(찬물)") || labels.includes("물세탁(30도)") || labels.includes("물세탁(30도, 약하게)")) requiresCold = true;
        });

        // 2. 코스 및 온도 결정
        if (requiresDelicate) {
            finalCourse = deviceSpec.courses.includes("울/섬세") ? "울/섬세 코스" : "섬세 코스";
            specialNotes.push("옷감 보호를 위해 '울/섬세 코스'로 자동 설정됩니다.");
        } else if (deviceSpec.courses.includes("인공지능세탁")) {
            // 반팔티셔츠 등 일반 의류이고, F21VDW처럼 AI 기능이 있다면 AI 세탁 추천
            finalCourse = "인공지능세탁 코스";
            specialNotes.push("옷감의 재질과 무게를 감지하는 인공지능 세탁을 추천합니다.");
        }

        if (requiresCold) {
            finalTemp = "냉수 (30도 이하 고정)";
            specialNotes.push("라벨 조건에 맞추어 세탁기 온도를 냉수로 고정합니다.");
        }

        // 3. 기기 스펙(F21VDW)과 의류 라벨 충돌 경고
        if (requiresNoTumbleDry && deviceSpec.specs.includes("건조 기능 내장")) {
            systemWarnings.push({
                issue: "건조 모드 사용 주의",
                reason: "'기계건조 금지' 라벨이 감지되었습니다. 세탁 완료 후 세탁기의 건조 기능을 사용하지 마세요."
            });
        }

        if (requiresNoSteam && deviceSpec.specs.includes("트루스팀(TrueSteam)")) {
            systemWarnings.push({
                issue: "트루스팀(TrueSteam) 해제",
                reason: "열에 약한 의류가 포함되어 있습니다. 스팀 옵션을 끄고 세탁해 주세요."
            });
        }

        // 4. 최종 결과 반환
        res.status(200).json({
            status: "success",
            total_items: clothesList.length,
            matched_device: {
                name: deviceSpec.model_title,
                model_code: modelName || "미입력",
                detected_specs: deviceSpec.specs
            },
            final_recommendation: {
                confirmed_course: finalCourse,
                target_temperature: finalTemp,
                hardware_adjustments: specialNotes
            },
            critical_conflicts: systemWarnings,
            message: "옷장 의류 데이터와 기기 스펙 대조가 완료되었습니다."
        });

    } catch (error) {
        console.error("옷장 대조 에러:", error.message);
        res.status(500).json({ error: "대조 처리 중 문제가 발생했습니다." });
    }
});

// ---------------------------------------------------------
// 🌟 [신규 API] 세탁 완료 처리 및 이력(DB) 업데이트 🌟
// ---------------------------------------------------------
app.post('/api/clothes/wash-complete', async (req, res) => {
    try {
        // 프론트엔드에서 방금 세탁기에 넣은 옷들의 ID 배열을 보냅니다.
        // 예: { "clothIds": ["docId_1", "docId_2"] }
        const { clothIds } = req.body;

        if (!clothIds || !Array.isArray(clothIds) || clothIds.length === 0) {
            return res.status(400).json({ error: "세탁 완료 처리할 옷의 정보가 없습니다." });
        }

        // 파이어베이스 일괄 처리(Batch) 준비
        const batch = db.batch();

        clothIds.forEach(id => {
            const clothRef = db.collection('clothes').doc(id);
            batch.update(clothRef, {
                lastWashedAt: admin.firestore.FieldValue.serverTimestamp(), // 오늘 날짜로 세탁일 갱신
                wearCount: admin.firestore.FieldValue.increment(1),         // 착용(세탁) 횟수 1 증가
                status: "정상"                                              // 얼룩 등이 있었다면 깨끗해짐으로 상태 변경
            });
        });

        // DB에 일괄 저장 실행
        await batch.commit();

        res.status(200).json({
            status: "success",
            message: `${clothIds.length}벌의 옷에 대한 세탁 이력이 성공적으로 저장되었습니다.`
        });

    } catch (error) {
        console.error("세탁 이력 업데이트 에러:", error.message);
        res.status(500).json({ error: "세탁 이력 저장 중 문제가 발생했습니다." });
    }
});

app.listen(port, () => {
    console.log(`Node.js 백엔드 서버 실행 중: port ${port}`);
});