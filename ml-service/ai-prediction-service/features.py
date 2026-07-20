import pandas as pd
from math import radians, sin, cos, sqrt, atan2

def haversine_distance(lat1, lon1, lat2, lon2):
    R = 6371
    lat1, lon1, lat2, lon2 = map(radians, [float(lat1), float(lon1),
                                            float(lat2), float(lon2)])
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))

def days_since_last_donation(last_donation_date):
    if pd.isna(last_donation_date):
        return 365
    return (pd.Timestamp.now().date() - pd.to_datetime(last_donation_date).date()).days

def build_features(df):
    features = pd.DataFrame()

    features['distance_km'] = df.apply(
        lambda r: haversine_distance(
            r['donor_lat'], r['donor_lng'],
            r['hospital_latitude'], r['hospital_longitude']
        ), axis=1
    )

    features['days_since_donation'] = df['last_donation_date'].apply(
        days_since_last_donation
    )

    features['response_rate'] = df['response_rate'].fillna(0.5).astype(float)

    features['is_available'] = df['availability_status'].astype(int)

    features['total_donations'] = df['total_donations'].fillna(0).astype(float)

    features['hour_of_day'] = df.get('hour_of_day', pd.Series([12] * len(df))).astype(float)
    features['day_of_week'] = df.get('day_of_week', pd.Series([1]  * len(df))).astype(float)

    urgency_map = {'LOW': 0, 'MEDIUM': 1, 'HIGH': 2, 'CRITICAL': 3}
    features['urgency_encoded'] = df['urgency_level'].map(urgency_map).fillna(1)

    return features