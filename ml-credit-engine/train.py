import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
import joblib
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger("ml-training")

FEATURES = [
    "monthly_income", "requested_amount", "tenure_months",
    "existing_emi_amount", "existing_loan_count",
    "debt_to_income_ratio", "credit_utilization",
    "employment_type_encoded", "loan_to_income_ratio",
    "affordability_ratio"
]


def generate_data(n=50000):
    np.random.seed(42)
    rng = np.random.default_rng(42)

    seg = rng.choice([0, 1, 2], n, p=[0.40, 0.45, 0.15])
    income = np.where(seg == 0,
                      rng.uniform(15000, 40000, n),
                      np.where(seg == 1,
                               rng.uniform(40000, 150000, n),
                               rng.uniform(150000, 500000, n)))

    amount = np.clip(income * rng.uniform(3, 20, n), 50000, 2000000)
    tenure = rng.choice([12, 24, 36, 48, 60, 84, 120, 180, 240], n)
    emi_ratio = rng.beta(1.5, 5, n)
    existing_emi = income * emi_ratio * 0.4
    loan_count = rng.choice([0, 1, 2, 3, 4, 5], n,
                             p=[0.35, 0.30, 0.20, 0.10, 0.03, 0.02])
    emp_type = rng.choice([0, 1, 2, 3, 4, 5], n,
                           p=[0.50, 0.20, 0.15, 0.08, 0.05, 0.02])

    dti = (existing_emi / income) * 100
    projected_emi = (amount / tenure) * 1.1
    utilization = ((existing_emi + projected_emi) / income) * 100
    lti = amount / income
    affordability = (amount / tenure) / income

    p_good = (0.95
              - 0.30 * (utilization > 60).astype(float)
              - 0.20 * (dti > 40).astype(float)
              - 0.15 * (loan_count >= 3).astype(float)
              - 0.25 * (income < 20000).astype(float)
              + 0.10 * (emp_type == 0).astype(float)
              + 0.05 * (seg == 2).astype(float)
              - 0.10 * (lti > 15).astype(float))
    p_good = np.clip(p_good, 0.05, 0.97)
    label = rng.binomial(1, p_good)

    df = pd.DataFrame({
        "monthly_income": income,
        "requested_amount": amount,
        "tenure_months": tenure.astype(float),
        "existing_emi_amount": existing_emi,
        "existing_loan_count": loan_count.astype(float),
        "debt_to_income_ratio": dti,
        "credit_utilization": utilization,
        "employment_type_encoded": emp_type.astype(float),
        "loan_to_income_ratio": lti,
        "affordability_ratio": affordability,
        "label": label
    })

    logger.info("Generated %d samples — %.1f%% good borrowers",
                n, label.mean() * 100)
    return df


def train_and_save():
    Path("models").mkdir(exist_ok=True)
    df = generate_data()

    X = df[FEATURES].values
    y = df["label"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)

    scaler = StandardScaler()
    X_train_s = scaler.fit_transform(X_train)
    X_test_s = scaler.transform(X_test)

    model = RandomForestClassifier(
        n_estimators=200, max_depth=12,
        min_samples_leaf=5, max_features="sqrt",
        class_weight="balanced", n_jobs=-1, random_state=42)

    model.fit(X_train_s, y_train)

    y_prob = model.predict_proba(X_test_s)[:, 1]
    auc = roc_auc_score(y_test, y_prob)
    logger.info("AUC-ROC: %.4f", auc)
    logger.info("\n%s", classification_report(
        y_test, model.predict(X_test_s),
        target_names=["Bad", "Good"]))

    joblib.dump(model, "models/credit_model.joblib")
    joblib.dump(scaler, "models/scaler.joblib")
    logger.info("Model saved to models/")
    return model, scaler


if __name__ == "__main__":
    train_and_save()