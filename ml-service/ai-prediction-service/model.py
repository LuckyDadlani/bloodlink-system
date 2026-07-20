import pickle, os
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.preprocessing import StandardScaler
from db import fetch_training_data
from features import build_features

MODEL_PATH  = "model.pkl"
SCALER_PATH = "scaler.pkl"

def generate_synthetic_data():
    np.random.seed(42)
    n = 500
    return pd.DataFrame({
        'availability_status':       np.random.choice([True, False], n, p=[0.7, 0.3]),
        'total_donations':           np.random.randint(0, 20, n),
        'last_donation_date':        pd.date_range('2020-01-01', periods=n, freq='D')[:n],
        'donor_lat':                 np.random.uniform(12.9, 13.1, n),
        'donor_lng':                 np.random.uniform(77.5, 77.7, n),
        'hospital_latitude':         np.full(n, 13.0),
        'hospital_longitude':        np.full(n, 77.6),
        'urgency_level':             np.random.choice(['LOW','MEDIUM','HIGH','CRITICAL'], n),
        'response_rate':             np.random.uniform(0, 1, n),
        'avg_response_time_minutes': np.random.uniform(5, 60, n),
        'hour_of_day':               np.random.randint(0, 24, n),
        'day_of_week':               np.random.randint(0, 7, n),
        'responded':                 np.random.choice([0, 1], n, p=[0.4, 0.6])
    })

def train_model():
    print("Fetching training data from Supabase...")
    try:
        df = fetch_training_data()
        if len(df) < 20:
            print(f"Only {len(df)} real records. Using synthetic data...")
            df = pd.concat([df, generate_synthetic_data()], ignore_index=True)
    except Exception as e:
        print(f"DB fetch failed: {e}. Using synthetic data.")
        df = generate_synthetic_data()

    print(f"Training on {len(df)} total records...")

    X = build_features(df)
    y = df['responded'].astype(int)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    scaler = StandardScaler()
    X_train_s = scaler.fit_transform(X_train)
    X_test_s  = scaler.transform(X_test)

    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=6,
        random_state=42,
        class_weight='balanced'
    )
    model.fit(X_train_s, y_train)

    y_pred = model.predict(X_test_s)
    y_prob = model.predict_proba(X_test_s)[:, 1]

    print("\n--- Model Evaluation ---")
    print(classification_report(y_test, y_pred))
    print(f"ROC-AUC Score: {roc_auc_score(y_test, y_prob):.4f}")

    with open(MODEL_PATH,  'wb') as f: pickle.dump(model,  f)
    with open(SCALER_PATH, 'wb') as f: pickle.dump(scaler, f)
    print("Model saved successfully!")
    return model, scaler

def load_model():
    if not os.path.exists(MODEL_PATH):
        print("No model found. Training now...")
        return train_model()
    with open(MODEL_PATH,  'rb') as f: model  = pickle.load(f)
    with open(SCALER_PATH, 'rb') as f: scaler = pickle.load(f)
    return model, scaler

def predict_donors(emergency_id):
    from db import fetch_eligible_donors
    model, scaler = load_model()
    df = fetch_eligible_donors(emergency_id)

    if df.empty:
        return []

    X = build_features(df)
    X_scaled = scaler.transform(X)
    probs = model.predict_proba(X_scaled)[:, 1]

    results = []
    for idx, (i, row) in enumerate(df.iterrows()):
        results.append({
            "donor_id":    str(row['donor_id']),
            "probability": round(float(probs[idx]), 4),
            "distance_km": round(float(X.iloc[idx]['distance_km']), 2)
        })

    results.sort(key=lambda x: x['probability'], reverse=True)
    return results