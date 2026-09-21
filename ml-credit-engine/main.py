from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, List, Dict
import numpy as np
import joblib
import logging
import time
import os
from pathlib import Path

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("ml-credit-engine")

app = FastAPI(
    title="LoanFlow ML Credit Engine",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_PATH  = Path("models/credit_model.joblib")
SCALER_PATH = Path("models/scaler.joblib")
MODEL_VERSION = os.getenv("MODEL_VERSION", "1.0.0")

FEATURES = [
    "monthly_income", "requested_amount", "tenure_months",
    "existing_emi_amount", "existing_loan_count",
    "debt_to_income_ratio", "credit_utilization",
    "employment_type_encoded", "loan_to_income_ratio",
    "affordability_ratio"
]

EMPLOYMENT_ENCODING = {
    "SALARIED": 0, "SELF_EMPLOYED": 1, "BUSINESS_OWNER": 2,
    "FREELANCER": 3, "RETIRED": 4, "STUDENT": 5
}


class ScoreRequest(BaseModel):
    loanApplicationId: str
    monthlyIncome:      float = Field(..., gt=0)
    requestedAmount:    float = Field(..., gt=0)
    tenureMonths:       int   = Field(..., ge=6, le=360)
    existingEmiAmount:  float = Field(default=0.0, ge=0)
    existingLoanCount:  int   = Field(default=0,   ge=0)
    employmentType:     str   = "SALARIED"
    debtToIncomeRatio:  float = Field(default=0.0, ge=0)
    creditUtilization:  float = Field(default=0.0, ge=0)
    correlationId:      Optional[str] = None


class ScoreResponse(BaseModel):
    creditScore:            int
    riskTier:               str
    maxEligibleAmount:      float
    recommendedInterestRate: float
    rejectionReasons:       List[str]
    shapExplanation:        Dict[str, float]
    modelVersion:           str
    scoringDurationMs:      int


class ModelRegistry:
    def __init__(self):
        self.model     = None
        self.scaler    = None
        self.explainer = None
        self.loaded    = False

    def load(self):
        if MODEL_PATH.exists() and SCALER_PATH.exists():
            logger.info("Loading model from %s", MODEL_PATH)
            self.model     = joblib.load(MODEL_PATH)
            self.scaler    = joblib.load(SCALER_PATH)

            self.loaded    = True
            logger.info("Model loaded — version %s", MODEL_VERSION)
        else:
            logger.warning("No model found. Training now...")
            from train import train_and_save
            train_and_save()
            self.model     = joblib.load(MODEL_PATH)
            self.scaler    = joblib.load(SCALER_PATH)
            self.explainer = shap.TreeExplainer(self.model)
            self.loaded    = True


registry = ModelRegistry()


@app.on_event("startup")
async def startup():
    Path("models").mkdir(exist_ok=True)
    registry.load()


def build_features(req: ScoreRequest) -> np.ndarray:
    emp = EMPLOYMENT_ENCODING.get(req.employmentType.upper(), 0)
    lti = req.requestedAmount / max(req.monthlyIncome, 1)
    aff = (req.requestedAmount / max(req.tenureMonths, 1)) / max(req.monthlyIncome, 1)
    return np.array([[
        req.monthlyIncome, req.requestedAmount, req.tenureMonths,
        req.existingEmiAmount, req.existingLoanCount,
        req.debtToIncomeRatio, req.creditUtilization,
        emp, lti, aff
    ]])


def prob_to_score(prob: float) -> int:
    return int(300 + prob * 600)


def score_to_tier(score: int) -> str:
    if score >= 750: return "LOW"
    if score >= 650: return "MEDIUM"
    if score >= 550: return "HIGH"
    return "VERY_HIGH"


def max_eligible(req: ScoreRequest, score: int) -> float:
    cap = req.monthlyIncome * 0.40 * req.tenureMonths
    mult = {"LOW": 1.0, "MEDIUM": 0.85,
            "HIGH": 0.65, "VERY_HIGH": 0.0}.get(score_to_tier(score), 0.0)
    return min(req.requestedAmount, cap) * mult


def interest_rate(score: int) -> float:
    if score >= 800: return 9.5
    if score >= 750: return 10.5
    if score >= 700: return 11.5
    if score >= 650: return 13.0
    if score >= 600: return 15.0
    if score >= 550: return 18.0
    return 24.0


def rejection_reasons(req: ScoreRequest, score: int) -> List[str]:
    reasons = []
    if req.debtToIncomeRatio > 50:
        reasons.append(f"DTI too high: {req.debtToIncomeRatio:.1f}% (max 50%)")
    if req.existingLoanCount >= 3:
        reasons.append(f"Too many existing loans: {req.existingLoanCount}")
    if req.creditUtilization > 80:
        reasons.append(f"Credit utilization too high: {req.creditUtilization:.1f}%")
    if score < 550:
        reasons.append(f"Credit score below minimum: {score} (min 550)")
    if req.monthlyIncome < 15000:
        reasons.append(f"Income below minimum: {req.monthlyIncome:,.0f} (min 15,000)")
    return reasons


@app.get("/health")
def health():
    return {"status": "UP", "modelLoaded": registry.loaded,
            "modelVersion": MODEL_VERSION}


@app.post("/score", response_model=ScoreResponse)
def score(req: ScoreRequest, request: Request):
    corr = request.headers.get("X-Correlation-ID", "unknown")
    logger.info("[%s] Scoring loan %s", corr, req.loanApplicationId)

    if not registry.loaded:
        raise HTTPException(status_code=503, detail="Model not loaded")

    start = time.time()

    X        = build_features(req)
    X_scaled = registry.scaler.transform(X)

    prob_good    = registry.model.predict_proba(X_scaled)[0][1]
    credit_score = prob_to_score(prob_good)
    tier         = score_to_tier(credit_score)

    shap_exp = {f: round(float(v), 4)
                for f, v in zip(FEATURES,
                registry.model.feature_importances_)}

    duration_ms = int((time.time() - start) * 1000)
    logger.info("[%s] score=%d tier=%s duration=%dms",
                corr, credit_score, tier, duration_ms)

    return ScoreResponse(
        creditScore=credit_score,
        riskTier=tier,
        maxEligibleAmount=round(max_eligible(req, credit_score), 2),
        recommendedInterestRate=interest_rate(credit_score),
        rejectionReasons=rejection_reasons(req, credit_score),
        shapExplanation=shap_exp,
        modelVersion=MODEL_VERSION,
        scoringDurationMs=duration_ms
    )