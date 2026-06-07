const multer = require('multer');
const axios = require('axios');
const FormData = require('form-data');
const express = require('express');
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');
const path = require('path');
const careDB = require(path.join(__dirname, 'care_guide.js'));

const vision = require('@google-cloud/vision');
const visionClient = new vision.ImageAnnotatorClient({ keyFilename: './serviceAccountKey.json' });
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

// 1. 날씨 API 호출 함수
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

// 2. 미세먼지 API 호출 함수
async function getAirQualityData(lat, lon) {
    if (!lat || !lon) return null;
    const url = `http://api.openweathermap.org/data/2.5/air_pollution?lat=${lat}&lon=${lon}&appid=${WEATHER_API_KEY}`;
    try {
        const response = await axios.get(url);
        return response.data;
    } catch (error) {
        console.error("미세먼지 API 호출 실패:", error.message);
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
    res.send('<h1>Laundry Care 서버가 정상 작동 중입니다! 🚀 (혼용률/사이즈 추출 최적화 버전)</h1>');
});

// ---------------------------------------------------------
// 🌟 [API] 옷/라벨 스캔 (순차 처리 + 긴바지 기반 자동 분류 + 혼용률 업그레이드) 🌟
// ---------------------------------------------------------
app.post('/api/clothes/analyze', upload.fields([
    { name: 'clothImage', maxCount: 1 },
    { name: 'labelImages', maxCount: 5 } 
]), async (req, res) => {
    try {
        const { uid, category, material, lat, lon, season, mainCategory, subCategory, color } = req.body;
        const files = req.files || {};
        const clothFile = files['clothImage'] ? files['clothImage'][0] : null;
        const labelFiles = files['labelImages'] || []; 

        if (!clothFile && labelFiles.length === 0) return res.status(400).json({ error: "사진이 필요합니다!" });

        const weatherData = await getWeatherData(lat, lon);

        async function sendToAiServer(file) {
            if (!file) return [];
            const formData = new FormData();
            formData.append('file', file.buffer, { filename: file.originalname, contentType: file.mimetype });
            const response = await axios.post('http://ai-server:8000/analyze', formData, { headers: { ...formData.getHeaders() } });
            return response.data.data || [];
        }

        let combinedAiData = [];

        if (clothFile) {
            console.log("👕 옷 사진 AI 분석 시작...");
            const clothResult = await sendToAiServer(clothFile);
            combinedAiData.push(...clothResult);
        }

        if (labelFiles.length > 0) {
            console.log(`🏷️ 라벨 사진 ${labelFiles.length}장 순차 분석 시작...`);
            for (const file of labelFiles) {
                const labelResult = await sendToAiServer(file);
                combinedAiData.push(...labelResult);
            }
        }
        
        const detectedSymbols = [...new Set(combinedAiData.map(item => item.name))];
        let currentScanType = (clothFile && labelFiles.length > 0) ? "COMPREHENSIVE_CARE" : (clothFile ? "CLOSET_DIET" : "LABEL_CARE");

        const subCategoryMap = {
            "반팔티셔츠": "상의", "긴팔티셔츠": "상의", "스웨터": "상의", "셔츠": "상의", "후드티": "상의", "아우터": "상의",
            "바지": "하의", "청바지": "하의", "긴바지": "하의", "반바지": "하의", "치마": "하의", "원피스": "하의"
        };

        const detectedCloth = combinedAiData.find(item => subCategoryMap[item.name]);
        let finalSubCategory = subCategory || (detectedCloth ? detectedCloth.name : "미분류");

        let predictedColor = "알 수 없음";
        const clothItemWithColor = combinedAiData.find(item => item.color);
        if (clothItemWithColor) {
            predictedColor = clothItemWithColor.color;
        }
        const finalColor = color || predictedColor;

        if (finalSubCategory === "긴바지") {
            if (finalColor === "청색" || finalColor === "파랑" || finalColor === "회색") {
                finalSubCategory = "청바지";
            }
        }

        const determinedMainCategory = subCategoryMap[finalSubCategory] || mainCategory || "미분류";

        const guideAiData = combinedAiData.map(item => {
            if (item.name === "긴바지") {
                return { ...item, name: finalSubCategory === "청바지" ? "청바지" : "바지" };
            }
            return item;
        });
        const smartGuide = generateSmartGuide(guideAiData, weatherData);

        // ------------------------------------------------------------------
        // 🌟 구글 비전 OCR 처리 (소재 혼용률 및 사이즈 추출 업그레이드) 🌟
        // ------------------------------------------------------------------
        let extractedMaterials = [];
        let extractedSize = "알 수 없음";
        let allLabelsText = ""; 

        if (labelFiles.length > 0) {
            try {
                for (const file of labelFiles) {
                    const [result] = await visionClient.textDetection(file.buffer);
                    if (result.fullTextAnnotation) {
                        allLabelsText += result.fullTextAnnotation.text + " ";
                    }
                }
                
                const textUpper = allLabelsText.toUpperCase();

                // 1. 👕 [사이즈 추출 개선] '사이즈100' 처럼 띄어쓰기 없이 붙어있어도 숫자/영문자만 깔끔하게 추출
                const sizeRegex = /(?:사이즈|호칭|SIZE)?\s*[:>-]?\s*(XS|S|M|L|XL|XXL|FREE|80|85|90|95|100|105|110|115|120)(?!\d)/gi;
                const sizeMatch = textUpper.match(sizeRegex);
                if (sizeMatch) {
                    // 불필요한 한글이나 특수기호를 날리고 핵심 사이즈 텍스트만 남김
                    extractedSize = sizeMatch[0].replace(/[^A-Z0-9]/g, '');
                }

                // 2. 🧵 [소재 혼용률 추출 개선] 단어 뒤에 붙은 숫자와 %까지 함께 스캔
                const materialKeywords = {
                    '면': ['면', 'COTTON'],
                    '폴리에스터': ['폴리에스터', '폴리에스텔', 'POLYESTER'],
                    '울': ['울', '모', 'WOOL'],
                    '나일론': ['나일론', 'NYLON'],
                    '아크릴': ['아크릴', 'ACRYLIC'],
                    '레이온': ['레이온', 'RAYON', 'VISCOSE'],
                    '린넨': ['린넨', '마', 'LINEN']
                };

                for (const [matName, keywords] of Object.entries(materialKeywords)) {
                    for (const keyword of keywords) {
                        // 예: "면 80%", "POLYESTER: 100%" 패턴을 잡는 정규식
                        const mixRateRegex = new RegExp(`${keyword}\\s*[:>-]?\\s*(\\d{1,3})\\s*%`, 'i');
                        const match = textUpper.match(mixRateRegex);
                        
                        if (match) {
                            // "면 80%" 형태로 배열에 저장
                            extractedMaterials.push(`${matName} ${match[1]}%`);
                            break; // 퍼센트를 찾았으면 해당 소재 검색 종료
                        } else if (textUpper.includes(keyword)) {
                            // 퍼센트는 안 적혀있지만 소재 이름은 발견된 경우 (예: 그냥 '면'만 적힌 경우)
                            if (!extractedMaterials.some(m => m.includes(matName))) {
                                extractedMaterials.push(matName);
                            }
                        }
                    }
                }

            } catch (ocrError) {
                console.error("OCR 분석 중 에러:", ocrError.message);
            }
        }
        // ------------------------------------------------------------------

        const docRef = await db.collection('clothes').add({
            ownerId: uid || "test_user",
            category: category || (clothFile && detectedSymbols.length > 0 ? detectedSymbols[0] : "미분류"),
            material: material || (extractedMaterials.length > 0 ? extractedMaterials.join(", ") : "알 수 없음"),
            season: season || "미분류",
            mainCategory: determinedMainCategory, 
            subCategory: finalSubCategory, 
            color: finalColor,
            size: extractedSize,
            laundrySymbols: detectedSymbols,
            scanType: currentScanType,
            weatherInfo: weatherData ? { temp: weatherData.main.temp, condition: weatherData.weather[0].main } : null,
            lastWashedAt: null,
            lastWornAt: null,
            wearCount: 0, 
            lifeScore: 100, 
            status: "정상", 
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });

        res.status(201).json({
            status: "success", 
            scan_type: currentScanType, 
            id: docRef.id,
            season: season || "미분류",
            mainCategory: determinedMainCategory, 
            subCategory: finalSubCategory,
            color: finalColor,
            weather: weatherData ? { city: weatherData.name, temp: weatherData.main.temp, desc: weatherData.weather[0].description } : null,
            ai_detected: detectedSymbols, 
            extracted_materials: extractedMaterials, 
            extracted_size: extractedSize,           
            guide: smartGuide
        });
    } catch (error) {
        console.error("분석 및 저장 중 에러 발생:", error.message);
        res.status(500).json({ error: "분석 및 저장 중 문제가 발생했습니다." });
    }
});

// ---------------------------------------------------------
// 🌟 [API] 얼룩 스캔 
// ---------------------------------------------------------
app.post('/api/stains/analyze', upload.single('stainImage'), async (req, res) => {
    try {
        const file = req.file;
        if (!file) return res.status(400).json({ error: "얼룩 사진이 필요합니다!" });
        
        const formData = new FormData();
        formData.append('file', file.buffer, { filename: file.originalname, contentType: file.mimetype });
        
        const response = await axios.post('http://ai-server:8000/analyze/stain', formData, { headers: { ...formData.getHeaders() } });
        
        const aiData = response.data;
        const rawResults = aiData.data || aiData.results || [];

        const formattedResults = rawResults.map(item => {
            let boxObj = { x: 0, y: 0, w: 0, h: 0 };

            if (Array.isArray(item.box) && item.box.length === 4) {
                boxObj = {
                    x: Math.round(item.box[0]),
                    y: Math.round(item.box[1]),
                    w: Math.round(item.box[2] - item.box[0]),
                    h: Math.round(item.box[3] - item.box[1])
                };
            } else if (item.box && item.box.x !== undefined) {
                boxObj = item.box;
            }

            return {
                box: boxObj,
                stainType: item.name || item.class || "얼룩", 
                confidence: item.confidence || 0.0
            };
        });

        res.status(200).json({
            status: "success",
            message: "얼룩 분석이 완료되었습니다!",
            results: formattedResults
        });

    } catch (error) {
        console.error("얼룩 스캔 중 문제가 발생했습니다:", error.message);
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

// ---------------------------------------------------------
// 🌟 [API] 세탁기 스캔 (OCR 에러 방어 로직 적용) 🌟
// ---------------------------------------------------------
app.post('/api/washers/analyze', upload.single('washerImage'), async (req, res) => {
    try {
        const file = req.file;
        if (!file) return res.status(400).json({ error: "세탁기 사진이 필요합니다!" });

        const formData = new FormData();
        formData.append('file', file.buffer, { filename: file.originalname, contentType: file.mimetype });
        
        // 🌟 1. AI 서버 통신 (안전하게 분리)
        let aiData = null;
        try {
            const aiResponse = await axios.post('http://ai-server:8000/analyze/washer', formData, { headers: { ...formData.getHeaders() } });
            aiData = aiResponse.data;
        } catch (aiError) {
            console.error("세탁기 AI 분석 서버 에러:", aiError.message);
            return res.status(500).json({ error: "AI 서버에서 세탁기를 분석하지 못했습니다." });
        }

        // 🌟 2. OCR 통신 (에러가 나도 서버가 터지지 않게 무시하고 진행)
        let labelsText = '';
        try {
            const [ocrResult] = await visionClient.textDetection(file.buffer);
            if (ocrResult && ocrResult.fullTextAnnotation) {
                labelsText = ocrResult.fullTextAnnotation.text;
            }
        } catch (ocrError) {
            console.error("세탁기 OCR 분석 에러 (무시하고 계속 진행):", ocrError.message);
            // OCR이 실패해도 AI가 찾은 세탁기 결과는 반환하기 위해 계속 진행합니다.
        }
        
        // 🌟 3. AI가 찾아낸 세탁기 종류 추출
        let extractedWasherType = "알 수 없음";
        if (aiData && aiData.data && aiData.data.length > 0) {
            const bestMatch = aiData.data.sort((a, b) => b.confidence - a.confidence)[0];
            extractedWasherType = bestMatch.name; // "드럼 세탁기" 또는 "통돌이 세탁기"
        }

        // 🌟 4. OCR 기반 브랜드 추출
        let extractedBrand = "기타";
        const textUpper = labelsText.toUpperCase();
        
        if (textUpper.includes('LG') || textUpper.includes('엘지') || textUpper.includes('TROMM') || textUpper.includes('트롬')) {
            extractedBrand = "LG";
        } else if (textUpper.includes('SAMSUNG') || textUpper.includes('삼성') || textUpper.includes('BESPOKE') || textUpper.includes('GRANDE') || textUpper.includes('그랑데')) {
            extractedBrand = "삼성";
        } else if (textUpper.includes('WINIA') || textUpper.includes('위니아') || textUpper.includes('클라쎄')) {
            extractedBrand = "위니아";
        }

        let extractedModelCandidates = [];
        const modelRegex = /[A-Z0-9]{4,}/g; 
        const modelMatch = textUpper.match(modelRegex);
        
        if (modelMatch) {
            extractedModelCandidates = [...new Set(modelMatch)].filter(word => 
                !['SAMSUNG', 'TROMM', 'WASH', 'SPIN', 'RINSE', 'TEMP'].includes(word)
            );
        }

        res.status(200).json({
            ...aiData,
            extracted_washer_type: extractedWasherType, 
            extracted_brand: extractedBrand,            
            extracted_models: extractedModelCandidates, 
            raw_text: textUpper                         
        });

    } catch (error) {
        console.error("세탁기 스캔 알 수 없는 에러:", error.message);
        res.status(500).json({ error: "세탁기 스캔 중 문제가 발생했습니다." });
    }
});

// 옷장 의류(5종) ↔ 세탁기 스펙 대조 API
app.post('/api/washers/guide', async (req, res) => {
    try {
        const { washerType, brand, modelName, clothesList } = req.body;

        if (!washerType || !careDB.washers[washerType]) {
            return res.status(400).json({ error: "유효하지 않은 세탁기 종류입니다." });
        }

        const brandInfo = careDB.washers[washerType].brands[brand] || careDB.washers[washerType].brands["기타"];
        let deviceSpec = brandInfo[modelName] || brandInfo["default"];
        
        let finalCourse = "표준세탁 코스"; 
        let finalTemp = "40도";
        let specialNotes = [];
        let systemWarnings = [];

        if (!clothesList || clothesList.length === 0) {
            return res.status(400).json({ error: "옷장에서 선택된 세탁할 의류 정보가 없습니다." });
        }

        let requiresDelicate = false;
        let requiresCold = false;
        let requiresNoTumbleDry = false;
        let requiresNoSteam = false;

        let containsOuter = false;
        let containsLongPants = false;
        let containsShortTshirt = false;

        clothesList.forEach(cloth => {
            const subType = cloth.subCategory || cloth.category || cloth.type || ""; 
            const mainType = cloth.mainCategory || "";
            const labels = cloth.labels || cloth.laundrySymbols || [];

            if (subType.includes("아우터")) {
                requiresDelicate = true;
                requiresNoSteam = true; 
                containsOuter = true;
            } else if (mainType.includes("고급")) {
                requiresDelicate = true; 
            } else if (subType.includes("바지") || mainType.includes("하의") || subType.includes("긴바지")) {
                containsLongPants = true;
            } else if (subType.includes("반팔")) {
                containsShortTshirt = true;
            }
            
            if (labels.includes("기계건조 금지")) requiresNoTumbleDry = true;
            if (labels.includes("스팀 금지")) requiresNoSteam = true;
            if (labels.includes("세탁기(찬물)") || labels.includes("물세탁(30도)") || labels.includes("물세탁(30도, 약하게)")) requiresCold = true;
        });

        if (requiresDelicate) {
            finalCourse = deviceSpec.courses.includes("울/섬세") ? "울/섬세 코스" : "섬세 코스";
            if (containsOuter) {
                specialNotes.push("🧥 [아우터 포함] 옷감 보호와 형태 유지를 위해 마찰이 적은 '울/섬세 코스'로 자동 설정됩니다.");
            } else {
                specialNotes.push("✨ [고급 의류 포함] 세심한 관리가 필요한 고급 의류가 포함되어 마찰이 적은 '울/섬세 코스'로 자동 설정됩니다.");
            }
        } else if (deviceSpec.courses.includes("인공지능세탁")) {
            finalCourse = "인공지능세탁 코스";
            specialNotes.push("🤖 [일반 의류] 최적의 세탁을 위해 인공지능 세탁 코스를 추천합니다.");
        }

        if (containsLongPants) {
            specialNotes.push("👖 [하의류 케어] 세탁 전 지퍼와 단추를 모두 잠그고 뒤집어서 세탁하면 이염과 핏 변형을 막을 수 있습니다.");
        }
        if (containsShortTshirt) {
            specialNotes.push("👕 [반팔/상의 케어] 프린팅 손상과 목 늘어남을 방지하기 위해 옷을 뒤집어서 세탁망에 넣는 것을 권장합니다.");
        }

        if (requiresCold) {
            finalTemp = "냉수 (30도 이하 고정)";
            specialNotes.push("❄️ 옷감 수축 또는 라벨 조건에 맞추어 세탁기 온도를 냉수로 고정합니다.");
        }
        if (requiresNoTumbleDry && deviceSpec.specs.includes("건조 기능 내장")) {
            systemWarnings.push({ issue: "건조 모드 사용 주의", reason: "기계건조 금지 라벨이 감지되었습니다. 세탁 후 기기의 건조 기능을 쓰지 마세요." });
        }
        if (requiresNoSteam && deviceSpec.specs.includes("트루스팀(TrueSteam)")) {
            systemWarnings.push({ issue: "트루스팀(TrueSteam) 해제", reason: "아우터 등 열에 약한 의류가 포함되어 있습니다. 스팀 옵션을 끄고 세탁해 주세요." });
        }

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

// 세탁 추천 및 이염/소재 알고리즘 연동
app.post('/api/laundry/recommend', async (req, res) => {
    try {
        const { washer_type, brand, model, cloth_ids } = req.body;

        if (!cloth_ids || !Array.isArray(cloth_ids) || cloth_ids.length === 0) {
            return res.status(400).json({ error: "선택된 옷이 없습니다." });
        }

        const clothesData = [];
        const snapshot = await db.collection('clothes').where(admin.firestore.FieldPath.documentId(), 'in', cloth_ids).get();
        snapshot.forEach(doc => clothesData.push({ id: doc.id, ...doc.data() }));

        let has_critical_warning = false;
        let warning_message = "";
        let recommended_course = "표준 코스";

        let colors = new Set();
        let hasOuterOrDelicate = false;

        clothesData.forEach(cloth => {
            if (cloth.color) colors.add(cloth.color);
            const subCategory = cloth.subCategory || cloth.category || cloth.type || "";
            const mainCategory = cloth.mainCategory || "";
            
            if (subCategory.includes("아우터") || mainCategory.includes("고급")) {
                hasOuterOrDelicate = true;
            }
        });

        if (colors.has('빨강') && (colors.has('흰색') || colors.has('흰색 계열'))) {
            has_critical_warning = true;
            warning_message = "경고: 빨간색 옷과 흰색 옷이 섞여 있어 이염될 위험이 있습니다. 분리 세탁을 강력히 권장합니다.";
        }

        if (hasOuterOrDelicate) {
            recommended_course = "울/섬세 코스";
            if (!has_critical_warning) {
                warning_message = "외투(아우터)나 고급 의류가 포함되어 있습니다. 옷감 보호를 위해 울/섬세 코스로 설정합니다.";
            }
        }

        return res.status(200).json({
            status: "success",
            washer_type: washer_type,
            recommended_course: recommended_course,
            has_critical_warning: has_critical_warning,
            warning_message: warning_message
        });

    } catch (error) {
        console.error("세탁 추천 API 에러:", error);
        return res.status(500).json({ error: "서버 내부 오류가 발생했습니다." });
    }
});

// 세탁 완료 처리 및 이력(DB) 업데이트
app.post('/api/clothes/wash-complete', async (req, res) => {
    try {
        const { clothIds } = req.body;

        if (!clothIds || !Array.isArray(clothIds) || clothIds.length === 0) {
            return res.status(400).json({ error: "세탁 완료 처리할 옷의 정보가 없습니다." });
        }

        const batch = db.batch();

        clothIds.forEach(id => {
            const clothRef = db.collection('clothes').doc(id);
            batch.update(clothRef, {
                lastWashedAt: admin.firestore.FieldValue.serverTimestamp(),
                wearCount: admin.firestore.FieldValue.increment(1), 
                status: "정상" 
            });
        });

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

// 날씨 및 미세먼지 기반 단독 세탁 추천
app.post('/api/weather/recommend', async (req, res) => {
    try {
        const { lat, lon } = req.body;

        if (!lat || !lon) {
            return res.status(400).json({ error: "위도(lat)와 경도(lon) 정보가 필요합니다." });
        }

        const [weatherData, airData] = await Promise.all([
            getWeatherData(lat, lon),
            getAirQualityData(lat, lon)
        ]);

        if (!weatherData) {
            return res.status(500).json({ error: "날씨 정보를 가져오는 데 실패했습니다." });
        }

        const temp = weatherData.main.temp;
        const humidity = weatherData.main.humidity;
        const condition = weatherData.weather[0].main;

        let aqi = 1;
        let dustStatus = "좋음";
        if (airData && airData.list && airData.list.length > 0) {
            aqi = airData.list[0].main.aqi;
            if (aqi === 1) dustStatus = "좋음";
            else if (aqi === 2 || aqi === 3) dustStatus = "보통";
            else if (aqi === 4) dustStatus = "나쁨";
            else if (aqi === 5) dustStatus = "매우 나쁨";
        }

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

        if (aqi >= 4) {
            advice.dryMethod = "실내 건조 또는 건조기 추천";
            advice.savingTip = "현재 외부 미세먼지가 나쁩니다. 세탁물을 야외에 널지 마세요!";
        }

        res.status(200).json({
            status: "success",
            city: weatherData.name,
            weather: {
                temp: temp,
                condition: condition,
                description: weatherData.weather[0].description,
                humidity: humidity
            },
            dust: { 
                aqi_level: aqi,
                status: dustStatus
            },
            advice: advice
        });

    } catch (error) {
        console.error("날씨 단독 추천 API 에러:", error);
        res.status(500).json({ error: "날씨 처리 중 문제가 발생했습니다." });
    }
});

app.listen(port, () => {
    console.log(`Node.js 백엔드 서버 실행 중: port ${port}`);
});